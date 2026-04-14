package com.xu.home.service.calendar.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.NotificationMethod;
import com.xu.home.mapper.calendar.NotificationMethodMapper;
import com.xu.home.service.calendar.NotificationMethodService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知方式服务实现
 */
@Service
public class NotificationMethodServiceImpl extends ServiceImpl<NotificationMethodMapper, NotificationMethod>
        implements NotificationMethodService {

    @Override
    public List<NotificationMethod> getActiveMethods() {
        return lambdaQuery()
                .eq(NotificationMethod::getStatus, 1)
                .orderByAsc(NotificationMethod::getSortOrder)
                .list();
    }
}
