package com.xu.home.param.timesheet;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算请求参数
 */
@Data
public class SettlementRequest {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 指定结算成员ID列表，空则结算全部成员
     */
    private List<Long> memberIds;

    /**
     * 每工薪资，可为空
     */
    private BigDecimal unitSalary;
}
