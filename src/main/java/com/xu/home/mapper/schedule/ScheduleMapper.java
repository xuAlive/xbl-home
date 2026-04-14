package com.xu.home.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.schedule.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排班记录 Mapper
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}
