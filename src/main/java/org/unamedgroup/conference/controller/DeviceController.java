package org.unamedgroup.conference.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.unamedgroup.conference.entity.Conference;
import org.unamedgroup.conference.entity.temp.FailureInfo;
import org.unamedgroup.conference.entity.temp.RoomTime;
import org.unamedgroup.conference.entity.temp.SuccessInfo;
import org.unamedgroup.conference.service.DevicesControlService;

/**
 * DeviceController
 * 错误代码使用4xxx
 *
 * @author liumengxiao
 * @date 2019/04/03
 */

@Api(value = "设备 API", description = "设备控制接口", protocols = "http")
@CrossOrigin
@RestController
@RequestMapping(value = "devices")
public class DeviceController {
    @Autowired
    DevicesControlService devicesControl;

    @ApiOperation(value = "开门操作api", protocols = "http"
            , produces = "application/json", consumes = "application/json"
            , response = Boolean.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userID", value = "用户编号", required = true, dataType = "int", paramType = "query")
    })
    @RequestMapping(value = "/door", method = RequestMethod.POST)
    @ResponseBody
    public Object openDoor(Integer userID) {
        if (devicesControl.openDoor(userID) == true) {
            return new SuccessInfo(true);
        } else {
            return new FailureInfo(4000, "开门失败，请重试！");
        }
    }

    @ApiOperation(value = "开启空调")
    @RequestMapping(value = "/airCondition", method = RequestMethod.POST)
    @ResponseBody
    public Object airCondition() {
        return null;
    }

    @ApiOperation(value = "开启投影仪")
    @RequestMapping(value = "/projector", method = RequestMethod.POST)
    @ResponseBody
    public Object projector() {
        return null;
    }

}

