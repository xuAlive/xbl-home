package com.xu.home.controller.calendar;

import com.xu.home.domain.calendar.Event;
import com.xu.home.param.calendar.po.EventPO;
import com.xu.home.param.calendar.vo.EventVO;
import com.xu.home.service.calendar.EventService;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件/记事管理接口
 */
@RestController
@RequestMapping("/calendar/event")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 创建事件
     */
    @PostMapping("/create")
    public Response<?> createEvent(@RequestBody EventPO po) {
        Event event = new Event();
        BeanUtils.copyProperties(po, event);
        event.setAccount(SessionUtil.getCurrentAccount());
        event.setStatus(1);  // 待完成
        event.setCreateTime(LocalDateTime.now());
        event.setUpdateTime(LocalDateTime.now());
        event.setIsDelete(0);
        boolean result = eventService.save(event);
        return Response.checkResult(result);
    }

    /**
     * 更新事件
     */
    @PostMapping("/update")
    public Response<?> updateEvent(@RequestBody EventPO po) {
        if (po.getId() == null) {
            return Response.error("事件ID不能为空");
        }
        Event event = new Event();
        BeanUtils.copyProperties(po, event);
        event.setUpdateTime(LocalDateTime.now());
        boolean result = eventService.updateById(event);
        return Response.checkResult(result);
    }

    /**
     * 删除事件（软删除）
     */
    @PostMapping("/delete")
    public Response<?> deleteEvent(@RequestBody IdPO po) {
        Long id = po.getId();
        Event event = new Event();
        event.setId(id);
        event.setIsDelete(1);
        event.setUpdateTime(LocalDateTime.now());
        boolean result = eventService.updateById(event);
        return Response.checkResult(result);
    }

    /**
     * 查询事件列表（按日期范围）
     */
    @GetMapping("/list")
    public Response<List<EventVO>> getEventList(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        String account = SessionUtil.getCurrentAccount();
        List<Event> events = eventService.getEventsByDateRange(account, startDate, endDate);
        List<EventVO> voList = events.stream().map(this::toVO).collect(Collectors.toList());
        return Response.success(voList);
    }

    /**
     * 获取事件详情
     */
    @GetMapping("/detail/{id}")
    public Response<EventVO> getEventDetail(@PathVariable Long id) {
        Event event = eventService.getById(id);
        if (event == null || event.getIsDelete() == 1) {
            return Response.error("事件不存在");
        }
        return Response.success(toVO(event));
    }

    /**
     * 获取月度事件汇总
     */
    @GetMapping("/month")
    public Response<List<EventVO>> getMonthEvents(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        String account = SessionUtil.getCurrentAccount();
        List<Event> events = eventService.getMonthEvents(account, year, month);
        List<EventVO> voList = events.stream().map(this::toVO).collect(Collectors.toList());
        return Response.success(voList);
    }

    /**
     * 更新事件状态
     */
    @PostMapping("/status")
    public Response<?> updateStatus(@RequestBody EventPO po) {
        boolean result = eventService.updateStatus(po.getId(), po.getStatus());
        return Response.checkResult(result);
    }

    private EventVO toVO(Event event) {
        EventVO vo = new EventVO();
        BeanUtils.copyProperties(event, vo);
        return vo;
    }
}





