package com.xu.home.service.schedule.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.schedule.Schedule;
import com.xu.home.mapper.schedule.ScheduleMapper;
import com.xu.home.service.schedule.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务实现
 */
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Override
    public List<Schedule> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Schedule::getScheduleDate, startDate)
               .le(Schedule::getScheduleDate, endDate)
               .eq(Schedule::getIsDelete, 0)
               .orderByAsc(Schedule::getScheduleDate)
               .orderByAsc(Schedule::getAccount);
        return list(wrapper);
    }

    @Override
    public List<Schedule> getSchedulesByAccount(String account, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getAccount, account)
               .ge(Schedule::getScheduleDate, startDate)
               .le(Schedule::getScheduleDate, endDate)
               .eq(Schedule::getIsDelete, 0)
               .orderByAsc(Schedule::getScheduleDate);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreateSchedules(List<Schedule> schedules) {
        return saveBatch(schedules);
    }
}
