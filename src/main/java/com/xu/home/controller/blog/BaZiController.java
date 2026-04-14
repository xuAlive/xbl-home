package com.xu.home.controller.blog;

import com.xu.home.param.blog.po.bazi.BaziFortunePO;
import com.xu.home.param.blog.po.bazi.BaziMarriagePO;
import com.xu.home.param.blog.vo.bazi.BaziFortuneHistoryVO;
import com.xu.home.param.blog.vo.bazi.BaziFortuneRecordVO;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.ai.bazi.BaziFortuneService;
import com.xu.home.utils.BaZiUtil;
import com.xu.home.utils.common.SessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 生辰八字计算接口
 */
@Slf4j
@RequestMapping("/blog/bazi")
@RestController
public class BaZiController {

    private final BaziFortuneService baziFortuneService;

    public BaZiController(BaziFortuneService baziFortuneService) {
        this.baziFortuneService = baziFortuneService;
    }

    /**
     * 根据出生日期计算生辰八字
     *
     * @param year  出生年份
     * @param month 出生月份（1-12）
     * @param day   出生日期
     * @param hour  出生小时（0-23），可选参数
     * @return 八字信息
     */
    @GetMapping("/calculate")
    public Response<Map<String, Object>> calculate(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam("day") Integer day,
            @RequestParam(value = "hour", required = false) Integer hour) {

        // 参数校验
        if (year == null || year < 1 || year > 9999) {
            return Response.error("年份参数无效");
        }
        if (month == null || month < 1 || month > 12) {
            return Response.error("月份参数无效，应为1-12");
        }
        if (day == null || day < 1 || day > 31) {
            return Response.error("日期参数无效，应为1-31");
        }
        if (hour != null && (hour < 0 || hour > 23)) {
            return Response.error("小时参数无效，应为0-23");
        }

        // 校验日期合法性
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
            daysInMonth[1] = 29;
        }
        if (day > daysInMonth[month - 1]) {
            return Response.error("日期超出该月最大天数");
        }

        try {
            Map<String, Object> result = BaZiUtil.calculate(year, month, day, hour);
            log.info("计算八字: {}-{}-{} {}时, 结果: {}", year, month, day, hour, result.get("baZi"));
            return Response.success(result);
        } catch (Exception e) {
            log.error("计算八字出错", e);
            return Response.error("计算八字出错: " + e.getMessage());
        }
    }

    @PostMapping(value = "/fortune/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFortune(@RequestBody BaziFortunePO po) {
        return baziFortuneService.streamFortune(po, SessionUtil.getCurrentAccount());
    }

    @PostMapping(value = "/marriage/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMarriage(@RequestBody BaziMarriagePO po) {
        return baziFortuneService.streamMarriage(po);
    }

    @GetMapping("/fortune/history")
    public Response<List<BaziFortuneHistoryVO>> getFortuneHistory() {
        return Response.success(baziFortuneService.getHistory(SessionUtil.getCurrentAccount()));
    }

    @GetMapping("/fortune/detail/{id}")
    public Response<BaziFortuneRecordVO> getFortuneDetail(@PathVariable("id") Long id) {
        return Response.success(baziFortuneService.getDetail(id, SessionUtil.getCurrentAccount()));
    }
}
