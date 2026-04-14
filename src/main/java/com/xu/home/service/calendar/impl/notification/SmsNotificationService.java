package com.xu.home.service.calendar.impl.notification;

import com.xu.home.service.calendar.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信通知服务（预留实现）
 */
@Slf4j
@Service("smsNotificationService")
public class SmsNotificationService implements NotificationService {

    @Override
    public boolean send(String account, String title, String content) {
        // TODO: 集成短信服务（阿里云/腾讯云短信）
        log.info("短信通知[预留] - account: {}, title: {}, content: {}", account, title, content);
        return true;
    }

    @Override
    public String getType() {
        return "sms";
    }
}
