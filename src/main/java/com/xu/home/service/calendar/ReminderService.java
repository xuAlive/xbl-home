package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.Reminder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒服务接口
 */
public interface ReminderService extends IService<Reminder> {

    /**
     * 查询用户的提醒列表
     */
    List<Reminder> getRemindersByAccount(String account);

    /**
     * 查询待发送的提醒
     */
    List<Reminder> getPendingReminders(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 更新提醒状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 取消提醒
     */
    boolean cancelReminder(Long id);

    /**
     * 重试失败的提醒
     */
    boolean retryReminder(Long id);
}
