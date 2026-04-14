package com.xu.home.param.calendar.po;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 周期计划PO
 */
@Data
public class CyclePlanPO {

    private Long id;

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
}
