package org.unamedgroup.conference.feature.service;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * Interface
 * @author zhoutao
 * @date 2019-03-12
 */
public interface IDetectFaceService {
    /**
     * 根据图片信息提取到人脸的特征信息
     * @param file
     * @return faceFeature
     */
    public int detectFeature(File file, Integer userID);

    /**
     * 根据图片信息提取到人脸的特征信息
     * @param bufferedImage
     * @return faceFeature
     */
    public int detectFeature(BufferedImage bufferedImage, Integer userID);

    /**
     * 根据人脸的特征信息进行人脸匹配
     * @param bufferedImage
     * @param userID
     * @return result
     */
    public double compareFace(BufferedImage bufferedImage, Integer userID);

    /**
     * 根据人脸的特征信息进行人脸匹配
     * @param file
     * @param userID
     * @return result
     */
    public double compareFace(File file, Integer userID);

}
