package com.xu.home.mapper.calendar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.calendar.Reminder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提醒 Mapper
 */
@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {
}
