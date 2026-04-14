package com.xu.home.service.calendar.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.Reminder;
import com.xu.home.mapper.calendar.ReminderMapper;
import com.xu.home.service.calendar.ReminderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒服务实现
 */
@Service
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder> implements ReminderService {

    @Override
    public List<Reminder> getRemindersByAccount(String account) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getAccount, account)
               .eq(Reminder::getIsDelete, 0)
               .orderByAsc(Reminder::getRemindTime);
        return list(wrapper);
    }

    @Override
    public List<Reminder> getPendingReminders(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getStatus, 1)  // 待发送
               .ge(Reminder::getRemindTime, startTime)
               .le(Reminder::getRemindTime, endTime)
               .eq(Reminder::getIsDelete, 0)
               .orderByAsc(Reminder::getRemindTime);
        return list(wrapper);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<Reminder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Reminder::getId, id)
               .set(Reminder::getStatus, status)
               .set(Reminder::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean cancelReminder(Long id) {
        LambdaUpdateWrapper<Reminder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Reminder::getId, id)
               .set(Reminder::getIsDelete, 1)
               .set(Reminder::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean retryReminder(Long id) {
        Reminder reminder = getById(id);
        if (reminder == null || reminder.getStatus() != 3) {
            return false;
        }

        LambdaUpdateWrapper<Reminder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Reminder::getId, id)
               .set(Reminder::getStatus, 1)  // 重置为待发送
               .set(Reminder::getRetryCount, reminder.getRetryCount() + 1)
               .set(Reminder::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }
}
