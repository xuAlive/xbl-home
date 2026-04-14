package com.xu.home.param.schedule.vo;

import lombok.Data;

import java.util.List;

/**
 * 排班统计 VO
 */
@Data
public class ScheduleStatisticsVO {

    /**
     * 统计周期类型：week, month, quarter, year
     */
    private String periodType;

    /**
     * 统计周期描述，如 "2026年第1周", "2026年1月"
     */
    private String periodDesc;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 总排班次数
     */
    private Integer totalCount;

    /**
     * 总工作时长（小时）
     */
    private Double totalHours;

    /**
     * 按班次统计（用于饼图）
     * key: 班次名称, value: 次数
     */
    private List<ChartItem> shiftStats;

    /**
     * 按状态统计（用于饼图）
     * key: 状态名称, value: 次数
     */
    private List<ChartItem> statusStats;

    /**
     * 按员工统计（用于柱状图）
     */
    private List<EmployeeStatItem> employeeStats;

    /**
     * 按日期统计（用于柱状图）
     */
    private List<DateStatItem> dateStats;

    /**
     * 图表数据项
     */
    @Data
    public static class ChartItem {
        private String name;
        private Integer value;

        public ChartItem() {}

        public ChartItem(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * 员工统计项
     */
    @Data
    public static class EmployeeStatItem {
        private String account;
        private String employeeName;
        private Integer scheduleCount;
        private Double workHours;
        private Integer normalCount;      // 正常
        private Integer leaveCount;       // 请假
        private Integer adjustCount;      // 调休
        private Integer overtimeCount;    // 加班

        public EmployeeStatItem() {}
    }

    /**
     * 日期统计项（用于趋势图）
     */
    @Data
    public static class DateStatItem {
        private String date;
        private Integer count;

        public DateStatItem() {}

        public DateStatItem(String date, Integer count) {
            this.date = date;
            this.count = count;
        }
    }
}
