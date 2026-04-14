package com.xu.home.domain.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班记录表
 */
@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工账号（关联 sys_user.account）
     */
    private String account;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 班次ID
     */
    private Long shiftId;

    /**
     * 班次名称（冗余字段，方便查询）
     */
    private String shiftName;

    /**
     * 状态：1-正常 2-请假 3-调休 4-加班
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 是否删除：0-否 1-是
     */
    private Integer isDelete;
}
