package com.xu.home.service.calendar.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.AppNotification;
import com.xu.home.mapper.calendar.AppNotificationMapper;
import com.xu.home.service.calendar.AppNotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用内通知服务实现
 */
@Service
public class AppNotificationServiceImpl extends ServiceImpl<AppNotificationMapper, AppNotification>
        implements AppNotificationService {

    @Override
    public List<AppNotification> getNotifications(String account) {
        return lambdaQuery()
                .eq(AppNotification::getAccount, account)
                .orderByDesc(AppNotification::getRemindTime)
                .orderByDesc(AppNotification::getCreateTime)
                .list();
    }

    @Override
    public List<AppNotification> getPendingNotifications(String account) {
        return lambdaQuery()
                .eq(AppNotification::getAccount, account)
                .eq(AppNotification::getIsRead, 0)
                .le(AppNotification::getRemindTime, LocalDateTime.now())
                .orderByDesc(AppNotification::getRemindTime)
                .list();
    }

    @Override
    public boolean markAsRead(Long id) {
        AppNotification notification = new AppNotification();
        notification.setId(id);
        notification.setIsRead(1);
        return updateById(notification);
    }

    @Override
    public boolean markAllAsRead(String account) {
        return lambdaUpdate()
                .eq(AppNotification::getAccount, account)
                .eq(AppNotification::getIsRead, 0)
                .set(AppNotification::getIsRead, 1)
                .update();
    }
}
