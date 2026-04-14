package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 周期计划表
 */
@Data
@TableName("calendar_cycle_plan")
public class CyclePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 计划标题
     */
    private String title;

    /**
     * 计划内容
     */
    private String content;

    /**
     * 周期类型: 1-每周 2-每月 3-自定义
     */
    private Integer cycleType;

    /**
     * 周期配置(JSON)
     */
    private String cycleConfig;

    /**
     * Cron表达式(自定义时使用)
     */
    private String cronExpression;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期(可为空,表示永久)
     */
    private LocalDate endDate;

    /**
     * 通知方式
     */
    private String notificationType;

    /**
     * 每次提醒的时间点
     */
    private LocalTime remindTime;

    /**
     * 状态: 1-启用 2-暂停 3-已结束
     */
    private Integer status;

    /**
     * 下次触发时间
     */
    private LocalDateTime nextTriggerTime;

    /**
     * 上次触发时间
     */
    private LocalDateTime lastTriggerTime;

    /**
     * Quartz任务Key
     */
    private String quartzJobKey;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记
     */
    private Integer isDelete;
}
