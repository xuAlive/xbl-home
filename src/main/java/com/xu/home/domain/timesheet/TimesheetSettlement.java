package com.xu.home.domain.timesheet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单表
 */
@Data
@TableName("timesheet_settlement")
public class TimesheetSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 结算单号
     */
    private String settlementNo;

    /**
     * 1-单人结算 2-批量结算 3-项目结束结算
     */
    private Integer settlementType;

    /**
     * 结算人账号
     */
    private String settledBy;

    /**
     * 结算人姓名
     */
    private String settledByName;

    /**
     * 明细条数
     */
    private Integer itemCount;

    /**
     * 总工时
     */
    private BigDecimal totalWorkHours;

    /**
     * 总工数
     */
    private BigDecimal totalWorkUnits;

    /**
     * 每工薪资
     */
    private BigDecimal unitSalary;

    /**
     * 总薪资
     */
    private BigDecimal totalSalary;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
