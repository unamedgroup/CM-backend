package org.unamedgroup.conference.feature.entity;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.tensorflow.Graph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.tensorflow.*;



/**
 * 人脸匹配类
 * @author zhoutao
 * @date 2019-5-22
 */
@Component
public class FaceNet {
    private static final String MODEL_FILE = "models/FaceCompare.pb";
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "embeddings:0";
    private static final String TYPE = "phase_train:0";
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128f;
    private Session session;

    public FaceNet(){
        //初始化时加载模型
        loadModel();
    }

    /**
     * 加载Tensorflow模型
     * @return
     */
    private boolean loadModel(){
        try{
            Graph graph = new Graph();
//            byte[] graphBytes = IOUtils.toByteArray(new FileInputStream(MODEL_FILE ));
            Resource resource = new ClassPathResource(MODEL_FILE);
            InputStream in = resource.getInputStream();
            byte[] graphBytes = IOUtils.toByteArray(in);
            graph.importGraphDef(graphBytes);
            this.session = new Session(graph);
            System.out.println("创建图成功");
            return true;
        }
        catch (Exception e){
            System.err.println("创建图失败");
            return false;
        }
    }

    /**
     *  获取人脸特征
     * @param bufferedImage
     * @return 特征值
     */
    public float[][] getFeature(BufferedImage bufferedImage){
        try {
            //将图片流缩放大小，生成BufferedImage
            BufferedImage cropImage = this.resize(bufferedImage, 160, 160);
            // 生成输入数据
            Tensor<Float> in = this.bufferedImage2Tensor(cropImage);
            Session.Runner runner = session.runner();
            // feed数据到模型中
            runner.feed(INPUT_NAME, in);
            runner.feed(TYPE, Tensor.create(false));
            // 获取输出的Tensor
            Tensor tensor = runner.fetch(OUTPUT_NAME).run().get(0);
            // non scalar data ,特征向量长度
            float[][] embedding = new float[1][512];
            // 获取输出的值
            tensor.copyTo(embedding);
            return embedding;
        }catch(Exception e){
            System.err.println("获取特征信息失败，请检查输入是否有误" + e.toString());
            return null;
        }
    }

    /**
     * 比较人脸相似度
     * @param emb1
     * @param emb2
     * @return result
     */
    public float compareFeature(float[][] emb1, float[][] emb2) {
        float sum = 0;
        for(int i=0;i<emb1[0].length;i++){
            sum += Math.pow(emb1[0][i] - emb2[0][i], 2);
        }
        // 计算两者之间的距离
        double distance = Math.sqrt(sum);
        // 将距离映射到[0,1]上，作为相似的概率，函数为y = e^(-x)
        float similar =(float) Math.exp(-distance);
        return similar;
    }

    /**
     * 将模型的输出转成Box数组
     * @param smallScale
     * @param bigScale
     * @return
     */
    private Vector<Box> generateBoxes(float[][][] smallScale, float[][][] bigScale) {
        Box box = null;
        int[] bndBox = new int[4];
        Vector<Box> boxes = new Vector<>();
        // 处理13x13的输出
        for(int i=0; i<13; i++) {
            for(int j=0; j<13; j++) {
                for(int k=0; k<18;k=k+6) {
                    box = new Box();
                    bndBox[0] = (int)(this.sigmoid(smallScale[i][j][k]) -  this.sigmoid(smallScale[i][j][k+2]) / 2);
                    bndBox[1] = (int)(this.sigmoid(smallScale[i][j][k+1]) -  this.sigmoid(smallScale[i][j][k+3]) / 2);
                    bndBox[2] = (int)(this.sigmoid(smallScale[i][j][k]) +  this.sigmoid(smallScale[i][j][k+2]) / 2);
                    bndBox[3] = (int)(this.sigmoid(smallScale[i][j][k+1]) +  this.sigmoid(smallScale[i][j][k+3]) / 2);
                    box.box = bndBox;
                    box.score = this.sigmoid(smallScale[i][j][k+4]);
                    boxes.add(box);
                }
            }
        }
        // 处理26x26输出
        for(int i=0;i<26; i++) {
            for(int j=0;j<26; j++) {
                for(int k=0; k<18;k=k+6) {
                    box = new Box();
                    bndBox[0] = (int)(this.sigmoid(bigScale[i][j][k]) - this.sigmoid(bigScale[i][j][k+2]) / 2);
                    bndBox[1] = (int)(this.sigmoid(bigScale[i][j][k+1]) -  this.sigmoid(bigScale[i][j][k+3]) / 2);
                    bndBox[2] = (int)(this.sigmoid(bigScale[i][j][k]) +  this.sigmoid(bigScale[i][j][k+2]) / 2);
                    bndBox[3] = (int)(this.sigmoid(bigScale[i][j][k+1]) +  this.sigmoid(bigScale[i][j][k+3]) / 2);
                    box.box = bndBox;
                    box.score = this.sigmoid(bigScale[i][j][k+4]);
                    boxes.add(box);
                }
            }
        }
        return boxes;
    }

    /**
     * sigmoid 函数
     * @param in
     * @return
     */
    private float sigmoid(float in){
        return (float)(1.0 / (1.0 + Math.exp(-in)));
    }

    /**
     * Non Maximum Supress 非极大值抑制
     * @param boxes
     * @param threshold
     * @param method
     * @return
     */
    private static Vector<Box> nms(Vector<Box> boxes, float threshold, String method){
        //NMS.两两比对
        for(int i=0;i<boxes.size();i++) {
            Box box = boxes.get(i);
            if (!box.deleted) {
                //score<0表示当前矩形框被删除
                for (int j = i + 1; j < boxes.size(); j++) {
                    Box box2=boxes.get(j);
                    if (!box2.deleted) {
                        float x1 = Math.max((float)box.box[0], (float)box2.box[0]);
                        float y1 = Math.max((float)box.box[1], (float)box2.box[1]);
                        float x2 = Math.min((float)box.box[2], (float)box2.box[2]);
                        float y2 = Math.min((float)box.box[3], (float)box2.box[3]);
                        if (x2 < x1 || y2 < y1) {
                            continue;
                        }
                        float areaIoU = (x2 - x1 ) * (y2 - y1 );
                        float iou=0f;
                        if (method.equals("Union")){
                            iou = 1.0f * areaIoU / (box.area() + box2.area() - areaIoU);
                        }
                        else if (method.equals("Min")) {
                            iou = 1.0f * areaIoU / (Math.min(box.area(), box2.area()));
                        }
                        if (iou >= threshold) {
                            //删除prob小的那个框
                            if (box.score > box2.score){
                                box2.deleted = true;
                            }
                            else{
                                box.deleted = true;
                            }
                        }
                    }
                }
            }
        }
        Vector<Box> boxes1 = new Vector<>();
        for(int i =0;i<boxes.size();i++){
            if (!boxes.get(i).deleted){
                boxes1.add(boxes.get(i));
            }
        }
        return boxes1;
    }

    /**
     * 将BufferedImage转Tensor
     * @param img
     * @return Tensor<Float>
     */
    private Tensor<Float> bufferedImage2Tensor(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][][][] floatValues = new float[1][w][h][3];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int val = img.getRGB(i, j);
                floatValues[0][j][i][0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatValues[0][j][i][1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatValues[0][j][i][2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            }
        }
        return Tensors.create(floatValues);
    }

    /**
     * 处理图片流，将其resize到指定大小
     * @param bufferedImage
     * @param width
     * @param height
     * @return BufferedImage
     */
    private BufferedImage resize(BufferedImage bufferedImage, int width, int height) {
        try {
            BufferedImage cropImage = Thumbnails.of(bufferedImage).size(width, height).keepAspectRatio(false).asBufferedImage();
            return cropImage;
        } catch (IOException e) {
            System.err.println("" + e.toString());
            return null;
        }
    }
}
