package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员表
 */
@Data
@TableName("timesheet_project_member")
public class TimesheetProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    /**
     * 加入项目时间
     */
    private LocalDateTime joinTime;

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
