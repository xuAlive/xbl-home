package com.xu.home.domain.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次表
 */
@Data
@TableName("shift")
public class Shift {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班次编码
     */
    private String shiftCode;

    /**
     * 班次名称（早班、中班、晚班、夜班等）
     */
    private String shiftName;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 班次颜色（用于前端展示）
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：1-启用 0-禁用
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

    /**
     * 是否删除：0-否 1-是
     */
    private Integer isDelete;
}
