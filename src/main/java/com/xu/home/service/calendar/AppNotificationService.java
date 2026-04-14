package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.AppNotification;

import java.util.List;

/**
 * 应用内通知服务接口
 */
public interface AppNotificationService extends IService<AppNotification> {

    /**
     * 获取用户的全部通知
     */
    List<AppNotification> getNotifications(String account);

    /**
     * 获取用户的待显示通知（当前时间之前且未读）
     */
    List<AppNotification> getPendingNotifications(String account);

    /**
     * 标记为已读
     */
    boolean markAsRead(Long id);

    /**
     * 标记全部已读
     */
    boolean markAllAsRead(String account);
}
