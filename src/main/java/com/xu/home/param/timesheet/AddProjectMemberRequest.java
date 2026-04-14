package com.xu.home.param.timesheet;

import lombok.Data;

/**
 * 新增项目成员请求参数
 */
@Data
public class AddProjectMemberRequest {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 成员账号，人工记工模式下可为空
     */
    private String memberAccount;

    /**
     * 成员姓名
     */
    private String memberName;

    /**
     * 成员手机号
     */
    private String memberPhone;
}
