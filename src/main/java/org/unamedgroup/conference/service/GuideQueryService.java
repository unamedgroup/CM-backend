package org.unamedgroup.conference.service;

import javafx.stage.Screen;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.Room;
import org.unamedgroup.conference.entity.temp.PageRoomTime;
import org.unamedgroup.conference.entity.temp.RoomTime;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 向导查询相关业务
 *
 * @author liumengxiao
 * @date 2019-04-09
 */

public interface GuideQueryService {
    /**
     * 根据起止时间段查询该时间段的空闲会议室
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 会议室列表
     */
    public List<Room> getFreeRoomIDByDate(Date start, Date end);

    /**
     * 查询所有会议室信息
     * @return 所有会议室信息列表
     *
     */
    List<Room> getAllRoom();

    /**
     * 按房间在时间区间内房间的空闲指数进行排序，每半个小时空闲累积一个空闲指数
     * @param start 开始时间
     * @param end 结束时间
     * @param roomList 房间列表
     * @return 排序后的房间信息列表
     */
    List<Room> sortRoomByFreeIndex(List<Room> roomList, Date start, Date end);


    /**
     * 计算每个房间的空闲指数并存在TreeMap中（排序）
     * @param roomList 房间列表
     * @param start 开始时间
     * @param end 结束时间
     * @return 房间及其空闲指数的映射
     */
    Map<Room, Integer> calculateRoomFreeIndex(List<Room> roomList, Date start, Date end);

    /**
     * 将排序的空闲指数和房间的键值对顺序转换为房间信息列表
     * @param roomMap 空闲指数房间信息键值对
     * @return 排序后的房间信息列表
     */
    List<Room> transformRoom(Map<Room, Integer> roomMap);


    /**
     * 通过会议室参数筛选符合条件的会议室
     * @param room 会议室筛选参数信息
     * @return 会议室列表
     */
    List<Room> screenRoomList(Room room);


    /**
     * 将会议室列表中的会议室查询出时间块信息并返回
     * @param roomList 会议室列表
     * @param date 日期
     * @return 会议室时间块信息列表
     */
    List<RoomTime> roomTable(List<Room> roomList,String date);

    /**
     * 房间location信息转换
     * @param room 房间信息
     * @return 转换之后的房间信息
     */
    Room locationShift(Room room);

    /**
     * 去重查询所有会议室类别
     * @return 会议室类别列表
     */
    List<String> getAllCatalogue();

    /**
     * 会议室时间块信息列表分页
     * @param roomTimeList 会议室时间块列表（分页前）
     * @param pageCurrent 当前页
     * @param pageSize 页大小
     * @return 会议室时间块列表（分页后，含总页数）
     */
    PageRoomTime pageRoomTimeList(List<RoomTime> roomTimeList, Integer pageCurrent, Integer pageSize);

    /**
     * 会议室id列表获取会议室详情列表
     * @param rooms 会议室id列表
     * @return 会议室详情列表
     */
    List<Room> getRoomList(List<Integer> rooms);
}
