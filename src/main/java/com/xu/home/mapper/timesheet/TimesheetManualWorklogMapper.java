package com.xu.home.mapper.timesheet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.timesheet.TimesheetManualWorklog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人工记工 Mapper
 */
@Mapper
public interface TimesheetManualWorklogMapper extends BaseMapper<TimesheetManualWorklog> {
}
