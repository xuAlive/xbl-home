package com.xu.home.param.calendar.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒VO
 */
@Data
public class ReminderVO {

    private Long id;

    private Long eventId;

    /**
     * 提醒标题
     */
    private String title;

    private String account;

    private LocalDateTime remindTime;

    private Integer remindBefore;

    private String notificationType;

    /**
     * 状态: 1-待发送 2-已发送 3-发送失败
     */
    private Integer status;

    private Integer retryCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
