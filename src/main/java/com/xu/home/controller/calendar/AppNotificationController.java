package com.xu.home.controller.calendar;

import com.xu.home.domain.calendar.AppNotification;
import com.xu.home.service.calendar.AppNotificationService;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用内通知接口
 */
@RestController
@RequestMapping("/calendar/app-notification")
public class AppNotificationController {

    private final AppNotificationService appNotificationService;

    public AppNotificationController(AppNotificationService appNotificationService) {
        this.appNotificationService = appNotificationService;
    }

    /**
     * 获取当前用户的全部通知
     */
    @GetMapping("/list")
    public Response<List<AppNotification>> getNotifications() {
        String account = SessionUtil.getCurrentAccount();
        List<AppNotification> notifications = appNotificationService.getNotifications(account);
        return Response.success(notifications);
    }

    /**
     * 获取待显示的通知
     */
    @GetMapping("/pending")
    public Response<List<AppNotification>> getPendingNotifications() {
        String account = SessionUtil.getCurrentAccount();
        List<AppNotification> notifications = appNotificationService.getPendingNotifications(account);
        return Response.success(notifications);
    }

    /**
     * 标记为已读
     */
    @PostMapping("/read")
    public Response<?> markAsRead(@RequestBody IdPO po) {
        boolean result = appNotificationService.markAsRead(po.getId());
        return Response.checkResult(result);
    }

    /**
     * 标记全部已读
     */
    @PostMapping("/readAll")
    public Response<?> markAllAsRead() {
        String account = SessionUtil.getCurrentAccount();
        boolean result = appNotificationService.markAllAsRead(account);
        return Response.checkResult(result);
    }
}
