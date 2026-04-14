package com.xu.home.controller.calendar;

import com.xu.home.domain.calendar.CyclePlan;
import com.xu.home.param.calendar.po.CyclePlanPO;
import com.xu.home.param.calendar.vo.CyclePlanVO;
import com.xu.home.service.calendar.CyclePlanService;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 周期计划管理接口
 */
@RestController
@RequestMapping("/calendar/cycle")
public class CyclePlanController {

    private final CyclePlanService cyclePlanService;

    public CyclePlanController(CyclePlanService cyclePlanService) {
        this.cyclePlanService = cyclePlanService;
    }

    /**
     * 创建周期计划
     */
    @PostMapping("/create")
    public Response<?> createCyclePlan(@RequestBody CyclePlanPO po) {
        CyclePlan plan = new CyclePlan();
        BeanUtils.copyProperties(po, plan);
        plan.setAccount(SessionUtil.getCurrentAccount());
        plan.setStatus(1);  // 启用

        // 计算下次触发时间
        LocalDateTime nextTrigger = cyclePlanService.calculateNextTriggerTime(plan);
        plan.setNextTriggerTime(nextTrigger);

        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        plan.setIsDelete(0);
        boolean result = cyclePlanService.save(plan);
        return Response.checkResult(result);
    }

    /**
     * 更新周期计划
     */
    @PostMapping("/update")
    public Response<?> updateCyclePlan(@RequestBody CyclePlanPO po) {
        if (po.getId() == null) {
            return Response.error("周期计划ID不能为空");
        }
        CyclePlan plan = new CyclePlan();
        BeanUtils.copyProperties(po, plan);

        // 重新计算下次触发时间
        CyclePlan fullPlan = cyclePlanService.getById(po.getId());
        if (fullPlan != null) {
            BeanUtils.copyProperties(po, fullPlan);
            LocalDateTime nextTrigger = cyclePlanService.calculateNextTriggerTime(fullPlan);
            plan.setNextTriggerTime(nextTrigger);
        }

        plan.setUpdateTime(LocalDateTime.now());
        boolean result = cyclePlanService.updateById(plan);
        return Response.checkResult(result);
    }

    /**
     * 删除周期计划（软删除）
     */
    @PostMapping("/delete")
    public Response<?> deleteCyclePlan(@RequestBody IdPO po) {
        Long id = po.getId();
        CyclePlan plan = new CyclePlan();
        plan.setId(id);
        plan.setIsDelete(1);
        plan.setUpdateTime(LocalDateTime.now());
        boolean result = cyclePlanService.updateById(plan);
        return Response.checkResult(result);
    }

    /**
     * 获取周期计划列表
     */
    @GetMapping("/list")
    public Response<List<CyclePlanVO>> getCyclePlanList() {
        String account = SessionUtil.getCurrentAccount();
        List<CyclePlan> plans = cyclePlanService.getCyclePlansByAccount(account);
        List<CyclePlanVO> voList = plans.stream().map(this::toVO).collect(Collectors.toList());
        return Response.success(voList);
    }

    /**
     * 暂停周期计划
     */
    @PostMapping("/pause")
    public Response<?> pauseCyclePlan(@RequestBody IdPO po) {
        boolean result = cyclePlanService.pauseCyclePlan(po.getId());
        return Response.checkResult(result);
    }

    /**
     * 恢复周期计划
     */
    @PostMapping("/resume")
    public Response<?> resumeCyclePlan(@RequestBody IdPO po) {
        boolean result = cyclePlanService.resumeCyclePlan(po.getId());
        return Response.checkResult(result);
    }

    /**
     * 获取即将触发的计划
     */
    @GetMapping("/upcoming")
    public Response<List<CyclePlanVO>> getUpcomingPlans(@RequestParam(value = "hours", defaultValue = "24") Integer hours) {
        String account = SessionUtil.getCurrentAccount();
        LocalDateTime before = LocalDateTime.now().plusHours(hours);
        List<CyclePlan> plans = cyclePlanService.getUpcomingPlans(account, before);
        List<CyclePlanVO> voList = plans.stream().map(this::toVO).collect(Collectors.toList());
        return Response.success(voList);
    }

    private CyclePlanVO toVO(CyclePlan plan) {
        CyclePlanVO vo = new CyclePlanVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }
}
