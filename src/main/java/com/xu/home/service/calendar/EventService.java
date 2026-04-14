package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.Event;

import java.time.LocalDate;
import java.util.List;

/**
 * 事件服务接口
 */
public interface EventService extends IService<Event> {

    /**
     * 查询指定日期范围的事件
     */
    List<Event> getEventsByDateRange(String account, LocalDate startDate, LocalDate endDate);

    /**
     * 获取月度事件汇总
     */
    List<Event> getMonthEvents(String account, int year, int month);

    /**
     * 更新事件状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查并更新过期事件
     */
    void checkAndUpdateExpiredEvents();
}
