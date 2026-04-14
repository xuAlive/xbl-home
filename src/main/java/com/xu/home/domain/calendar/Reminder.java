package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒表
 */
@Data
@TableName("calendar_reminder")
public class Reminder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联事件ID
     */
    private Long eventId;

    /**
     * 提醒标题(不超过20字)
     */
    private String title;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 提醒时间
     */
    private LocalDateTime remindTime;

    /**
     * 提前多少分钟提醒
     */
    private Integer remindBefore;

    /**
     * 通知方式(wechat,sms,dingtalk)
     */
    private String notificationType;

    /**
     * 状态: 1-待发送 2-已发送 3-发送失败
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

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
