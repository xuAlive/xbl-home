package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工时项目表
 */
@Data
@TableName("timesheet_project")
public class TimesheetProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 1-签到记工 2-项目创建人维护记工
     */
    private Integer mode;

    /**
     * 1-编辑中 2-已结束
     */
    private Integer status;

    /**
     * 创建者账号
     */
    private String creatorAccount;

    /**
     * 创建者姓名
     */
    private String creatorName;

    /**
     * 项目备注
     */
    private String remark;

    /**
     * 项目结束时间
     */
    private LocalDateTime finishedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-否 1-是
     */
    private Integer isDelete;
}
