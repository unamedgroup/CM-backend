package org.unamedgroup.conference.controller;

import com.auth0.jwt.JWT;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.unamedgroup.conference.dao.ConferenceRepository;
import org.unamedgroup.conference.dao.UserRepository;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.User;
import org.unamedgroup.conference.entity.temp.FailureInfo;
import org.unamedgroup.conference.entity.temp.Info;
import org.unamedgroup.conference.entity.temp.SuccessInfo;
import org.unamedgroup.conference.security.JWTUtil;
import org.unamedgroup.conference.security.UnauthorizedException;

import java.util.List;

/**
 * UserController
 *
 * @author liumengxiao
 * @date 2019/03/12
 */

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ConferenceRepository conferenceRepository;

    @RequestMapping(value = "/myConferences", method = RequestMethod.GET)
    @ResponseBody
    public List<Conference> getMyConferences(Integer userID) {
        List<Conference> myConferences;
        try {
            myConferences = conferenceRepository.getConferencesByUser(userID);
            return myConferences;
        } catch (Exception e) {
            System.err.println("根据用户信息获取会议信息错误，请核实。详细信息：");
            System.err.println(e.toString());
        }
        return null;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Object login(String phoneNumber, String password) {
        try {
            Info info = null;
            User user = userRepository.getUserByPhoneNumber(phoneNumber);
            if (user == null) {
                info = new FailureInfo("用户名不存在！");
                return info;
            }
            //用户名密码是否匹配
            if (password.equals(user.getPassword())) {
                String token = JWTUtil.generateToken(phoneNumber, user.getPasswordHash());
                info = new SuccessInfo(token);
                return info;
            } else {
                info = new FailureInfo("用户名密码不匹配！");
                return info;
            }
        } catch (Exception e) {
            System.err.println("用户名密码验证出现异常！详细信息：");
            System.err.println(e.toString());
        }
        return new FailureInfo("用户名密码验证出现异常！");
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public Info test() {
        Info info = null;
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            info = new FailureInfo("用户登录失败！");
            return info;
        }
        info = new SuccessInfo("success!");
        return info;
    }
}
