package com.xu.home.param.timesheet;

import lombok.Data;

/**
 * 补签审批请求参数
 */
@Data
public class ApproveMakeupRequest {

    /**
     * 补签申请ID
     */
    private Long requestId;

    /**
     * 2-通过 3-拒绝
     */
    private Integer approvalStatus;

    /**
     * 审批备注
     */
    private String approvalRemark;
}
