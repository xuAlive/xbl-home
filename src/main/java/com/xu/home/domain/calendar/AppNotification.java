package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用内通知表
 */
@Data
@TableName("calendar_app_notification")
public class AppNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 提醒时间
     */
    private LocalDateTime remindTime;

    /**
     * 来源: 1-事件提醒 2-周期计划
     */
    private Integer sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 是否已读: 0-否 1-是
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
