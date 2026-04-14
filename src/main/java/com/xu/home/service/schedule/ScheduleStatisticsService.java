package com.xu.home.service.schedule;

import com.xu.home.param.schedule.vo.ScheduleStatisticsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班统计服务接口
 */
public interface ScheduleStatisticsService {

    /**
     * 获取周统计
     * @param account 员工账号（可选，为空则统计所有员工）
     * @param date 周内任意一天（用于确定是哪一周）
     */
    ScheduleStatisticsVO getWeeklyStatistics(String account, LocalDate date);

    /**
     * 获取月统计
     * @param account 员工账号（可选）
     * @param year 年份
     * @param month 月份
     */
    ScheduleStatisticsVO getMonthlyStatistics(String account, int year, int month);

    /**
     * 获取季度统计
     * @param account 员工账号（可选）
     * @param year 年份
     * @param quarter 季度 (1-4)
     */
    ScheduleStatisticsVO getQuarterlyStatistics(String account, int year, int quarter);

    /**
     * 获取年度统计
     * @param account 员工账号（可选）
     * @param year 年份
     */
    ScheduleStatisticsVO getYearlyStatistics(String account, int year);

    /**
     * 获取自定义日期范围统计
     * @param account 员工账号（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    ScheduleStatisticsVO getCustomStatistics(String account, LocalDate startDate, LocalDate endDate);

    /**
     * 导出统计报表（返回CSV格式数据）
     */
    String exportStatistics(String account, LocalDate startDate, LocalDate endDate);

    /**
     * 导出月排班日历 Excel
     */
    byte[] exportMonthlyCalendar(List<String> accounts, int year, int month);
}
