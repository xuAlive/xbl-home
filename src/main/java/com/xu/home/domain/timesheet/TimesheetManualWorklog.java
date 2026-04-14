package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人工记工记录表
 */
@Data
@TableName("timesheet_manual_worklog")
public class TimesheetManualWorklog {

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
     * 人工维护的工数
     */
    private BigDecimal workUnits;

    /**
     * 备注
     */
    private String remark;

    /**
     * 维护人账号
     */
    private String maintainedBy;

    /**
     * 维护人姓名
     */
    private String maintainedByName;

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
