package com.xu.home.param.timesheet;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算薪资重算请求参数
 */
@Data
public class SalaryCalcRequest {

    /**
     * 结算单ID
     */
    private Long settlementId;

    /**
     * 每工薪资
     */
    private BigDecimal unitSalary;
}
