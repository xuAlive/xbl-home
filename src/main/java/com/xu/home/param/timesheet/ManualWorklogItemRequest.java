package com.xu.home.param.timesheet;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单个成员人工记工明细
 */
@Data
public class ManualWorklogItemRequest {

    /**
     * 项目成员ID
     */
    private Long memberId;

    /**
     * 工数，仅支持 0、0.5、1
     */
    private BigDecimal workUnits;

    /**
     * 备注
     */
    private String remark;
}
