package com.xu.home.param.calendar.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒PO
 */
@Data
public class ReminderPO {

    private Long id;

    private Long eventId;

    /**
     * 提醒标题(不超过20字)
     */
    private String title;

    private LocalDateTime remindTime;

    private Integer remindBefore;

    private String notificationType;
}
