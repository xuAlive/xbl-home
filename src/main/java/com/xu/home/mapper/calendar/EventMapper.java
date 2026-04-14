package com.xu.home.mapper.calendar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.calendar.Event;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件 Mapper
 */
@Mapper
public interface EventMapper extends BaseMapper<Event> {
}
