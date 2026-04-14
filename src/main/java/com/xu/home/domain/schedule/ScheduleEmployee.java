package com.xu.home.domain.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工表
 */
@Data
@TableName("schedule_employee")
public class ScheduleEmployee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工编号
     */
    private String employeeCode;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 部门
     */
    private String department;

    /**
     * 职位
     */
    private String position;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 状态：1-在职 2-离职 3-休假
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 是否删除：0-否 1-是
     */
    private Integer isDelete;
}
