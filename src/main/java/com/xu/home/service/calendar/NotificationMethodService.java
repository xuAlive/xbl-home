package com.xu.home.service.calendar;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.calendar.NotificationMethod;

import java.util.List;

/**
 * 通知方式服务接口
 */
public interface NotificationMethodService extends IService<NotificationMethod> {

    /**
     * 获取所有启用的通知方式
     */
    List<NotificationMethod> getActiveMethods();
}
