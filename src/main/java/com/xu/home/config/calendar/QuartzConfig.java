package com.xu.home.config.calendar;

import com.xu.home.service.calendar.job.CyclePlanJob;
import com.xu.home.service.calendar.job.ReminderJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 定时任务配置类
 */
@Configuration
public class QuartzConfig {

    /**
     * 提醒任务 JobDetail
     */
    @Bean
    public JobDetail reminderJobDetail() {
        return JobBuilder.newJob(ReminderJob.class)
                .withIdentity("reminderJob", "calendar")
                .storeDurably()
                .build();
    }

    /**
     * 提醒任务 Trigger - 每分钟执行一次
     */
    @Bean
    public Trigger reminderJobTrigger(@Qualifier("reminderJobDetail") JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("reminderTrigger", "calendar")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }

    /**
     * 周期计划任务 JobDetail
     */
    @Bean
    public JobDetail cyclePlanJobDetail() {
        return JobBuilder.newJob(CyclePlanJob.class)
                .withIdentity("cyclePlanJob", "calendar")
                .storeDurably()
                .build();
    }

    /**
     * 周期计划任务 Trigger - 每5分钟执行一次
     */
    @Bean
    public Trigger cyclePlanJobTrigger(@Qualifier("cyclePlanJobDetail") JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("cyclePlanTrigger", "calendar")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))
                .build();
    }
}
