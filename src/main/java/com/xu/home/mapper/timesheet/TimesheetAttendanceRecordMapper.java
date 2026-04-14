package com.xu.home.mapper.timesheet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.timesheet.TimesheetAttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录 Mapper
 */
@Mapper
public interface TimesheetAttendanceRecordMapper extends BaseMapper<TimesheetAttendanceRecord> {
}
