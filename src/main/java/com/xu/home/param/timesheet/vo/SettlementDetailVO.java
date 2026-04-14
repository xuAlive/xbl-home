package com.xu.home.param.timesheet.vo;

import com.xu.home.domain.timesheet.TimesheetSettlement;
import com.xu.home.domain.timesheet.TimesheetSettlementItem;
import lombok.Data;

import java.util.List;

/**
 * 结算详情返回对象
 */
@Data
public class SettlementDetailVO {

    /**
     * 结算单基础信息
     */
    private TimesheetSettlement settlement;

    /**
     * 按成员汇总后的统计结果
     */
    private List<SettlementMemberSummaryVO> memberSummaries;

    /**
     * 结算明细列表
     */
    private List<TimesheetSettlementItem> items;
}
