package com.xu.home.controller.calendar;

import com.xu.home.domain.calendar.Reminder;
import com.xu.home.param.calendar.po.ReminderPO;
import com.xu.home.param.calendar.vo.ReminderVO;
import com.xu.home.service.calendar.ReminderService;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提醒管理接口
 */
@RestController
@RequestMapping("/calendar/reminder")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * 创建提醒
     */
    @PostMapping("/create")
    public Response<?> createReminder(@RequestBody ReminderPO po) {
        Reminder reminder = new Reminder();
        BeanUtils.copyProperties(po, reminder);
        reminder.setAccount(SessionUtil.getCurrentAccount());
        reminder.setStatus(1);  // 待发送
        reminder.setRetryCount(0);
        reminder.setCreateTime(LocalDateTime.now());
        reminder.setUpdateTime(LocalDateTime.now());
        reminder.setIsDelete(0);
        boolean result = reminderService.save(reminder);
        return Response.checkResult(result);
    }

    /**
     * 更新提醒
     */
    @PostMapping("/update")
    public Response<?> updateReminder(@RequestBody ReminderPO po) {
        if (po.getId() == null) {
            return Response.error("提醒ID不能为空");
        }
        Reminder reminder = new Reminder();
        BeanUtils.copyProperties(po, reminder);
        reminder.setUpdateTime(LocalDateTime.now());
        boolean result = reminderService.updateById(reminder);
        return Response.checkResult(result);
    }

    /**
     * 删除提醒（软删除）
     */
    @PostMapping("/delete")
    public Response<?> deleteReminder(@RequestBody IdPO po) {
        Long id = po.getId();
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setIsDelete(1);
        reminder.setUpdateTime(LocalDateTime.now());
        boolean result = reminderService.updateById(reminder);
        return Response.checkResult(result);
    }

    /**
     * 获取提醒列表
     */
    @GetMapping("/list")
    public Response<List<ReminderVO>> getReminderList() {
        String account = SessionUtil.getCurrentAccount();
        List<Reminder> reminders = reminderService.getRemindersByAccount(account);
        List<ReminderVO> voList = reminders.stream().map(this::toVO).collect(Collectors.toList());
        return Response.success(voList);
    }

    /**
     * 取消提醒
     */
    @PostMapping("/cancel")
    public Response<?> cancelReminder(@RequestBody IdPO po) {
        boolean result = reminderService.cancelReminder(po.getId());
        return Response.checkResult(result);
    }

    /**
     * 重试失败的提醒
     */
    @PostMapping("/retry")
    public Response<?> retryReminder(@RequestBody IdPO po) {
        boolean result = reminderService.retryReminder(po.getId());
        return Response.checkResult(result);
    }

    private ReminderVO toVO(Reminder reminder) {
        ReminderVO vo = new ReminderVO();
        BeanUtils.copyProperties(reminder, vo);
        return vo;
    }
}
