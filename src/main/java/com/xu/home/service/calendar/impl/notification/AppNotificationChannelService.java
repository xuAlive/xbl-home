package com.xu.home.service.calendar.impl.notification;

import com.xu.home.domain.calendar.AppNotification;
import com.xu.home.service.calendar.AppNotificationService;
import com.xu.home.service.calendar.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 应用内通知发送实现
 */
@Service("appNotificationService")
public class AppNotificationChannelService implements NotificationService {

    private final AppNotificationService appNotificationService;

    public AppNotificationChannelService(AppNotificationService appNotificationService) {
        this.appNotificationService = appNotificationService;
    }

    @Override
    public boolean send(String account, String title, String content) {
        AppNotification notification = new AppNotification();
        notification.setAccount(account);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRemindTime(LocalDateTime.now());
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        return appNotificationService.save(notification);
    }

    @Override
    public String getType() {
        return "app";
    }
}
