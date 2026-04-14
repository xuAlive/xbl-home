package com.xu.home.service.schedule.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.schedule.Schedule;
import com.xu.home.domain.schedule.ScheduleEmployee;
import com.xu.home.domain.schedule.Shift;
import com.xu.home.mapper.schedule.ScheduleEmployeeMapper;
import com.xu.home.mapper.schedule.ScheduleMapper;
import com.xu.home.mapper.schedule.ShiftMapper;
import com.xu.home.param.schedule.vo.ScheduleStatisticsVO;
import com.xu.home.param.schedule.vo.ScheduleStatisticsVO.ChartItem;
import com.xu.home.param.schedule.vo.ScheduleStatisticsVO.DateStatItem;
import com.xu.home.param.schedule.vo.ScheduleStatisticsVO.EmployeeStatItem;
import com.xu.home.service.schedule.ScheduleStatisticsService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排班统计服务实现
 */
@Service
public class ScheduleStatisticsServiceImpl implements ScheduleStatisticsService {

    private final ScheduleMapper scheduleMapper;
    private final ShiftMapper shiftMapper;
    private final ScheduleEmployeeMapper scheduleEmployeeMapper;

    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put(1, "正常");
        STATUS_MAP.put(2, "请假");
        STATUS_MAP.put(3, "调休");
        STATUS_MAP.put(4, "加班");
    }

    public ScheduleStatisticsServiceImpl(ScheduleMapper scheduleMapper, ShiftMapper shiftMapper,
                                         ScheduleEmployeeMapper scheduleEmployeeMapper) {
        this.scheduleMapper = scheduleMapper;
        this.shiftMapper = shiftMapper;
        this.scheduleEmployeeMapper = scheduleEmployeeMapper;
    }

    @Override
    public ScheduleStatisticsVO getWeeklyStatistics(String account, LocalDate date) {
        // 获取本周的开始和结束日期
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        ScheduleStatisticsVO vo = buildStatistics(account, startOfWeek, endOfWeek);
        vo.setPeriodType("week");
        vo.setPeriodDesc(String.format("%d年第%d周", date.getYear(), getWeekOfYear(date)));
        return vo;
    }

    @Override
    public ScheduleStatisticsVO getMonthlyStatistics(String account, int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        ScheduleStatisticsVO vo = buildStatistics(account, startOfMonth, endOfMonth);
        vo.setPeriodType("month");
        vo.setPeriodDesc(String.format("%d年%d月", year, month));
        return vo;
    }

    @Override
    public ScheduleStatisticsVO getQuarterlyStatistics(String account, int year, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = quarter * 3;

        LocalDate startOfQuarter = LocalDate.of(year, startMonth, 1);
        LocalDate endOfQuarter = LocalDate.of(year, endMonth, 1).with(TemporalAdjusters.lastDayOfMonth());

        ScheduleStatisticsVO vo = buildStatistics(account, startOfQuarter, endOfQuarter);
        vo.setPeriodType("quarter");
        vo.setPeriodDesc(String.format("%d年第%d季度", year, quarter));
        return vo;
    }

    @Override
    public ScheduleStatisticsVO getYearlyStatistics(String account, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        ScheduleStatisticsVO vo = buildStatistics(account, startOfYear, endOfYear);
        vo.setPeriodType("year");
        vo.setPeriodDesc(String.format("%d年", year));
        return vo;
    }

    @Override
    public ScheduleStatisticsVO getCustomStatistics(String account, LocalDate startDate, LocalDate endDate) {
        ScheduleStatisticsVO vo = buildStatistics(account, startDate, endDate);
        vo.setPeriodType("custom");
        vo.setPeriodDesc(String.format("%s 至 %s", startDate, endDate));
        return vo;
    }

    @Override
    public String exportStatistics(String account, LocalDate startDate, LocalDate endDate) {
        List<Schedule> schedules = querySchedules(account, startDate, endDate);
        Map<Long, Shift> shiftMap = getShiftMap();

        StringBuilder sb = new StringBuilder();
        // CSV 头部
        sb.append("日期,员工账号,员工姓名,班次名称,状态,工作时长(小时),备注\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Schedule schedule : schedules) {
            sb.append(schedule.getScheduleDate().format(dateFormatter)).append(",");
            sb.append(schedule.getAccount()).append(",");
            sb.append(schedule.getEmployeeName() != null ? schedule.getEmployeeName() : "").append(",");
            sb.append(schedule.getShiftName() != null ? schedule.getShiftName() : "").append(",");
            sb.append(STATUS_MAP.getOrDefault(schedule.getStatus(), "未知")).append(",");
            sb.append(calculateWorkHours(schedule.getShiftId(), shiftMap)).append(",");
            sb.append(schedule.getRemark() != null ? schedule.getRemark().replace(",", "，") : "").append("\n");
        }

        return sb.toString();
    }

    @Override
    public byte[] exportMonthlyCalendar(List<String> accounts, int year, int month) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Map<Long, Shift> shiftMap = getShiftMap();
            List<ScheduleEmployee> employees = resolveExportEmployees(accounts);
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
            LocalDate start = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate end = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            if (employees.isEmpty()) {
                employees = new ArrayList<>();
                ScheduleEmployee employee = new ScheduleEmployee();
                employee.setEmployeeCode(accounts.isEmpty() ? "unknown" : accounts.get(0));
                employee.setEmployeeName(accounts.isEmpty() ? "未命名员工" : accounts.get(0));
                employees.add(employee);
            }

            List<ScheduleEmployee> exportEmployees = employees;
            Map<String, List<Schedule>> scheduleMap = querySchedulesByAccounts(accounts, start, end).stream()
                    .filter(s -> exportEmployees.stream().anyMatch(e -> e.getEmployeeCode().equals(s.getAccount())))
                    .collect(Collectors.groupingBy(Schedule::getAccount));

            for (ScheduleEmployee employee : exportEmployees) {
                createEmployeeCalendarSheet(workbook, employee, year, month, start, end,
                        scheduleMap.getOrDefault(employee.getEmployeeCode(), List.of()), shiftMap);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出月排班失败", e);
        }
    }

    /**
     * 构建统计数据
     */
    private ScheduleStatisticsVO buildStatistics(String account, LocalDate startDate, LocalDate endDate) {
        List<Schedule> schedules = querySchedules(account, startDate, endDate);
        Map<Long, Shift> shiftMap = getShiftMap();

        ScheduleStatisticsVO vo = new ScheduleStatisticsVO();
        vo.setStartDate(startDate.toString());
        vo.setEndDate(endDate.toString());
        vo.setTotalCount(schedules.size());

        // 计算总工作时长
        double totalHours = schedules.stream()
                .mapToDouble(s -> calculateWorkHours(s.getShiftId(), shiftMap))
                .sum();
        vo.setTotalHours(Math.round(totalHours * 100.0) / 100.0);

        // 按班次统计
        Map<String, Integer> shiftCountMap = schedules.stream()
                .filter(s -> s.getShiftName() != null)
                .collect(Collectors.groupingBy(
                        Schedule::getShiftName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        vo.setShiftStats(shiftCountMap.entrySet().stream()
                .map(e -> new ChartItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        // 按状态统计
        Map<String, Integer> statusCountMap = schedules.stream()
                .collect(Collectors.groupingBy(
                        s -> STATUS_MAP.getOrDefault(s.getStatus(), "未知"),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        vo.setStatusStats(statusCountMap.entrySet().stream()
                .map(e -> new ChartItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        // 按员工统计
        Map<String, List<Schedule>> employeeScheduleMap = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getAccount));

        List<EmployeeStatItem> employeeStats = new ArrayList<>();
        for (Map.Entry<String, List<Schedule>> entry : employeeScheduleMap.entrySet()) {
            EmployeeStatItem item = new EmployeeStatItem();
            item.setAccount(entry.getKey());
            List<Schedule> empSchedules = entry.getValue();

            if (!empSchedules.isEmpty()) {
                item.setEmployeeName(empSchedules.get(0).getEmployeeName());
            }
            item.setScheduleCount(empSchedules.size());

            double empHours = empSchedules.stream()
                    .mapToDouble(s -> calculateWorkHours(s.getShiftId(), shiftMap))
                    .sum();
            item.setWorkHours(Math.round(empHours * 100.0) / 100.0);

            // 按状态细分
            Map<Integer, Long> statusCount = empSchedules.stream()
                    .collect(Collectors.groupingBy(Schedule::getStatus, Collectors.counting()));
            item.setNormalCount(statusCount.getOrDefault(1, 0L).intValue());
            item.setLeaveCount(statusCount.getOrDefault(2, 0L).intValue());
            item.setAdjustCount(statusCount.getOrDefault(3, 0L).intValue());
            item.setOvertimeCount(statusCount.getOrDefault(4, 0L).intValue());

            employeeStats.add(item);
        }
        vo.setEmployeeStats(employeeStats);

        // 按日期统计（用于趋势图）
        Map<LocalDate, Long> dateCountMap = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getScheduleDate, Collectors.counting()));

        List<DateStatItem> dateStats = dateCountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DateStatItem(e.getKey().toString(), e.getValue().intValue()))
                .collect(Collectors.toList());
        vo.setDateStats(dateStats);

        return vo;
    }

    /**
     * 查询排班数据
     */
    private List<Schedule> querySchedules(String account, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getIsDelete, 0)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate);

        if (StringUtils.hasText(account)) {
            wrapper.eq(Schedule::getAccount, account);
        }

        wrapper.orderByAsc(Schedule::getScheduleDate);
        return scheduleMapper.selectList(wrapper);
    }

    private List<Schedule> querySchedulesByAccounts(List<String> accounts, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getIsDelete, 0)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate);

        if (accounts != null && !accounts.isEmpty()) {
            wrapper.in(Schedule::getAccount, accounts);
        }

        wrapper.orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getAccount);
        return scheduleMapper.selectList(wrapper);
    }

    /**
     * 获取班次映射
     */
    private Map<Long, Shift> getShiftMap() {
        List<Shift> shifts = shiftMapper.selectList(
                new LambdaQueryWrapper<Shift>().eq(Shift::getIsDelete, 0)
        );
        return shifts.stream().collect(Collectors.toMap(Shift::getId, s -> s));
    }

    /**
     * 计算工作时长
     */
    private double calculateWorkHours(Long shiftId, Map<Long, Shift> shiftMap) {
        if (shiftId == null || !shiftMap.containsKey(shiftId)) {
            return 0;
        }
        Shift shift = shiftMap.get(shiftId);
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            return 0;
        }

        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();

        // 处理跨天情况（如晚班 22:00 - 08:00）
        long minutes;
        if (end.isBefore(start)) {
            // 跨天
            minutes = (24 * 60 - start.toSecondOfDay() / 60) + end.toSecondOfDay() / 60;
        } else {
            minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60;
        }

        return minutes / 60.0;
    }

    /**
     * 获取周数
     */
    private int getWeekOfYear(LocalDate date) {
        return date.get(java.time.temporal.WeekFields.of(Locale.CHINA).weekOfYear());
    }

    private List<ScheduleEmployee> resolveExportEmployees(List<String> accounts) {
        LambdaQueryWrapper<ScheduleEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEmployee::getIsDelete, 0)
                .eq(ScheduleEmployee::getStatus, 1)
                .orderByAsc(ScheduleEmployee::getEmployeeCode);
        if (accounts != null && !accounts.isEmpty()) {
            wrapper.in(ScheduleEmployee::getEmployeeCode, accounts);
        }
        return scheduleEmployeeMapper.selectList(wrapper);
    }

    private void createEmployeeCalendarSheet(Workbook workbook, ScheduleEmployee employee, int year, int month,
                                             LocalDate start, LocalDate end, List<Schedule> schedules,
                                             Map<Long, Shift> shiftMap) {
        String sheetName = employee.getEmployeeName() != null && !employee.getEmployeeName().isBlank()
                ? employee.getEmployeeName()
                : employee.getEmployeeCode();
        Sheet sheet = workbook.createSheet(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName);

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dayStyle = createDayStyle(workbook);
        CellStyle detailStyle = createDetailStyle(workbook);
        CellStyle blankStyle = createBlankStyle(workbook);
        CellStyle mutedDayStyle = createMutedDayStyle(workbook);

        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(26);
        createCell(titleRow, 0, String.format("%d年%02d月排班确认表 - %s", year, month,
                employee.getEmployeeName() != null ? employee.getEmployeeName() : employee.getEmployeeCode()), titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row headerRow = sheet.createRow(1);
        String[] weekNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < weekNames.length; i++) {
            createCell(headerRow, i, weekNames[i], headerStyle);
        }

        Map<LocalDate, Schedule> scheduleByDate = schedules.stream()
                .collect(Collectors.toMap(Schedule::getScheduleDate, s -> s, (a, b) -> a));

        int rowIndex = 2;
        for (LocalDate weekStart = start; !weekStart.isAfter(end); weekStart = weekStart.plusWeeks(1)) {
            Row dayRow = sheet.createRow(rowIndex++);
            dayRow.setHeightInPoints(22);
            Row detailRow = sheet.createRow(rowIndex++);
            detailRow.setHeightInPoints(46);
            Row blankRow = sheet.createRow(rowIndex++);
            blankRow.setHeightInPoints(24);

            for (int i = 0; i < 7; i++) {
                LocalDate currentDate = weekStart.plusDays(i);
                boolean inMonth = currentDate.getMonthValue() == month;
                CellStyle currentDayStyle = inMonth ? dayStyle : mutedDayStyle;
                CellStyle currentDetailStyle = inMonth ? detailStyle : mutedDayStyle;

                createCell(dayRow, i, String.format("%02d", currentDate.getDayOfMonth()), currentDayStyle);

                Schedule schedule = scheduleByDate.get(currentDate);
                String detail = buildScheduleDetail(schedule, shiftMap);
                createCell(detailRow, i, detail, currentDetailStyle);
                createCell(blankRow, i, "", blankStyle);
            }
        }
    }

    private String buildScheduleDetail(Schedule schedule, Map<Long, Shift> shiftMap) {
        if (schedule == null) {
            return "";
        }

        StringBuilder detail = new StringBuilder();
        if (schedule.getShiftName() != null) {
            detail.append(schedule.getShiftName());
        }
        Shift shift = schedule.getShiftId() != null ? shiftMap.get(schedule.getShiftId()) : null;
        if (shift != null && shift.getStartTime() != null && shift.getEndTime() != null) {
            detail.append("\n")
                    .append(shift.getStartTime())
                    .append(" - ")
                    .append(shift.getEndTime());
        }
        if (schedule.getStatus() != null && schedule.getStatus() != 1) {
            detail.append("\n").append(STATUS_MAP.getOrDefault(schedule.getStatus(), "未知"));
        }
        if (schedule.getRemark() != null && !schedule.getRemark().isBlank()) {
            detail.append("\n").append(schedule.getRemark());
        }
        return detail.toString();
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        return style;
    }

    private CellStyle createDayStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private CellStyle createDetailStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        applyBorder(style);
        return style;
    }

    private CellStyle createBlankStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyBorder(style);
        return style;
    }

    private CellStyle createMutedDayStyle(Workbook workbook) {
        CellStyle style = createDayStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
