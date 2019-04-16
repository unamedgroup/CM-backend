package org.unamedgroup.conference.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.unamedgroup.conference.entity.Room;
import org.unamedgroup.conference.entity.temp.FailureInfo;
import org.unamedgroup.conference.entity.temp.RoomTime;
import org.unamedgroup.conference.entity.temp.SuccessInfo;
import org.unamedgroup.conference.service.GuideQueryService;
import org.unamedgroup.conference.service.QuickCheckService;
import org.unamedgroup.conference.service.RelevanceQueryService;

import java.util.Date;
import java.util.List;

/**
 * RoomController
 * 错误代码使用6xxx
 *
 * @author liumengxiao
 * @date 2019/03/12
 */

@Api(value = "会议室操作 API", description = "会议室操作 API", position = 100, protocols = "http")
@CrossOrigin
@RestController
@RequestMapping("/room")
public class RoomController {
    @Autowired
    QuickCheckService quickCheckService;
    @Autowired
    GuideQueryService guideQueryService;
    @Autowired
    RelevanceQueryService relevanceQueryService;

    @RequestMapping(value = "/free", method = RequestMethod.GET)
    @ResponseBody
    public Object getFreeRoom(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        if (guideQueryService.getFreeRoomIDByDate(start, end) == null) {
            return new FailureInfo(6000, "查询空闲房间失败！请核实！");
        } else {
            return new SuccessInfo(guideQueryService.getFreeRoomIDByDate(start, end));
        }
    }

    @ApiOperation(value = "快速查看会议室api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = RoomTime.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "date", value = "日期", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "buildingID", value = "楼宇编号", required = false, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "roomID", value = "房间编号", required = false, dataType = "int", paramType = "query")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "请求成功")
    })
    @GetMapping(value = "list")
    public Object getList(String date, Integer buildingID, Integer roomID) {
        List<RoomTime> roomTimeList = quickCheckService.handleRoomTime(date, buildingID, roomID);
        if (roomTimeList == null) {
            return new FailureInfo(6001, "处理房间填充失败！");
        } else {
            return new SuccessInfo(roomTimeList);
        }
    }

    @GetMapping(value = "list/pre")
    public Object listPre(Integer buildingID, Integer roomID) {
        List<Room> roomList = quickCheckService.getConferenceList(buildingID, roomID);
        if(roomList == null) {
            return new FailureInfo(6002, "处理房间预处理失败！");
        } else {
            return new SuccessInfo(roomList);
        }
    }

    @GetMapping(value = "list/building")
    public Object listByBuilding(Integer buildingID) {
        List<Room> roomList = relevanceQueryService.roomByBuilding(buildingID);
        if(roomList == null) {
            return new FailureInfo(6003, "获取会议室失败!");
        } else {
            return new SuccessInfo(roomList);
        }
    }
}
