package com.xu.home.domain.calendar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 事件/记事表
 */
@Data
@TableName("calendar_event")
public class Event {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 事件标题
     */
    private String title;

    /**
     * 事件内容/记事详情
     */
    private String content;

    /**
     * 事件日期
     */
    private LocalDate eventDate;

    /**
     * 事件时间(可为空)
     */
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
     * 心情(emoji或文字)
     */
    private String mood;

    /**
     * 标签(JSON数组)
     */
    private String tags;

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
