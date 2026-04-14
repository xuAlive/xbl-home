package com.xu.home.controller.schedule;

import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口（无需登录）
 */
@RestController("scheduleHealthController")
@RequestMapping("/schedule")
public class HealthController {

    @GetMapping("/health")
    public Response<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "schedule-app");
        data.put("time", LocalDateTime.now().toString());
        return Response.success(data);
    }
}
