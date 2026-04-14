package com.xu.home.service.schedule.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.schedule.Shift;
import com.xu.home.mapper.schedule.ShiftMapper;
import com.xu.home.service.schedule.ShiftService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 班次服务实现
 */
@Service
public class ShiftServiceImpl extends ServiceImpl<ShiftMapper, Shift> implements ShiftService {

    @Override
    public List<Shift> getActiveShifts() {
        LambdaQueryWrapper<Shift> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shift::getStatus, 1)
               .eq(Shift::getIsDelete, 0)
               .orderByAsc(Shift::getStartTime);
        return list(wrapper);
    }
}
