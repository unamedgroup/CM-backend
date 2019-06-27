package org.unamedgroup.conference.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.*;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.unamedgroup.conference.dao.RoomRepository;
import org.unamedgroup.conference.entity.Building;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.Room;
import org.unamedgroup.conference.entity.temp.*;
import org.unamedgroup.conference.service.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * RoomController
 * 错误代码使用6xxx
 *
 * @author liumengxiao
 * @date 2019/03/12
 */

@Api(value = "会议室 API", description = "会议室操作接口", protocols = "http")
@CrossOrigin
@RestController
@RequestMapping("/room")
public class RoomController {
    @Autowired
    RoomManageService roomManageService;
    @Autowired
    QuickCheckService quickCheckService;
    @Autowired
    GuideQueryService guideQueryService;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    RelevanceQueryService relevanceQueryService;
    @Autowired
    GeneralService generalService;

    @Value("${server.python.url}")
    private String python_url;

    @ApiOperation(value = "获取空闲会议室api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Integer.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "start", value = "开始时间", required = true, dataType = "Date", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "结束时间", required = true, dataType = "Date", paramType = "query"),
    })
    @RequestMapping(value = "/free", method = RequestMethod.GET)
    @ResponseBody
    public Object getFreeRoom(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        if (guideQueryService.getFreeRoomIDByDate(start, end) == null) {
            return new FailureInfo(6000, "没有房间空闲或查询失败！请核实！");
        } else {
            return new SuccessInfo(guideQueryService.getFreeRoomIDByDate(start, end));
        }
    }

    @ApiOperation(value = "当天空闲会议室信息查询api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Integer.class)
    @RequestMapping(value = "/freeRoomNumber", method = RequestMethod.GET)
    @ResponseBody
    public Object getFreeRoomNumberToday() {
        Date current = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        // 日期后移一天
        calendar.add(Calendar.DAY_OF_MONTH, +1);
        // 小时置零
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        // 分置零
        calendar.set(Calendar.MINUTE, 0);
        // 秒置零
        calendar.set(Calendar.SECOND, 0);
        // 毫秒置零
        calendar.set(Calendar.MILLISECOND, 0);


        if (guideQueryService.getFreeRoomIDByDate(current, calendar.getTime()) == null) {
            return new FailureInfo(6000, "没有房间空闲或查询失败！请核实！");
        } else {
            return new SuccessInfo(guideQueryService.getFreeRoomIDByDate(current, calendar.getTime()).size());
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
    @GetMapping(value = "list")
    public Object getList(String date, Building building, Integer roomID) {
        if (date == null || building == null || roomID == null) {
            return new FailureInfo(6001, "处理房间填充失败！");
        }
        List<Room> roomList = quickCheckService.getConferenceList(building, roomID);
        List<RoomTime> roomTimeList = guideQueryService.roomTable(roomList, date);
        if (roomTimeList == null) {
            return new FailureInfo(6001, "处理房间填充失败！");
        } else {
            return new SuccessInfo(roomTimeList);
        }
    }

    @ApiOperation(value = "会议室列表预处理api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Room.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "buildingID", value = "楼宇编号", required = false, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "roomID", value = "房间编号", required = false, dataType = "int", paramType = "query")
    })
    @GetMapping(value = "list/pre")
    public Object listPre(Building building, Integer roomID) {
        List<Room> roomList = quickCheckService.getConferenceList(building, roomID);
        if(roomList == null) {
            return new FailureInfo(6004, "处理房间预处理失败！");
        } else {
            return new SuccessInfo(roomList);
        }
    }

    @ApiOperation(value = "会议室级联查询api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Room.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "buildingID", value = "楼宇编号", required = true, dataType = "int", paramType = "query"),
    })
    @GetMapping(value = "list/building")
    public Object listByBuilding(Building building) {
        List<Room> roomList = relevanceQueryService.roomByBuilding(building);
        if(roomList == null) {
            return new FailureInfo(6003, "获取会议室失败!");
        } else {
            return new SuccessInfo(roomList);
        }
    }

    @ApiOperation(value = "获取会议室实体api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Room.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roomID", value = "房间编号", required = true, dataType = "int", paramType = "query")
    })
    @RequestMapping(value = "/roomObject", method = RequestMethod.GET)
    @ResponseBody
    public Object getRoomObject(Integer roomID) {
        try {
            Room room = roomRepository.getRoomByRoomID(roomID);
            if (room != null) {
                return new SuccessInfo(room);
            } else {
                throw new NullPointerException();
            }
        } catch (Exception e) {
            return new FailureInfo(6002, "找不到满足条件的房间！");
        }
    }

    @GetMapping(value = "guide")
    public Object guide(Date start, Date end, Room room, Building building) {
        try{
            room.setBuilding(building);
//        room = guideQueryService.locationShift(room);
            List<Room> roomList = guideQueryService.screenRoomList(room);
            roomList = guideQueryService.sortRoomByFreeIndex(roomList, start, end);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = simpleDateFormat.format(start);
            List<RoomTime> roomTimeList = guideQueryService.roomTable(roomList, date);
            if(roomTimeList!=null) {
                return new SuccessInfo(roomTimeList);
            } else {
                return new FailureInfo(6001, "处理房间填充失败!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6001, "处理房间填充失败！");
        }
    }

    @GetMapping(value = "guide/page")
    public Object guidePage(Integer pageCurrent, Integer pageSize, Date start, Date end, Room room, Building building) {
        room.setBuilding(building);
        List<Room> roomList = guideQueryService.screenRoomList(room);
        roomList = guideQueryService.sortRoomByFreeIndex(roomList, start, end);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(start);
        List<RoomTime> roomTimeList = guideQueryService.roomTable(roomList, date);
        PageRoomTime pageRoomTime = guideQueryService.pageRoomTimeList(roomTimeList, pageCurrent, pageSize);
        if(pageRoomTime!=null) {
            return new SuccessInfo(pageRoomTime);
        } else {
            return new FailureInfo(6001, "处理房间填充失败!");
        }
    }

    @GetMapping(value = "catalogue")
    public Object catalogue() {
        try {
            List<String> catalogueList = guideQueryService.getAllCatalogue();
            return new SuccessInfo(catalogueList);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6005, "获取会议室类型列表出错！");
        }
    }

    @GetMapping(value = "list/room")
    public Object listRoom(Integer pageCurrent, Integer pageSize) {
        try {
            List<Room> roomList = roomManageService.getPageRoomInfo(pageCurrent, pageSize);
            return new SuccessInfo(roomList);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6006, "分页查询所有房间信息失败！");
        }
    }

    @GetMapping(value = "total")
    public Object total() {
        try {
            Integer total = roomManageService.totalPageRomInfo();
            return new SuccessInfo(total);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6007, "获取会议室信息总条数失败！");
        }
    }

    @PostMapping(value = "delete")
    public Object delete(Integer roomID) {
        try {
            Subject subject = SecurityUtils.getSubject();
            if (!subject.isAuthenticated()) {
                return new FailureInfo();
            }
            Integer count = roomManageService.deleteRoomByRoomID(roomID);
            return new SuccessInfo(count);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6008, "删除会议室记录失败！");
        }
    }

    @PostMapping(value = "modify")
    public Object modify(Room room) {
        if(room.getFlag()==null) {
            room.setFlag(0);
        }
        if(room.getBuilding()==null) {
            return new FailureInfo(6011, "无此楼宇！");
        }
        try {
            Subject subject = SecurityUtils.getSubject();
            if(!subject.isAuthenticated()) {
                return new FailureInfo();
            }
            Room roomAfter = roomManageService.modifyRoom(room);
            return new SuccessInfo(roomAfter);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6009, "修改会议室信息失败！");
        }
    }

    @GetMapping(value = "match")
    public Object match(String params) {
        try {
            Set<Room> roomSet = roomManageService.allFuzzyMatching(params);
            return new SuccessInfo(roomSet);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6010, "模糊匹配异常！");
        }
    }

    @GetMapping(value = "match/page")
    public Object matchPage(String params, Integer pageCurrent, Integer pageSize) {
        try {
            Set<Room> roomSet = roomManageService.allFuzzyMatching(params);
            PageRoom roomSetPage = roomManageService.pageRoomSet(roomSet, pageCurrent, pageSize);
            return new SuccessInfo(roomSetPage);
        } catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6010, "模糊匹配异常！");
        }
    }

    @GetMapping(value = "intelligence")
    public Object intelligence() {
        try {
            Subject subject = SecurityUtils.getSubject();
            if(!subject.isAuthenticated()) {
                return new FailureInfo();
            }
            Integer userID = generalService.getLoginUser().getUserID();
            RestTemplate restTemplate = new RestTemplate();

            String url = python_url;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
            map.add("userID", String.valueOf(userID));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity( url, request , String.class );
            Map maps = (Map) JSON.parse(response.getBody());
            assert maps != null;
            String jsonStr = JSONObject.toJSONString(maps.get("data"));
            List<Integer> list = JSONObject.parseArray(jsonStr,  Integer.class);
            List<Room> roomList = new ArrayList<>();
            for(Integer i : list) {
                roomList.add(roomRepository.getRoomByRoomID(i));
            }
            return new SuccessInfo(roomList);
        }catch (Exception e) {
            e.printStackTrace();
            return new FailureInfo(6011, "智能推荐失败！");
        }
    }
}
