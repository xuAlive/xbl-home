package com.xu.home.param.timesheet.vo;

import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import lombok.Data;

import java.util.List;

/**
 * 项目详情返回对象
 */
@Data
public class ProjectDetailVO {

    /**
     * 项目基础信息
     */
    private TimesheetProject project;

    /**
     * 项目成员列表
     */
    private List<TimesheetProjectMember> members;
}
