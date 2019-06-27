package org.unamedgroup.conference.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.unamedgroup.conference.entity.Building;
import org.unamedgroup.conference.entity.Room;

import java.util.List;

@Repository
public interface RoomRepository extends CrudRepository<Room, Integer> {
//    List<Room> getRoomsByFlag(Integer flag);

    /**
     * 根据楼宇查找楼宇内所有的会议室
     * 应王宇辰前端快速查找要求
     *
     * @param building
     * @return 会议室列表
     */
    List<Room> getRoomsByBuilding(Building building);

    /**
     * 根据楼层位置信息返回可用会议室
     * 应王宇辰前端快速查找要求
     *
     * @param location
     * @return 会议室列表
     */
    List<Room> getRoomsByLocation(String location);

    /**
     * 根据房间名得到房间实体
     *
     * @param name
     * @return 房间实体
     */
    Room getRoomByName(String name);

    /**
     * 根据房间号得到房间实体
     *
     * @param roomID
     * @return 房间实体
     */
    Room getRoomByRoomID(Integer roomID);

    /**
     * 根据房间号列表返房间信息列表
     * @param rooms 房间号列表
     * @return 房间实体列表
     */
    List<Room> getRoomsByRoomIDIn(List rooms);

    /**
     * 返回所有房间
     * 请慎用此方法！
     *
     * @return 所有房间列表
     */
    @Override
    List<Room> findAll();

//    List<Room> getRoomsByBuilding(Integer building);

    /**
     * 去重查询所有的会议室类型
     * @return 会议室类型列表
     */
    @Query(value = "select distinct catalogue from Room ")
    List<String> findDistinctCatalogue();

    /**
     * 分页查询房间信息
     * @param pageNumber 起始索引
     * @param pageSize 每页容量
     * @return 房间页信息
     */
    @Query(value = "select * from room limit ?1, ?2", nativeQuery = true)
    List<Room> findRoomPage(Integer pageNumber, Integer pageSize);

    /**
     * 查询房间信息总条数
     * @return 房间信息条数
     */
    @Query(value = "select count(*) from room", nativeQuery = true)
    Integer countRoom();

    @Transactional
    @Modifying
    Integer deleteByRoomID(Integer roomID);

    /**
     * 会议室全信息模糊匹配
     * @param id 会议室编号
     * @param capacity 会议室容量
     * @param catalogue 会议室类型
     * @param location 会议室位置
     * @param name 会议室名称
     * @param building 会议室所在楼宇
     * @return 匹配到的会议室信息列表
     */
    List<Room> findByRoomIDOrCapacityOrCatalogueLikeOrLocationLikeOrNameLikeOrBuilding(Integer id, Integer capacity, String catalogue, String location, String name, Building building);

}
