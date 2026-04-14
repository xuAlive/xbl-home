package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算明细表
 */
@Data
@TableName("timesheet_settlement_item")
public class TimesheetSettlementItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属结算单ID
     */
    private Long settlementId;

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
     * 工作日期
     */
    private LocalDate workDate;

    /**
     * 1-签到 2-人工维护
     */
    private Integer sourceMode;

    /**
     * 来源记录ID
     */
    private Long sourceRecordId;

    /**
     * 工时
     */
    private BigDecimal workHours;

    /**
     * 工数
     */
    private BigDecimal workUnits;

    /**
     * 每工薪资
     */
    private BigDecimal unitSalary;

    /**
     * 本条薪资金额
     */
    private BigDecimal salaryAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
