package com.xu.home.param.common;

import lombok.Data;

/**
 * 用户Token载体
 * 存储在JWT中的用户基本信息
 */
@Data
public class UserToken {
    private String phone;
    private String account;
    private String userName;
}
