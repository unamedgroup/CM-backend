package org.unamedgroup.conference.service;

import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unamedgroup.conference.entity.User;
import org.unamedgroup.conference.service.GeneralService;
import org.unamedgroup.conference.service.impl.MySmsSingleSender;

import java.io.IOException;

/**
 * 腾讯云短信发送接口
 * 调用该端口发送短信
 *
 * @author liumengxiao
 * @date 2019/5/17
 */
@Component
public class Message {
    // 短信应用SDK AppID
    private static final int appid = 1400208768;
    // 短信应用SDK AppKey
    private static final String appkey = "671161f5d900db6e7337a61d56f821a7";
    // 短信模板ID，需要在短信应用中申请
    private static int templateId = 0;
    // 签名
    private static final String smsSign = "";

    @Autowired
    private GeneralService generalService;

    public Boolean attendance(String conferenceName, String time, String roomName) {
        try {
            templateId = 334604;
            String[] params = {conferenceName, time, roomName};
            sendMessage(params);
            return true;
        } catch (Exception e) {
            System.err.println("发送参会通知短信失败。详细信息：");
            System.err.println(e.toString());
        }
        return false;
    }

    public Boolean cancel(String year, String month, String day, String time, String roomName, String conferenceName) {
        try {
            templateId = 334605;
            String[] params = {year, month, day, time, roomName, conferenceName};
            sendMessage(params);
            return true;
        } catch (Exception e) {
            System.err.println("发送取消会议通知短信失败。详细信息：");
            System.err.println(e.toString());
        }
        return false;
    }

    private Boolean sendMessage(String[] params) {
        try {
            User user = generalService.getLoginUser();
            SmsSingleSender ssender = new MySmsSingleSender(appid, appkey);
            // 签名参数未提供或者为空时，会使用默认签名发送短信
            SmsSingleSenderResult result = ssender.sendWithParam("86", user.getPhoneNumber(), templateId, params, smsSign, "", "");
            System.out.println(result);
        } catch (HTTPException e) {
            // HTTP响应码错误
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // json解析错误
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // 网络IO错误
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
