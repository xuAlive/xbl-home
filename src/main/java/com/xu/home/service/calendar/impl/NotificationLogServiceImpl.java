package com.xu.home.service.calendar.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.calendar.NotificationLog;
import com.xu.home.mapper.calendar.NotificationLogMapper;
import com.xu.home.service.calendar.NotificationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知日志服务实现
 */
@Service
public class NotificationLogServiceImpl extends ServiceImpl<NotificationLogMapper, NotificationLog>
        implements NotificationLogService {

    @Override
    public List<NotificationLog> getLogsByAccount(String account) {
        return lambdaQuery()
                .eq(NotificationLog::getAccount, account)
                .orderByDesc(NotificationLog::getSendTime)
                .orderByDesc(NotificationLog::getCreateTime)
                .list();
    }
}
