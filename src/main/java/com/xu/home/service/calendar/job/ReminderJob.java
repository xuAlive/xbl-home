package com.xu.home.service.calendar.job;

import com.xu.home.domain.calendar.Event;
import com.xu.home.domain.calendar.NotificationLog;
import com.xu.home.domain.calendar.Reminder;
import com.xu.home.mapper.calendar.NotificationLogMapper;
import com.xu.home.service.calendar.EventService;
import com.xu.home.service.calendar.NotificationService;
import com.xu.home.service.calendar.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 提醒任务Job
 * 定时扫描待发送的提醒，触发通知
 */
@Slf4j
@Component
public class ReminderJob implements Job {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private EventService eventService;

    @Autowired
    private NotificationLogMapper notificationLogMapper;

    @Autowired
    private Map<String, NotificationService> notificationServices;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("开始执行提醒任务扫描...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(1);
        LocalDateTime endTime = now.plusMinutes(1);

        // 查询待发送的提醒
        List<Reminder> reminders = reminderService.getPendingReminders(startTime, endTime);
        log.debug("找到 {} 个待发送的提醒", reminders.size());

        for (Reminder reminder : reminders) {
            try {
                processReminder(reminder);
            } catch (Exception e) {
                log.error("处理提醒失败, reminderId={}", reminder.getId(), e);
                handleReminderFailed(reminder, e.getMessage());
            }
        }

        // 检查并更新过期事件
        eventService.checkAndUpdateExpiredEvents();

        log.debug("提醒任务扫描完成");
    }

    private void processReminder(Reminder reminder) {
        // 获取关联事件信息
        String title = "日历提醒";
        String content = "您有一个待处理的事项";

        if (reminder.getEventId() != null) {
            Event event = eventService.getById(reminder.getEventId());
            if (event != null) {
                title = event.getTitle();
                content = event.getContent() != null ? event.getContent() : title;
            }
        }

        // 获取通知服务
        String notificationType = reminder.getNotificationType();
        if (notificationType == null) {
            notificationType = "wechat";
        }

        String serviceName = notificationType + "NotificationService";
        NotificationService notificationService = notificationServices.get(serviceName);

        if (notificationService == null) {
            log.warn("未找到通知服务: {}", serviceName);
            handleReminderFailed(reminder, "未找到通知服务: " + notificationType);
            return;
        }

        // 发送通知
        boolean success = notificationService.send(reminder.getAccount(), title, content);

        // 记录通知日志
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setAccount(reminder.getAccount());
        notificationLog.setSourceType(1);  // 事件提醒
        notificationLog.setSourceId(reminder.getId());
        notificationLog.setNotificationType(notificationType);
        notificationLog.setTitle(title);
        notificationLog.setContent(content);
        notificationLog.setStatus(success ? 1 : 2);
        notificationLog.setSendTime(LocalDateTime.now());
        notificationLog.setCreateTime(LocalDateTime.now());
        notificationLogMapper.insert(notificationLog);

        // 更新提醒状态
        if (success) {
            reminderService.updateStatus(reminder.getId(), 2);  // 已发送
            log.info("提醒发送成功, reminderId={}, account={}", reminder.getId(), reminder.getAccount());
        } else {
            handleReminderFailed(reminder, "通知发送失败");
        }
    }

    private void handleReminderFailed(Reminder reminder, String errorMessage) {
        int retryCount = reminder.getRetryCount() != null ? reminder.getRetryCount() : 0;
        if (retryCount >= 3) {
            // 重试次数已达上限，标记为发送失败
            reminderService.updateStatus(reminder.getId(), 3);
            log.warn("提醒发送失败次数已达上限, reminderId={}", reminder.getId());
        } else {
            // 增加重试次数
            Reminder update = new Reminder();
            update.setId(reminder.getId());
            update.setRetryCount(retryCount + 1);
            update.setUpdateTime(LocalDateTime.now());
            reminderService.updateById(update);
        }

        // 记录失败日志
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setAccount(reminder.getAccount());
        notificationLog.setSourceType(1);
        notificationLog.setSourceId(reminder.getId());
        notificationLog.setNotificationType(reminder.getNotificationType());
        notificationLog.setStatus(2);  // 失败
        notificationLog.setErrorMessage(errorMessage);
        notificationLog.setSendTime(LocalDateTime.now());
        notificationLog.setCreateTime(LocalDateTime.now());
        notificationLogMapper.insert(notificationLog);
    }
}
