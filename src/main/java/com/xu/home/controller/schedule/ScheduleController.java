package com.xu.home.controller.schedule;

import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.domain.schedule.Schedule;
import com.xu.home.service.schedule.ScheduleAccessService;
import com.xu.home.service.schedule.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班管理接口
 */
@RestController
@RequestMapping("/schedule/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleAccessService scheduleAccessService;

    public ScheduleController(ScheduleService scheduleService, ScheduleAccessService scheduleAccessService) {
        this.scheduleService = scheduleService;
        this.scheduleAccessService = scheduleAccessService;
    }

    /**
     * 查询日期范围内的排班
     */
    @GetMapping("/list")
    public Response<List<Schedule>> getScheduleList(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        String currentAccount = SessionUtil.getCurrentAccount();
        List<Schedule> schedules = scheduleAccessService.isAdmin(currentAccount)
                ? scheduleService.getSchedulesByDateRange(startDate, endDate)
                : scheduleService.getSchedulesByAccount(currentAccount, startDate, endDate);
        return Response.success(schedules);
    }

    /**
     * 查询当前用户的排班
     */
    @GetMapping("/my")
    public Response<List<Schedule>> getMySchedules(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        String account = SessionUtil.getCurrentAccount();
        List<Schedule> schedules = scheduleService.getSchedulesByAccount(account, startDate, endDate);
        return Response.success(schedules);
    }

    /**
     * 创建排班
     */
    @PostMapping("/create")
    public Response<?> createSchedule(@RequestBody Schedule schedule) {
        String currentAccount = SessionUtil.getCurrentAccount();
        scheduleAccessService.requireAdmin(currentAccount);
        schedule.setCreateTime(LocalDateTime.now());
        schedule.setUpdateTime(LocalDateTime.now());
        schedule.setCreateBy(currentAccount);
        schedule.setIsDelete(0);
        boolean result = scheduleService.save(schedule);
        return Response.checkResult(result);
    }

    /**
     * 批量创建排班
     */
    @PostMapping("/batchCreate")
    public Response<?> batchCreateSchedules(@RequestBody List<Schedule> schedules) {
        String currentAccount = SessionUtil.getCurrentAccount();
        scheduleAccessService.requireAdmin(currentAccount);
        LocalDateTime now = LocalDateTime.now();
        schedules.forEach(s -> {
            s.setCreateTime(now);
            s.setUpdateTime(now);
            s.setCreateBy(currentAccount);
            s.setIsDelete(0);
        });
        boolean result = scheduleService.batchCreateSchedules(schedules);
        return Response.checkResult(result);
    }

    /**
     * 更新排班
     */
    @PostMapping("/update")
    public Response<?> updateSchedule(@RequestBody Schedule schedule) {
        String currentAccount = SessionUtil.getCurrentAccount();
        scheduleAccessService.requireAdmin(currentAccount);
        Schedule existing = scheduleService.getById(schedule.getId());
        if (existing == null || existing.getIsDelete() != null && existing.getIsDelete() == 1) {
            return Response.error("排班不存在");
        }
        if (existing.getCreateBy() != null && !existing.getCreateBy().equals(currentAccount)) {
            return Response.error("只能修改自己创建的排班");
        }
        schedule.setUpdateTime(LocalDateTime.now());
        boolean result = scheduleService.updateById(schedule);
        return Response.checkResult(result);
    }

    /**
     * 删除排班（软删除）
     */
    @PostMapping("/delete")
    public Response<?> deleteSchedule(@RequestBody IdPO po) {
        String currentAccount = SessionUtil.getCurrentAccount();
        scheduleAccessService.requireAdmin(currentAccount);
        Long id = po.getId();
        Schedule existing = scheduleService.getById(id);
        if (existing == null || existing.getIsDelete() != null && existing.getIsDelete() == 1) {
            return Response.error("排班不存在");
        }
        if (existing.getCreateBy() != null && !existing.getCreateBy().equals(currentAccount)) {
            return Response.error("只能删除自己创建的排班");
        }
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setIsDelete(1);
        schedule.setUpdateTime(LocalDateTime.now());
        boolean result = scheduleService.updateById(schedule);
        return Response.checkResult(result);
    }
}
