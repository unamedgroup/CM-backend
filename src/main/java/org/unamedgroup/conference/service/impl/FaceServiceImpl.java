package org.unamedgroup.conference.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.service.GeneralService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 人脸服务类
 * 错误代码使用1xxx
 *
 * @author zhoutao
 * @date 2019/06/18
 */
@Component
public class FaceServiceImpl {
    @Autowired
    ParticipantsServiceImpl participantsService;
    @Autowired
    GeneralService generalService;

    /**
     * 获取与指定会议的所有参与者
     * @param roomID
     * @param now
     * @param date
     * @return
     */
    public List<Integer> getUserIDFromConference(Integer roomID, Date now, Date date){
        List<Conference> conferenceList = generalService.getConferencesByLocationAndDate(roomID, now, date);
        if(conferenceList.isEmpty()){
            return null;
        }
        List<Integer> userIdList = new ArrayList<>();
        userIdList.add(conferenceList.get(0).getUser());
        userIdList.addAll(participantsService.getParticipantsID(conferenceList.get(0).getConferenceID()));
        return userIdList;
    }
}
