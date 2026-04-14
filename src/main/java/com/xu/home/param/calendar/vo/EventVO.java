package com.xu.home.param.calendar.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 事件VO
 */
@Data
public class EventVO {

    private Long id;

    private String account;

    private String title;

    private String content;

    private LocalDate eventDate;

    private LocalTime eventTime;

    /**
     * 类型: 1-待办 2-记事 3-纪念日
     */
    private Integer eventType;

    /**
     * 状态: 1-待完成 2-已完成 3-已过期
     */
    private Integer status;

    /**
     * 优先级: 1-低 2-中 3-高
     */
    private Integer priority;

    /**
     * 心情
     */
    private String mood;

    private String tags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
