package com.xu.home.param.timesheet;

import lombok.Data;

/**
 * 创建项目请求参数
 */
@Data
public class CreateProjectRequest {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 1-签到记工 2-人工记工
     */
    private Integer mode;

    /**
     * 项目备注
     */
    private String remark;
}
