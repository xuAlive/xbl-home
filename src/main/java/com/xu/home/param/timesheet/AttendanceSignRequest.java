package com.xu.home.param.timesheet;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到请求参数
 */
@Data
public class AttendanceSignRequest {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 1-签到 2-离班
     */
    private Integer signType;

    /**
     * 签到时间，未传时使用当前时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signTime;

    /**
     * 备注
     */
    private String remark;
}
