package com.xu.home.controller.schedule;

import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.schedule.vo.ScheduleStatisticsVO;
import com.xu.home.service.schedule.ScheduleAccessService;
import com.xu.home.service.schedule.ScheduleStatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 排班统计接口
 */
@RestController
@RequestMapping("/schedule/statistics")
public class ScheduleStatisticsController {

    private final ScheduleStatisticsService statisticsService;
    private final ScheduleAccessService scheduleAccessService;

    public ScheduleStatisticsController(ScheduleStatisticsService statisticsService, ScheduleAccessService scheduleAccessService) {
        this.statisticsService = statisticsService;
        this.scheduleAccessService = scheduleAccessService;
    }

    /**
     * 获取周统计
     * @param account 员工账号（可选）
     * @param date 周内任意一天，格式：yyyy-MM-dd（可选，默认当前日期）
     */
    @GetMapping("/weekly")
    public Response<ScheduleStatisticsVO> getWeeklyStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "date", required = false) String date) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        ScheduleStatisticsVO vo = statisticsService.getWeeklyStatistics(account, targetDate);
        return Response.success(vo);
    }

    /**
     * 获取月统计
     * @param account 员工账号（可选）
     * @param year 年份（可选，默认当前年）
     * @param month 月份（可选，默认当前月）
     */
    @GetMapping("/monthly")
    public Response<ScheduleStatisticsVO> getMonthlyStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        ScheduleStatisticsVO vo = statisticsService.getMonthlyStatistics(account, targetYear, targetMonth);
        return Response.success(vo);
    }

    /**
     * 获取季度统计
     * @param account 员工账号（可选）
     * @param year 年份（可选，默认当前年）
     * @param quarter 季度 1-4（可选，默认当前季度）
     */
    @GetMapping("/quarterly")
    public Response<ScheduleStatisticsVO> getQuarterlyStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "quarter", required = false) Integer quarter) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetQuarter = quarter != null ? quarter : (now.getMonthValue() - 1) / 3 + 1;
        ScheduleStatisticsVO vo = statisticsService.getQuarterlyStatistics(account, targetYear, targetQuarter);
        return Response.success(vo);
    }

    /**
     * 获取年度统计
     * @param account 员工账号（可选）
     * @param year 年份（可选，默认当前年）
     */
    @GetMapping("/yearly")
    public Response<ScheduleStatisticsVO> getYearlyStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "year", required = false) Integer year) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        int targetYear = year != null ? year : LocalDate.now().getYear();
        ScheduleStatisticsVO vo = statisticsService.getYearlyStatistics(account, targetYear);
        return Response.success(vo);
    }

    /**
     * 获取自定义日期范围统计
     * @param account 员工账号（可选）
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     */
    @GetMapping("/custom")
    public Response<ScheduleStatisticsVO> getCustomStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        ScheduleStatisticsVO vo = statisticsService.getCustomStatistics(account, start, end);
        return Response.success(vo);
    }

    /**
     * 导出统计报表
     * @param account 员工账号（可选）
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStatistics(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        String csvContent = statisticsService.exportStatistics(account, start, end);

        // 添加 BOM 以支持 Excel 正确识别 UTF-8
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(contentBytes, 0, result, bom.length, contentBytes.length);

        String filename = String.format("排班统计_%s_%s.csv", startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                URLEncoder.encode(filename, StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .headers(headers)
                .body(result);
    }

    @GetMapping("/exportMonthlyCalendar")
    public ResponseEntity<byte[]> exportMonthlyCalendar(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(value = "account", required = false) String account) {
        String currentAccount = SessionUtil.getCurrentAccount();
        boolean admin = scheduleAccessService.isAdmin(currentAccount);

        java.util.List<String> exportAccounts;
        if (admin) {
            exportAccounts = account != null && !account.isBlank()
                    ? java.util.List.of(account)
                    : java.util.List.of();
        } else {
            exportAccounts = java.util.List.of(currentAccount);
        }

        byte[] result = statisticsService.exportMonthlyCalendar(exportAccounts, year, month);
        String filename = String.format("排班月历_%d-%02d.xlsx", year, month);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                URLEncoder.encode(filename, StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .headers(headers)
                .body(result);
    }
}
