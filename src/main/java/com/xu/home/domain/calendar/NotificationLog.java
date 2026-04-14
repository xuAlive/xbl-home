package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知日志表
 */
@Data
@TableName("calendar_notification_log")
public class NotificationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 来源类型: 1-事件提醒 2-周期计划
     */
    private Integer sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 状态: 1-成功 2-失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
