package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记工记录表
 */
@Data
@TableName("timesheet_attendance_record")
public class TimesheetAttendanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目成员ID
     */
    private Long memberId;

    /**
     * 成员账号
     */
    private String memberAccount;

    /**
     * 成员姓名
     */
    private String memberName;

    /**
     * 记工日期
     */
    private LocalDate workDate;

    /**
     * 上班签到时间
     */
    private LocalDateTime signInTime;

    /**
     * 离班签到时间
     */
    private LocalDateTime signOutTime;

    /**
     * 当天总工时
     */
    private BigDecimal workHours;

    /**
     * 当天工数
     */
    private BigDecimal workUnits;

    /**
     * 1-待签离 2-已完成 3-已补签通过
     */
    private Integer recordStatus;

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
     * 是否删除：0-否 1-是
     */
    private Integer isDelete;
}
