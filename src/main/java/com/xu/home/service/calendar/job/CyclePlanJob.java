package com.xu.home.service.calendar.job;

import com.xu.home.domain.calendar.CyclePlan;
import com.xu.home.domain.calendar.Event;
import com.xu.home.domain.calendar.NotificationLog;
import com.xu.home.domain.calendar.Reminder;
import com.xu.home.mapper.calendar.NotificationLogMapper;
import com.xu.home.service.calendar.CyclePlanService;
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
 * 周期计划任务Job
 * 根据周期计划生成下一次的事件和提醒
 */
@Slf4j
@Component
public class CyclePlanJob implements Job {

    @Autowired
    private CyclePlanService cyclePlanService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private NotificationLogMapper notificationLogMapper;

    @Autowired
    private Map<String, NotificationService> notificationServices;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("开始执行周期计划任务扫描...");

        LocalDateTime now = LocalDateTime.now();

        // 查询需要触发的计划
        List<CyclePlan> plans = cyclePlanService.getPlansToTrigger(now);
        log.debug("找到 {} 个需要触发的周期计划", plans.size());

        for (CyclePlan plan : plans) {
            try {
                processCyclePlan(plan);
            } catch (Exception e) {
                log.error("处理周期计划失败, planId={}", plan.getId(), e);
            }
        }

        log.debug("周期计划任务扫描完成");
    }

    private void processCyclePlan(CyclePlan plan) {
        log.info("处理周期计划, planId={}, title={}", plan.getId(), plan.getTitle());

        // 1. 创建事件
        Event event = new Event();
        event.setAccount(plan.getAccount());
        event.setTitle(plan.getTitle());
        event.setContent(plan.getContent());
        event.setEventDate(plan.getNextTriggerTime().toLocalDate());
        event.setEventTime(plan.getRemindTime());
        event.setEventType(1);  // 待办
        event.setStatus(1);  // 待完成
        event.setPriority(2);  // 中
        event.setCreateTime(LocalDateTime.now());
        event.setUpdateTime(LocalDateTime.now());
        event.setIsDelete(0);
        eventService.save(event);
        log.debug("创建事件成功, eventId={}", event.getId());

        // 2. 创建提醒
        if (plan.getNotificationType() != null) {
            Reminder reminder = new Reminder();
            reminder.setEventId(event.getId());
            reminder.setAccount(plan.getAccount());
            reminder.setRemindTime(plan.getNextTriggerTime());
            reminder.setRemindBefore(0);
            reminder.setNotificationType(plan.getNotificationType());
            reminder.setStatus(1);  // 待发送
            reminder.setRetryCount(0);
            reminder.setCreateTime(LocalDateTime.now());
            reminder.setUpdateTime(LocalDateTime.now());
            reminder.setIsDelete(0);
            reminderService.save(reminder);
            log.debug("创建提醒成功, reminderId={}", reminder.getId());
        }

        // 3. 发送即时通知
        sendNotification(plan);

        // 4. 计算并更新下次触发时间
        LocalDateTime nextTrigger = cyclePlanService.calculateNextTriggerTime(plan);
        if (nextTrigger != null) {
            cyclePlanService.updateNextTriggerTime(plan.getId(), nextTrigger);
            log.debug("更新下次触发时间, planId={}, nextTrigger={}", plan.getId(), nextTrigger);
        } else {
            // 计划已结束
            CyclePlan update = new CyclePlan();
            update.setId(plan.getId());
            update.setStatus(3);  // 已结束
            update.setUpdateTime(LocalDateTime.now());
            cyclePlanService.updateById(update);
            log.info("周期计划已结束, planId={}", plan.getId());
        }
    }

    private void sendNotification(CyclePlan plan) {
        String notificationType = plan.getNotificationType();
        if (notificationType == null) {
            return;
        }

        String serviceName = notificationType + "NotificationService";
        NotificationService notificationService = notificationServices.get(serviceName);

        if (notificationService == null) {
            log.warn("未找到通知服务: {}", serviceName);
            return;
        }

        boolean success = notificationService.send(plan.getAccount(), plan.getTitle(), plan.getContent());

        // 记录通知日志
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setAccount(plan.getAccount());
        notificationLog.setSourceType(2);  // 周期计划
        notificationLog.setSourceId(plan.getId());
        notificationLog.setNotificationType(notificationType);
        notificationLog.setTitle(plan.getTitle());
        notificationLog.setContent(plan.getContent());
        notificationLog.setStatus(success ? 1 : 2);
        notificationLog.setSendTime(LocalDateTime.now());
        notificationLog.setCreateTime(LocalDateTime.now());
        notificationLogMapper.insert(notificationLog);

        log.info("周期计划通知发送{}, planId={}", success ? "成功" : "失败", plan.getId());
    }
}
