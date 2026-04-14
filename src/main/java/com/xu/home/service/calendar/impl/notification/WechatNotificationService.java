package com.xu.home.service.calendar.impl.notification;

import com.xu.home.service.calendar.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信通知服务（预留实现）
 */
@Slf4j
@Service("wechatNotificationService")
public class WechatNotificationService implements NotificationService {

    @Override
    public boolean send(String account, String title, String content) {
        // TODO: 集成微信公众号/小程序消息推送
        log.info("微信通知[预留] - account: {}, title: {}, content: {}", account, title, content);
        return true;
    }

    @Override
    public String getType() {
        return "wechat";
    }
}
