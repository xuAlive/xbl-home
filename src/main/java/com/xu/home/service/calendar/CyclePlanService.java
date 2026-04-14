package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.CyclePlan;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 周期计划服务接口
 */
public interface CyclePlanService extends IService<CyclePlan> {

    /**
     * 查询用户的周期计划列表
     */
    List<CyclePlan> getCyclePlansByAccount(String account);

    /**
     * 暂停周期计划
     */
    boolean pauseCyclePlan(Long id);

    /**
     * 恢复周期计划
     */
    boolean resumeCyclePlan(Long id);

    /**
     * 获取即将触发的计划
     */
    List<CyclePlan> getUpcomingPlans(String account, LocalDateTime before);

    /**
     * 获取需要触发的计划
     */
    List<CyclePlan> getPlansToTrigger(LocalDateTime now);

    /**
     * 更新下次触发时间
     */
    boolean updateNextTriggerTime(Long id, LocalDateTime nextTriggerTime);

    /**
     * 计算下次触发时间
     */
    LocalDateTime calculateNextTriggerTime(CyclePlan cyclePlan);
}
