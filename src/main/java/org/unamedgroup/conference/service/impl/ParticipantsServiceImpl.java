package org.unamedgroup.conference.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unamedgroup.conference.dao.ParticipantsRepository;
import org.unamedgroup.conference.dao.UserRepository;
import org.unamedgroup.conference.entity.Participants;
import org.unamedgroup.conference.entity.User;
import org.unamedgroup.conference.entity.temp.ReturnUser;
import org.unamedgroup.conference.service.ManagingAttendeesService;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 白振宇、周韬
 * @Date： 2019/5/12 22:16
 */
@Service
public class ParticipantsServiceImpl implements ManagingAttendeesService {
    @Autowired
    ParticipantsRepository participantsRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public Boolean modifyParticipants(String userIds, Integer conferenceID) {
        List<Integer> userIdList = new ArrayList<>();
        String[] userIdArray = userIds.split(",");
        for(int i=0; i<userIdArray.length; i++) {
            userIdList.add(Integer.valueOf(userIdArray[i]));
        }
        try {
            participantsRepository.removeBySequenceID(conferenceID);
            for(Integer userId : userIdList) {
                Participants participants = new Participants();
                participants.setUserID(userId);
                participants.setSequenceID(conferenceID);
                participantsRepository.save(participants);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public List<ReturnUser> getParticipants(Integer conferenceID) {
        List<Participants> participantsList = participantsRepository.findBySequenceID(conferenceID);
        List<ReturnUser> userList = new ArrayList<>();
        for(Participants participants : participantsList) {
            User user = userRepository.getUserByUserID(participants.getUserID());
            ReturnUser returnUser = new ReturnUser(user);
            userList.add(returnUser);
        }
        return userList;
    }

    /**
     * 获取会议室参与者的ID集合
     * @param conferenceID
     * @return userIDList 参与者ID集合
     */
    public List<Integer> getParticipantsID(Integer conferenceID){
        List<Participants> participantsList = participantsRepository.findBySequenceID(conferenceID);
        List<Integer> userIDList = new ArrayList<>();
        for (Participants participant: participantsList) {
            userIDList.add(participant.getUserID());
        }
        return userIDList;
    }
}
