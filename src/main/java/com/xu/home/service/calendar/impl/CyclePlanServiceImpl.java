package com.xu.home.service.calendar.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.CyclePlan;
import com.xu.home.mapper.calendar.CyclePlanMapper;
import com.xu.home.service.calendar.CyclePlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 周期计划服务实现
 */
@Slf4j
@Service
public class CyclePlanServiceImpl extends ServiceImpl<CyclePlanMapper, CyclePlan> implements CyclePlanService {

    @Override
    public List<CyclePlan> getCyclePlansByAccount(String account) {
        LambdaQueryWrapper<CyclePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CyclePlan::getAccount, account)
               .eq(CyclePlan::getIsDelete, 0)
               .orderByDesc(CyclePlan::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean pauseCyclePlan(Long id) {
        LambdaUpdateWrapper<CyclePlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CyclePlan::getId, id)
               .set(CyclePlan::getStatus, 2)  // 暂停
               .set(CyclePlan::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean resumeCyclePlan(Long id) {
        CyclePlan plan = getById(id);
        if (plan == null) {
            return false;
        }

        LocalDateTime nextTrigger = calculateNextTriggerTime(plan);

        LambdaUpdateWrapper<CyclePlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CyclePlan::getId, id)
               .set(CyclePlan::getStatus, 1)  // 启用
               .set(CyclePlan::getNextTriggerTime, nextTrigger)
               .set(CyclePlan::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public List<CyclePlan> getUpcomingPlans(String account, LocalDateTime before) {
        LambdaQueryWrapper<CyclePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CyclePlan::getAccount, account)
               .eq(CyclePlan::getStatus, 1)  // 启用
               .le(CyclePlan::getNextTriggerTime, before)
               .eq(CyclePlan::getIsDelete, 0)
               .orderByAsc(CyclePlan::getNextTriggerTime);
        return list(wrapper);
    }

    @Override
    public List<CyclePlan> getPlansToTrigger(LocalDateTime now) {
        LambdaQueryWrapper<CyclePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CyclePlan::getStatus, 1)  // 启用
               .le(CyclePlan::getNextTriggerTime, now)
               .eq(CyclePlan::getIsDelete, 0);
        return list(wrapper);
    }

    @Override
    public boolean updateNextTriggerTime(Long id, LocalDateTime nextTriggerTime) {
        LambdaUpdateWrapper<CyclePlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CyclePlan::getId, id)
               .set(CyclePlan::getNextTriggerTime, nextTriggerTime)
               .set(CyclePlan::getLastTriggerTime, LocalDateTime.now())
               .set(CyclePlan::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public LocalDateTime calculateNextTriggerTime(CyclePlan cyclePlan) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime remindTime = cyclePlan.getRemindTime();
        if (remindTime == null) {
            remindTime = LocalTime.of(9, 0);  // 默认早上9点
        }

        // 检查是否已结束
        if (cyclePlan.getEndDate() != null && LocalDate.now().isAfter(cyclePlan.getEndDate())) {
            return null;
        }

        Integer cycleType = cyclePlan.getCycleType();
        String cycleConfig = cyclePlan.getCycleConfig();

        try {
            if (cycleType == 1) {
                // 每周
                return calculateWeeklyNextTrigger(cycleConfig, remindTime, now);
            } else if (cycleType == 2) {
                // 每月
                return calculateMonthlyNextTrigger(cycleConfig, remindTime, now);
            } else if (cycleType == 3) {
                // 自定义 (使用cron表达式)
                // 简化处理：返回明天同一时间
                return LocalDateTime.of(now.toLocalDate().plusDays(1), remindTime);
            }
        } catch (Exception e) {
            log.error("计算下次触发时间失败, planId={}", cyclePlan.getId(), e);
        }

        // 默认返回明天同一时间
        return LocalDateTime.of(now.toLocalDate().plusDays(1), remindTime);
    }

    private LocalDateTime calculateWeeklyNextTrigger(String cycleConfig, LocalTime remindTime, LocalDateTime now) {
        JSONObject config = JSON.parseObject(cycleConfig);
        List<Integer> days = config.getList("days", Integer.class);
        if (days == null || days.isEmpty()) {
            return LocalDateTime.of(now.toLocalDate().plusDays(7), remindTime);
        }

        LocalDate today = now.toLocalDate();
        int todayDayOfWeek = today.getDayOfWeek().getValue();

        // 找到今天之后最近的触发日
        for (int i = 0; i < 7; i++) {
            LocalDate checkDate = today.plusDays(i);
            int dayOfWeek = checkDate.getDayOfWeek().getValue();
            if (days.contains(dayOfWeek)) {
                LocalDateTime candidate = LocalDateTime.of(checkDate, remindTime);
                if (candidate.isAfter(now)) {
                    return candidate;
                }
            }
        }

        // 下周第一个符合条件的日期
        for (int dayOfWeek : days) {
            LocalDate nextDate = today.with(DayOfWeek.of(dayOfWeek)).plusWeeks(1);
            return LocalDateTime.of(nextDate, remindTime);
        }

        return LocalDateTime.of(today.plusDays(7), remindTime);
    }

    private LocalDateTime calculateMonthlyNextTrigger(String cycleConfig, LocalTime remindTime, LocalDateTime now) {
        JSONObject config = JSON.parseObject(cycleConfig);
        List<Integer> days = config.getList("days", Integer.class);
        if (days == null || days.isEmpty()) {
            return LocalDateTime.of(now.toLocalDate().plusMonths(1), remindTime);
        }

        LocalDate today = now.toLocalDate();
        int todayDay = today.getDayOfMonth();

        // 本月剩余的符合条件的日期
        for (int day : days) {
            if (day >= todayDay && day <= today.lengthOfMonth()) {
                LocalDate checkDate = today.withDayOfMonth(day);
                LocalDateTime candidate = LocalDateTime.of(checkDate, remindTime);
                if (candidate.isAfter(now)) {
                    return candidate;
                }
            }
        }

        // 下月第一个符合条件的日期
        LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);
        for (int day : days) {
            if (day <= nextMonth.lengthOfMonth()) {
                return LocalDateTime.of(nextMonth.withDayOfMonth(day), remindTime);
            }
        }

        return LocalDateTime.of(nextMonth, remindTime);
    }
}
