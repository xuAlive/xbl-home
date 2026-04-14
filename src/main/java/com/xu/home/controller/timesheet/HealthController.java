package com.xu.home.controller.timesheet;

import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查接口
 */
@RestController("timesheetHealthController")
@RequestMapping("/timesheet")
public class HealthController {

    /**
     * 返回服务健康状态
     */
    @GetMapping("/health")
    public Response<Map<String, Object>> health() {
        return Response.success(Map.of("status", "UP", "service", "timesheet-app"));
    }
}
