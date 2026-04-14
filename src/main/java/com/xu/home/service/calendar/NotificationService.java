package com.xu.home.service.calendar;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知
     * @param account 用户账号
     * @param title 通知标题
     * @param content 通知内容
     * @return 是否发送成功
     */
    boolean send(String account, String title, String content);

    /**
     * 获取通知类型
     */
    String getType();
}
