package com.xu.home.param.timesheet;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 人工记工保存请求参数
 */
@Data
public class ManualWorklogSaveRequest {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 记工日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /**
     * 当天所有成员的记工明细
     */
    private List<ManualWorklogItemRequest> items;
}
