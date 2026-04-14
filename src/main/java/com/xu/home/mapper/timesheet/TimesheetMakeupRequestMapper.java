package com.xu.home.mapper.timesheet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.timesheet.TimesheetMakeupRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 补签申请 Mapper
 */
@Mapper
public interface TimesheetMakeupRequestMapper extends BaseMapper<TimesheetMakeupRequest> {
}
