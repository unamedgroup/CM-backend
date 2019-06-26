package org.unamedgroup.conference.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.unamedgroup.conference.dao.UserRepository;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.temp.FailureInfo;
import org.unamedgroup.conference.entity.temp.SuccessInfo;
import org.unamedgroup.conference.feature.entity.util.Utils;
import org.springframework.web.bind.annotation.*;
import org.unamedgroup.conference.feature.service.IDetectFaceService;
import org.unamedgroup.conference.security.JWTUtil;
import org.unamedgroup.conference.service.GeneralService;
import org.unamedgroup.conference.service.impl.FaceServiceImpl;
import org.unamedgroup.conference.service.impl.GeneralServiceImpl;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * BioFeatureController
 * 错误代码使用1xxx
 *
 * @author zhoutao
 * @date 2019/05/16
 */
@Api(value = "人脸识别 API", description = "人脸识别接口", protocols = "http")
@CrossOrigin
@RestController
@RequestMapping("/bioFeature")
public class BioFeatureController {

    @Autowired
    IDetectFaceService detectFaceService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    FaceServiceImpl faceServiceImpl;

    /**
     * 人脸获取
     *
     * @param imgStr
     * @return
     */
    @ApiOperation(value = "设置人脸特征信息api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Integer.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "imgStr", value = "图片流信息", required = true, dataType = "String", paramType = "query"),
    })
    @RequestMapping(value = "/set", method = RequestMethod.POST)
    @ResponseBody
    public Object faceDetect(String imgStr) {
        //登录有效性验证
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        // 获取用户ID
        String phone = JWTUtil.getPhoneNumber(subject.getPrincipal().toString());
        Integer userID = userRepository.getUserByPhoneNumber(phone).getUserID();
        try {
            // 图片base64转流
            InputStream inputStream = Utils.base64InputStream(imgStr);
            // 流转图片
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            // 人脸检测
            int result = detectFaceService.detectFeature(bufferedImage, userID);
            if (inputStream != null) {
                inputStream.close();
            }
            if (result == -1) {
                System.err.println("找不到人脸信息，请重试");
                return new FailureInfo(4000,"找不到人脸信息，请重试" );
            }
            return new SuccessInfo("设置人脸成功" );
        } catch (Exception e) {
            System.err.println("发生错误，请检查：" + e.toString());
            return new FailureInfo(4001, "发生错误请检查" );
        }
    }

    /**
     * 人脸匹配
     *
     * @param imgStr
     * @return result
     */
    @ApiOperation(value = "对比人脸特征信息api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Integer.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "imgStr", value = "图片流信息", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "roomID", value = "会议室id", required = true, dataType = "int", paramType = "query"),
    })
    @RequestMapping(value = "/get", method = RequestMethod.POST)
    @ResponseBody
    public Object faceCompare(String imgStr, Integer roomID) {
        // 获取当前时间
        // 格式化时间
        try {
            // 处理图片信息
            InputStream inputStream = Utils.base64InputStream(imgStr);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            //现在时间
            Date nowTime = new Date();
            //半个小时后时间
            Date afterHalfHour = new Date(nowTime.getTime() + (long)30*60*1000);
            // 获取会议所有的参与者
            List<Integer> userIDList = faceServiceImpl.getUserIDFromConference(roomID, nowTime, afterHalfHour);
            if(userIDList.isEmpty() || userIDList == null ){
                return new FailureInfo(4003,"当前时间没有会议" );
            }
            Double result = -1.0;
            for (int i=0;i<userIDList.size();i++) {
                // 匹配人脸
                Double temp =  detectFaceService.compareFace(bufferedImage, userIDList.get(i));
                if(result < temp){
                    result = temp;
                }
            }
            // 所有的输出都是-1，则是没有检测到人脸
            if (result == -1) {
                System.err.println("找不到人脸信息");
                return new FailureInfo(4000,"找不到人脸信息" );
            }
            // 相似度大于0.8认为是同一个人
            if (result >= 0.8){
                return new SuccessInfo("人脸匹配成功" );
            }else{
                return new FailureInfo(4002,"人脸匹配失败" );
            }
        } catch (Exception e) {
            System.err.println("发生错误，请检查：" + e.toString());
            return new FailureInfo(4001,"发生错误，请检查" );
        }
    }
}
