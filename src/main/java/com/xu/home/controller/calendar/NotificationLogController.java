package com.xu.home.controller.calendar;

import com.xu.home.domain.calendar.NotificationLog;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.calendar.NotificationLogService;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知日志接口
 */
@RestController
@RequestMapping("/calendar/notification-log")
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    public NotificationLogController(NotificationLogService notificationLogService) {
        this.notificationLogService = notificationLogService;
    }

    /**
     * 获取当前用户的通知投递记录
     */
    @GetMapping("/list")
    public Response<List<NotificationLog>> getLogs() {
        String account = SessionUtil.getCurrentAccount();
        List<NotificationLog> logs = notificationLogService.getLogsByAccount(account);
        return Response.success(logs);
    }
}
