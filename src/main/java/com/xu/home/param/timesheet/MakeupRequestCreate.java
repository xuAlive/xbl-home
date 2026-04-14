package com.xu.home.param.timesheet;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建补签申请请求参数
 */
@Data
public class MakeupRequestCreate {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 补签日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /**
     * 补签上班时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signInTime;

    /**
     * 补签离班时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signOutTime;

    /**
     * 补签原因
     */
    private String reason;
}
