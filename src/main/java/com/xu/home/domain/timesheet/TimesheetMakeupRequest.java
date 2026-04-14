package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补签申请表
 */
@Data
@TableName("timesheet_makeup_request")
public class TimesheetMakeupRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目成员ID
     */
    private Long memberId;

    /**
     * 申请人账号
     */
    private String memberAccount;

    /**
     * 申请人姓名
     */
    private String memberName;

    /**
     * 补签对应日期
     */
    private LocalDate workDate;

    /**
     * 补签上班时间
     */
    private LocalDateTime makeupSignInTime;

    /**
     * 补签离班时间
     */
    private LocalDateTime makeupSignOutTime;

    /**
     * 补签原因
     */
    private String reason;

    /**
     * 1-待审批 2-已通过 3-已拒绝
     */
    private Integer approvalStatus;

    /**
     * 审批人账号
     */
    private String approverAccount;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批备注
     */
    private String approvalRemark;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

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
