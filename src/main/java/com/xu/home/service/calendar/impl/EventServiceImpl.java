package com.xu.home.service.calendar.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.Event;
import com.xu.home.mapper.calendar.EventMapper;
import com.xu.home.service.calendar.EventService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件服务实现
 */
@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {

    @Override
    public List<Event> getEventsByDateRange(String account, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Event::getAccount, account)
               .ge(Event::getEventDate, startDate)
               .le(Event::getEventDate, endDate)
               .eq(Event::getIsDelete, 0)
               .orderByAsc(Event::getEventDate)
               .orderByAsc(Event::getEventTime);
        return list(wrapper);
    }

    @Override
    public List<Event> getMonthEvents(String account, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        return getEventsByDateRange(account, startDate, endDate);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<Event> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Event::getId, id)
               .set(Event::getStatus, status)
               .set(Event::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public void checkAndUpdateExpiredEvents() {
        LocalDate today = LocalDate.now();
        LambdaUpdateWrapper<Event> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Event::getStatus, 1)  // 待完成
               .lt(Event::getEventDate, today)
               .eq(Event::getIsDelete, 0)
               .set(Event::getStatus, 3)  // 已过期
               .set(Event::getUpdateTime, LocalDateTime.now());
        update(wrapper);
    }
}
