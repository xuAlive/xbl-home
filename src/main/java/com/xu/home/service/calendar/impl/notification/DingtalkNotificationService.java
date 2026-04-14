package com.xu.home.service.calendar.impl.notification;

import com.xu.home.service.calendar.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 钉钉通知服务（预留实现）
 */
@Slf4j
@Service("dingtalkNotificationService")
public class DingtalkNotificationService implements NotificationService {

    @Override
    public boolean send(String account, String title, String content) {
        // TODO: 集成钉钉机器人消息推送
        log.info("钉钉通知[预留] - account: {}, title: {}, content: {}", account, title, content);
        return true;
    }

    @Override
    public String getType() {
        return "dingtalk";
    }
}
