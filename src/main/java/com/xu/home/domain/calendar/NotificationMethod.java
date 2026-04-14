package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知方式配置表
 */
@Data
@TableName("calendar_notification_method")
public class NotificationMethod {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知方式编码
     */
    private String code;

    /**
     * 通知方式名称
     */
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 实现状态: 0-未实现 1-已实现
     */
    private Integer implStatus;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1-启用 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
