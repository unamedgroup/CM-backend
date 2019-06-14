package org.unamedgroup.conference.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.unamedgroup.conference.dao.ConferenceRepository;
import org.unamedgroup.conference.dao.UserRepository;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.temp.FailureInfo;
import org.unamedgroup.conference.entity.temp.SuccessInfo;
import org.unamedgroup.conference.security.JWTUtil;
import org.unamedgroup.conference.service.*;

import java.util.Calendar;
import java.util.List;

/**
 * 错误代码使用3xxx
 *
 * @Author: 白振宇
 * @Date： 2019/3/15 14:08
 */

@Api(value = "会议 API", description = "会议操作接口", protocols = "http")
@CrossOrigin
@RestController
@RequestMapping(value = "conference")
public class ConferenceController {

    @Autowired
    ConferenceRepository conferenceRepository;
    @Autowired
    MyConferenceService myConferenceService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    GeneralService generalService;
    @Autowired
    ManagingAttendeesService managingAttendeesService;
    @Autowired
    Message message;
    @Autowired
    ConferenceManageService conferenceManageService;

    /**
     * 预定会议
     * 前端传入一个完整的会议对象，后端写库操作
     *
     * @param conference
     * @return 预定是否成功
     * @author liumengxiao
     */
    @ApiOperation(value = "预定会议api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "conference", value = "日期", required = true, dataType = "Conference", paramType = "query"),
    })
    @RequestMapping(value = "/order", method = RequestMethod.POST)
    @ResponseBody
    public Object orderConference(Conference conference) {
        //身份信息认证
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }

        //会议预定逻辑
        try {
            if (conference == null) {
                return new FailureInfo(3000, "传入的会议参数无效！");
            } else {
                // 当前会议时间是否有会，如果无会议，继续
                List<Conference> conferenceList1 = generalService.getConferencesByDate(conference.getStartTime(), conference.getEndTime());

                // 如果当前时间段没有会议直接写数据库预定成功
                if (conferenceList1.size() != 0) {
                    // 如果当前时间段有回忆则检查这些会议是否都被驳回，如果有未被驳回的无法约定
                    for (int i = 0; i < conferenceList1.size(); i++) {
                        // 判断状态是否为正常会议
                        if (conferenceList1.get(i).getStatus() == 1) {
                            // 判断有会的是否是当前房间
                            if (conferenceList1.get(i).getRoom().equals(conference.getRoom())) {
                                return new FailureInfo(3001, "当前时段有会，无法预订！");
                            }
                        }
                    }
                }
                conferenceRepository.save(conference);
                return new SuccessInfo("会议预定成功！");
            }
        } catch (Exception e) {
            System.err.println("设置错误，详情：");
            System.err.println(e.toString());
            return new FailureInfo(3100, "预定会议出现未知错误，详情：" + e.toString());
        }
    }

    @ApiOperation(value = "我的会议信息api")
    @GetMapping(value = "/details")
    public Object getDetatils(Integer pageCurrent, Integer pageSize) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        String phone = JWTUtil.getPhoneNumber(subject.getPrincipal().toString());
        Integer userId = userRepository.getUserByPhoneNumber(phone).getUserID();
        List<Conference> conferenceList = myConferenceService.getMyConferenceList(userId, pageCurrent, pageSize);
        if (conferenceList == null) {
            return new FailureInfo(3101, "个人会议信息详情拉取失败");
        } else {
            return new SuccessInfo(conferenceList);
        }
    }

    @GetMapping(value = "/list/page")
    public Object getListPage(Integer pageCurrent, Integer pageSize) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        List<Conference> conferenceList = conferenceManageService.getPageConferenceList(pageCurrent, pageSize);
        if (conferenceList == null) {
            return new FailureInfo(3103, "所有会议信息详情拉取失败");
        } else {
            return new SuccessInfo(conferenceList);
        }
    }

    @GetMapping(value = "/page/total")
    public Object getPageTotal() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        Integer total = conferenceManageService.getPageConferenceTotal();
        if (total == null) {
            return new FailureInfo(3102, "会议信息总数拉取失败");
        } else {
            return new SuccessInfo(total);
        }
    }

    @ApiOperation(value = "我的会议信息总条数api")
    @GetMapping(value = "/total")
    public Object getTotal() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }

        Integer userId = generalService.getLoginUser().getUserID();
        Integer total = myConferenceService.getMyConferenceTotal(userId);
        if (total == null) {
            return new FailureInfo(3102, "会议信息总数拉取失败");
        } else {
            return new SuccessInfo(total);
        }
    }

    @ApiOperation(value = "取消会议api")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    @ResponseBody
    public Object cancel(Integer conferenceID) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        // 会议取消处理流程
        try {
            // 根据会议ID检索到相关会议
            Conference conference = conferenceRepository.getConferenceByConferenceID(conferenceID);
            // 将会议状态标记为取消
            conference.setStatus(-1);  //-1表示会议被取消
            // 写回数据库
            conferenceRepository.save(conference);

            //短信提醒处理
            try {
                // 获取当前日期和时间
                Calendar now = Calendar.getInstance();
                now.setTime(conference.getStartTime());
                // 年月日和时间的分割
                String year = String.valueOf(now.get(Calendar.YEAR));
                String month = String.valueOf(now.get(Calendar.MONTH) + 1);
                String day = String.valueOf(now.get(Calendar.DAY_OF_MONTH));
                String time = now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
                message.cancel(year, month, day, time, conference.getRoom().getName(), conference.getSubject());
            } catch (Exception e) {
                return new FailureInfo(3008, "会议取消短信发送失败");
            }

            return new SuccessInfo("会议取消成功！");
        } catch (Exception e) {
            System.err.println("会议取消出错！");
            return new FailureInfo(3004, "会议取消失败，请检查会议是否存在！");
        }
    }

    @ApiOperation(value = "驳回会议api")
    @RequestMapping(value = "/reject", method = RequestMethod.POST)
    @ResponseBody
    public Object reject(Integer conferenceID) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        // 会议驳回处理流程
        try {
            // 根据会议ID检索到相关会议
            Conference conference = conferenceRepository.getConferenceByConferenceID(conferenceID);
            // 将会议状态标记为取消
            conference.setStatus(0);  //0表示会议被驳回
            // 写回数据库
            conferenceRepository.save(conference);

            return new SuccessInfo("会议驳回成功！");
        } catch (Exception e) {
            System.err.println("会议驳回出错！");
            return new FailureInfo(3005, "会议驳回失败，请检查会议是否存在！");
        }
    }

    @PostMapping(value = "/participants")
    public Object participants(String userIdList, Integer conferenceID) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }

        try {
            managingAttendeesService.modifyParticipants(userIdList, conferenceID);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(3006, "与会人修改失败，请重新尝试!");
        }
        return new SuccessInfo("与会人修改成功!");
    }

    @GetMapping(value = "participants")
    public Object participantsGet(Integer conferenceID) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() == false) {
            return new FailureInfo();
        }
        try {
            return new SuccessInfo(managingAttendeesService.getParticipants(conferenceID));
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(3007, "会议与会人列表查询失败!");
        }
    }
}
