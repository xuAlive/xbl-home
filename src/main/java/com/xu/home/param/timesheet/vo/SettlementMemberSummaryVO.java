package com.xu.home.param.timesheet.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成员维度结算汇总
 */
@Data
public class SettlementMemberSummaryVO {

    /**
     * 成员ID
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
     * 汇总工时
     */
    private BigDecimal totalWorkHours;

    /**
     * 汇总工数
     */
    private BigDecimal totalWorkUnits;

    /**
     * 每工薪资
     */
    private BigDecimal unitSalary;

    /**
     * 汇总薪资
     */
    private BigDecimal totalSalary;
}
