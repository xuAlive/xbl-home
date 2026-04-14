package com.xu.home.param.calendar.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 周期计划VO
 */
@Data
public class CyclePlanVO {

    private Long id;

    private String account;

    private String title;

    private String content;

    /**
     * 周期类型: 1-每周 2-每月 3-自定义
     */
    private Integer cycleType;

    private String cycleConfig;

    private String cronExpression;

    private LocalDate startDate;

    private LocalDate endDate;

    private String notificationType;

    private LocalTime remindTime;

    /**
     * 状态: 1-启用 2-暂停 3-已结束
     */
    private Integer status;

    private LocalDateTime nextTriggerTime;

    private LocalDateTime lastTriggerTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
