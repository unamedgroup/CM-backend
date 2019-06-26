package org.unamedgroup.conference.feature.entity;


import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;

import java.io.IOException;
import java.util.List;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;

/**
 * 人脸检测类
 * @date 2019-5-23
 * @author  zhoutao
 */
@Component
public class MtCnn {

    private static final float FACTOR = 0.709f;
    private static final float P_NET_THRESHOLD = 0.6f;
    private static final float R_NET_THRESHOLD = 0.6f;
    private static final float O_NET_THRESHOLD = 0.7f;

    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128;

    private static final String PD_PATH = "models/FaceDetect.pb";
    private static final String P_NET_IN_NAME = "pnet/input:0";
    private static final String[] P_NET_OUT_NAME = new String[]{"PNet/conv4_1/Softmax:0", "PNet/conv4_2/BiasAdd:0"};
    private static final String R_NET_IN_NAME = "rnet/input:0";
    private static final String[] R_NET_OUT_NAME = new String[]{"RNet/cls_fc/Softmax", "RNet/bbox_fc/BiasAdd"};
    private static final String O_NET_IN_NAME = "onet/input:0";
    private static final String[] O_NET_OUT_NAME = new String[]{"ONet/cls_fc/Softmax", "ONet/bbox_fc/BiasAdd"};

    private Session session;

    public MtCnn() {
        Graph graph = new Graph();
        byte[] graphDef;
        try {
            Resource resource = new ClassPathResource(PD_PATH);
            InputStream in = resource.getInputStream();
//            graphDef = IOUtils.toByteArray(new FileInputStream(PD_PATH));
            graphDef = IOUtils.toByteArray(in);
            graph.importGraphDef(graphDef);
            session = new Session(graph);
        } catch (IOException e) {
            System.err.println("创建图失败"+ e.toString());
        }
    }


    /**
     * 检测人脸
     * @param img 输入的图片
     * @param minFaceSize 人脸检测的最小尺寸
     * @return Vector<Box>
     */
    public Vector<Box> detectFaces(BufferedImage img, int minFaceSize){
        // 获取图片的宽、高
        int w = img.getWidth();
        int h = img.getHeight();
        Vector<Box> boxes;
        try {
            // 执行PNE获取输出
            boxes = pNet(img, minFaceSize, w, h);
            // 将人脸框限制在图片范围内
            squareLimit(boxes, w, h);
            if (boxes.size() == 0) {
                return boxes;
            }
            // 执行RNE获取输出
            boxes = rNet(img, boxes);
            squareLimit(boxes, w, h);
            if (boxes.size() == 0) {
                return boxes;
            }
            // 执行ONE获取输出
            boxes = oNet(img, boxes);
            return boxes;
        } catch (IOException e) {
            System.err.println("人脸检测失败" + e.toString());
            return null;
        }
    }

    /**
     * MTCNN中PNet
     *
     * @param img 输入的图片
     * @param minFaceSize 人脸的最小尺寸
     * @param w 输入图片的宽
     * @param h 输入图片的高
     * @return Vector<Box>
     * @throws IOException 图片处理的异常
     */
    private Vector<Box> pNet(BufferedImage img, int minFaceSize, int w, int h) throws IOException {
        int whMin = min(w, h);
        Vector<Box> totalBoxes = new Vector<>();
        float currentFaceSize = minFaceSize;
        while (currentFaceSize <= whMin) {
            float scale = 12.0f / currentFaceSize;
            List<Box> list = pNetForword(img, scale);
            totalBoxes.addAll(list);
            //Face Size等比递增
            currentFaceSize /= FACTOR;
        }
        //NMS 0.7
        nms(totalBoxes, 0.7f, "Union");
        return updateBoxes(totalBoxes);
    }

    /**
     * MTCNN中PNet检测
     * @param img 输入的图片
     * @param scale 图片的缩放的尺寸
     * @return curBoxes 返回人脸的Box列表
     * @throws IOException 图片处理的异常
     */
    private List<Box> pNetForword(BufferedImage img, float scale) throws IOException {
        // 图片大小缩放
        BufferedImage resizeImg = resize(img, scale);
        // 创建输入的Tensor
        Tensor<Float> x = image2FloatTensor(resizeImg);
        // 获取输出的Tensor列表
        List<Tensor<?>> outputs = predict(x, P_NET_IN_NAME, P_NET_OUT_NAME);
        // 一次取出人脸的prob[可能性]及bbox[人脸框]
        Tensor<Float> outProb = outputs.get(0).expect(Float.class);
        Tensor<Float> outBox = outputs.get(1).expect(Float.class);
        // 获取到输出的Shape大小
        long[] shape = outProb.shape();
        int pNetOutSizeHeight = (int) shape[1];
        int pNetOutSizeWeight = (int) shape[2];
        // 获取输出的结果值
        float[][][][] p = outProb.copyTo(new float[1][pNetOutSizeHeight][pNetOutSizeWeight][2]);
        float[][][][] b = outBox.copyTo(new float[1][pNetOutSizeHeight][pNetOutSizeWeight][4]);
        // 从[1,?,?,2]数组中获取第一个
        float[][][] pNetOutBias = b[0];
        // 只读出每个人脸框的可信度
        float[][] pNetOutProb = new float[pNetOutSizeHeight][pNetOutSizeWeight];
        expandProb(p[0], pNetOutProb);
        // 创建存放结果的Vector
        Vector<Box> curBoxes = new Vector<>();
        // 生成人脸的Box
        generateBoxes(pNetOutProb, pNetOutBias, scale, curBoxes);
        // 执行非极大值抑制
        nms(curBoxes, 0.5f, "Union");
        // 人脸框的修正
        curBoxes.forEach(Box::calibrate);
        // 删除不满足的人脸框Boz
        return curBoxes.stream().filter(box -> !box.deleted).collect(toList());
    }

    /**
     * 不同scale裁剪图片
     * @param img 输入的图片
     * @param scale 图片缩放比例
     * @return BufferedImage 返回缩放后的图片
     * @throws IOException 图片处理的异常
     */
    private static BufferedImage resize(BufferedImage img, float scale) throws IOException {
        return Thumbnails.of(img).scale(scale).asBufferedImage();
    }

    /**
     * 取出预测的Tensor
     * @param x 输入的张量
     * @param inName 输入的节点名称
     * @param outputNames 输出的节点名称
     * @return List<Tensor <?>> 返回输出的Tensor列表
     */
    private List<Tensor<?>> predict(Tensor<Float> x, String inName, String[] outputNames) {
        // feed数据
        Session.Runner runner = session.runner().feed(inName, x);
        // 获取输出
        for (String outName : outputNames) {
            runner.fetch(outName);
        }
        return runner.run();
    }

    /**
     * 将BufferedImage转Tensor
     * @param img 输入的图片
     * @return Tensor<Float> 返回的Tensor张量
     */
    private Tensor<Float> image2FloatTensor(BufferedImage img) {
        // 获取图片张量
        float[][][][] floatValues = image2FloatArr(img);
        return Tensors.create(floatValues);
    }
    /**
     * 将BufferedImage转数组
     * @param img 输入的图片
     * @return float[][][][] 返回的图片矩阵
     */
    private float[][][][] image2FloatArr(BufferedImage img) {
        // 获取图片的宽高
        int w = img.getWidth();
        int h = img.getHeight();
        // 创建矩阵
        float[][][][] floatValues = new float[1][h][w][3];
        // 循环读取数据
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                // 获取图片的元素值
                int val = img.getRGB(i, j);
                // 图片需要BGR通道
                floatValues[0][j][i][2] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatValues[0][j][i][1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatValues[0][j][i][0] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            }
        }
        return floatValues;
    }

    /**
     * 获取输出人脸框的可信度
     * @param src 源矩阵
     * @param dst 输出矩阵
     */
    private void expandProb(float[][][] src, float[][] dst) {
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {

                dst[i][j] = src[i][j][1];
            }
        }
    }

    /**
     *  生成人脸框的BBox
     * @param prob 可信度数组
     * @param bias 人脸框的源数据
     * @param scale 缩放比例
     * @param boxes 存放人脸框的列表
     */
    private void generateBoxes(float[][] prob, float[][][] bias, float scale, Vector<Box> boxes) {
        // 获取对应的高度值
        int h = prob.length;
        // 获取对应的宽度值
        int w = prob[0].length;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float score = prob[y][x];
                // 只保留满足置信度大于0.6的
                if (score > P_NET_THRESHOLD) {
                    Box box = new Box();
                    // 获取得分
                    box.score = score;
                    //生成人脸坐标
                    box.box[0] = Math.round(x * 2 / scale);
                    box.box[1] = Math.round(y * 2 / scale);
                    box.box[2] = Math.round((x * 2 + 12) / scale);
                    box.box[3] = Math.round((y * 2 + 12) / scale);
                    // 人脸坐标的偏移
                    System.arraycopy(bias[y][x], 0, box.bbr, 0, 4);
                    boxes.addElement(box);
                }
            }
        }
    }

    /**
     * 非极大值抑制
     * @param boxes 输入的人脸框列表
     * @param threshold 阈值
     * @param method 采取的NMS方式
     */
    private void nms(Vector<Box> boxes, float threshold, String method) {
        //NMS.两两比对
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            if (!box.deleted) {
                //score<0表示当前矩形框被删除
                for (int j = i + 1; j < boxes.size(); j++) {
                    Box box2 = boxes.get(j);
                    if (!box2.deleted) {
                        // 获取相交区域的坐标
                        int x1 = max(box.box[0], box2.box[0]);
                        int y1 = max(box.box[1], box2.box[1]);
                        int x2 = min(box.box[2], box2.box[2]);
                        int y2 = min(box.box[3], box2.box[3]);
                        // 相交区域为空
                        if (x2 < x1 || y2 < y1) {
                            continue;
                        }
                        // 交叉区域的大小
                        int areaIou = (x2 - x1 + 1) * (y2 - y1 + 1);
                        float iou = 0f;
                        if ("Union".equals(method)) {
                            iou = 1.0f * areaIou / (box.area() + box2.area() - areaIou);
                        } else if ("Min".equals(method)) {
                            iou = 1.0f * areaIou / (min(box.area(), box2.area()));
                        }
                        //删除prob小的那个框
                        if (iou >= threshold) {
                            if (box.score > box2.score) {
                                box2.deleted = true;
                            } else {
                                box.deleted = true;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 限制人脸矩形大小
     * @param boxes 输入的人脸框
     * @param w 图片的宽
     * @param h 图片的高
     */
    private void squareLimit(Vector<Box> boxes, int w, int h) {
        boxes.forEach(Box::toSquareShape);
        boxes.forEach(box -> box.limitSquare(w, h));
    }

    /**
     *  rNet
     * @param img 输入的图片
     * @param boxes PNet输出人脸框
     * @return boxes 执行rnet后的人脸框
     * @throws IOException 图片处理的异常
     */
    private Vector<Box> rNet(BufferedImage img, Vector<Box> boxes) throws IOException {
        //RNet Input Init
        float[][][][] rNetIn = getCropFloatArray(img, boxes, 24);
        //Run RNet
        rNetForward(rNetIn, boxes);
        //R_NET_THRESHOLD
        checkScore(boxes, R_NET_THRESHOLD);
        //Nms
        nms(boxes, 0.7f, "Union");
        boxes.forEach(Box::calibrate);
        return updateBoxes(boxes);
    }

    /**
     *  去除小于阈值的box
     * @param boxes 人两矿
     * @param threshold 阈值
     */
    private void checkScore(Vector<Box> boxes, float threshold) {
        for (Box box : boxes) {
            if (box.score < threshold) {
                box.deleted = true;
            }
        }
    }

    /**
     *  rNet取出Boxes
     * @param rNetIn rNet输入
     * @param boxes 人脸框
     */
    private void rNetForward(float[][][][] rNetIn, Vector<Box> boxes) {
        netForward(rNetIn, boxes, R_NET_IN_NAME, R_NET_OUT_NAME);
    }

    /**
     * 截取box中指定的矩形框(越界要处理)，并resize到size*size大小，返回数据存放到data中。
     * @param img 图片
     * @param box 人脸框
     * @param size 图片裁剪后的大小
     * @return float[][][] 输出图片矩阵
     */
    private float[][][] cropAndResize(BufferedImage img, Box box, int size) throws IOException {
        //crop and resize
        float scale = 1.0f * size / box.width();
        BufferedImage bufferedImage = Thumbnails.of(img)
                .sourceRegion(box.left(), box.top(), box.width(), box.height())
                .scale(scale).asBufferedImage();
        float[][][][] floatValues = image2FloatArr(bufferedImage);
        return floatValues[0];
    }

    /**
     * oNet
     * @param img oNet输入图片
     * @param boxes 人脸框
     * @return boxes oNet输出人脸框
     * @throws IOException 图片处理的异常
     */
    private Vector<Box> oNet(BufferedImage img, Vector<Box> boxes) throws IOException {
        //ONet Input Init
        float[][][][] oNetIn = getCropFloatArray(img, boxes, 48);
        //Run ONet
        oNetForward(oNetIn, boxes);
        //O_NET_THRESHOLD
        checkScore(boxes, O_NET_THRESHOLD);
        boxes.forEach(Box::calibrate);
        //Nms
        nms(boxes, 0.7f, "Min");
        return updateBoxes(boxes);
    }

    /**
     *  从BufferedImage获取输入
     * @param img 输入的图片
     * @param boxes 人脸框
     * @param size 图片的数量
     * @return float[][][][] 输出的图片矩阵
     * @throws IOException 图片处理的异常
     */
    private float[][][][] getCropFloatArray(BufferedImage img, Vector<Box> boxes, int size) throws IOException {
        int num = boxes.size();
        float[][][][] in = new float[num][size][size][3];
        int idx = 0;
        for (Box box : boxes) {
            float[][][] curCrop = cropAndResize(img, box, size);
            in[idx++] = curCrop;
        }
        return in;
    }

    /**
     * 取出oNet输出boxes
     * @param oNetIn oNet网络输入
     * @param boxes 人脸框
     */
    private void oNetForward(float[][][][] oNetIn, Vector<Box> boxes) {
        netForward(oNetIn, boxes, O_NET_IN_NAME, O_NET_OUT_NAME);
    }

    /**
     * 从rNet，oNet获取输出
     * @param netIn 网络输入
     * @param boxes 人脸框
     * @param inName 输入Tensor的名称
     * @param outNames 输出Tensor的名称
     */
    private void netForward(float[][][][] netIn, Vector<Box> boxes, String inName, String[] outNames) {
        // 创建Tensor
        Tensor<Float> x = Tensors.create(netIn);
        // 获取输出Tensor列表
        List<Tensor<?>> outputs = predict(x, inName, outNames);
        // 获取每一个Tensor
        Tensor<Float> rNetProb = outputs.get(0).expect(Float.class);
        Tensor<Float> rNetBox = outputs.get(1).expect(Float.class);
        // 获取Tensor的Shape
        int c1 = (int) rNetProb.shape()[0];
        // 获取Tensor的值
        float[][] p = rNetProb.copyTo(new float[c1][2]);
        float[][] b = rNetBox.copyTo(new float[c1][4]);
        // 执行更新人脸框
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            box.score = p[i][1];
            System.arraycopy(b[i], 0, box.bbr, 0, 4);
        }
    }

    /**
     *  更新boxes，删除需要删除的box
     * @param boxes 人脸框
     * @return Vector<Box> 处理后人脸框
     */
    private static Vector<Box> updateBoxes(Vector<Box> boxes) {
        Vector<Box> b = new Vector<>();
        for (Box box : boxes) {
            if (!box.deleted) {
                b.addElement(box);
            }
        }
        return b;
    }
}