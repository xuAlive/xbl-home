package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.NotificationLog;

import java.util.List;

/**
 * 通知日志服务
 */
public interface NotificationLogService extends IService<NotificationLog> {

    /**
     * 获取当前用户的通知投递记录
     */
    List<NotificationLog> getLogsByAccount(String account);
}
