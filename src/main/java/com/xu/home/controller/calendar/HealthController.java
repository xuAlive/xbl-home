package com.xu.home.controller.calendar;

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
@RestController("calendarHealthController")
@RequestMapping("/calendar")
public class HealthController {

    @GetMapping("/health")
    public Response<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "calendar-app");
        data.put("time", LocalDateTime.now().toString());
        return Response.success(data);
    }
}
