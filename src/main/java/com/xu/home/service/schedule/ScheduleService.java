package com.xu.home.service.schedule;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.schedule.Schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务接口
 */
public interface ScheduleService extends IService<Schedule> {

    /**
     * 查询指定日期范围的排班
     */
    List<Schedule> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定员工的排班
     */
    List<Schedule> getSchedulesByAccount(String account, LocalDate startDate, LocalDate endDate);

    /**
     * 批量创建排班
     */
    boolean batchCreateSchedules(List<Schedule> schedules);
}
