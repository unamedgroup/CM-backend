package org.unamedgroup.conference.feature.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unamedgroup.conference.feature.entity.Box;
import org.unamedgroup.conference.feature.entity.FaceNet;
import org.unamedgroup.conference.feature.entity.MtCnn;
import org.unamedgroup.conference.feature.entity.util.Utils;
import org.unamedgroup.conference.feature.service.IDetectFaceService;
import org.unamedgroup.conference.service.UserManageService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;
import java.util.Base64;
import java.util.Vector;

/**
 * Interface
 *
 * @author zhoutao
 * @date 2019-03-12
 */
@Component
public class DetectFaceServiceImpl implements IDetectFaceService {

    @Autowired
    UserManageService userManageService;
    @Autowired
    MtCnn mtCnn;
    @Autowired
    FaceNet faceNet;

    /**
     * 根据图片信息提取到人脸的特征信息
     *
     * @param file
     * @return faceFeature
     */
    @Override
    public int detectFeature(File file, Integer userID) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            detectFeature(bufferedImage, userID);
            return 0;
        } catch (IOException e) {
            System.out.println(e.toString());
            return -1;
        }
    }

    /**
     * 根据图片信息提取到人脸的特征信息
     *
     * @param bufferedImage
     * @return faceFeature
     */
    @Override
    public int detectFeature(BufferedImage bufferedImage, Integer userID) {
        try {
            float[][] result = this.commonDetect(bufferedImage);
            byte[] bytes = Utils.floatArrayToByteArray(result);

            System.out.println("转换前特征值：" + bytes);
            String feature = Base64.getEncoder().encodeToString(bytes);
            System.out.println("转换后特征字符串：" + feature);
            userManageService.setUserFeature(userID, feature);
            return 0;
        } catch (IOException e) {
            System.err.println("流转图片失败" + e.toString());
            return -1;
        }
    }

    /**
     * 根据人脸的特征信息进行人脸匹配
     *
     * @param bufferedImage
     * @param userID
     * @return
     */
    @Override
    public double compareFace(BufferedImage bufferedImage, Integer userID) {
        try {
            //从数据库中读取人脸特征信息
            String sourceData = userManageService.getUserFeature(userID);
            if (sourceData == null) {
                System.out.println("查询人脸信息失败！");
                return 0;
            }
            System.out.println("转换前特征字符串：" + sourceData);
            //奖字符串转换成byte数组
            byte[] data = Base64.getDecoder().decode(sourceData);
            float[][] faceFeature = Utils.byteArrayToFloatArray(data);
            System.out.println("转换后特征值：" + data.toString());
            float[][] testFaceFeature = this.commonDetect(bufferedImage);
            return faceNet.compareFeature(faceFeature, testFaceFeature);
        } catch (IOException e) {
            System.err.println("匹配人脸出错，请检查" + e.toString());
            return -1;
        }
}

    /**
     * 根据人脸的特征信息进行人脸匹配
     *
     * @param file 图片文件
     * @param userID 用户ID
     * @return double 人脸比较相似度
     */
    @Override
    public double compareFace(File file, Integer userID) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return compareFace(bufferedImage, userID);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return -1.0;
    }


    /**
     * @param bufferedImage 输入的图片流
     * @return 特征信息
     * @throws IOException 图片处理异常
     */
    private float[][] commonDetect(BufferedImage bufferedImage) throws IOException {
        Vector<Box> boxes = mtCnn.detectFaces(bufferedImage,25);
        if(boxes.size() == 0){
            return null;
        }
        BufferedImage face = Utils.getFace(bufferedImage, boxes.get(0).box);
        return faceNet.getFeature(face);
    }
}
