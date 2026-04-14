package com.xu.home.param.timesheet;

import lombok.Data;

/**
 * 同步历史项目成员请求
 */
@Data
public class SyncProjectMembersRequest {

    /**
     * 当前目标项目ID
     */
    private Long targetProjectId;

    /**
     * 成员来源项目ID
     */
    private Long sourceProjectId;
}
