package com.xu.home.utils.common.enums;

/**
 * Token相关枚举常量
 */
public enum TokenEnum {
    LEGACY_KEY("xuBlog!@#123", "旧版token包装密钥key"),
    HEADER_AUTHORIZATION_KEY("Authorization", "标准认证请求头"),
    HEADER_TOKEN_KEY("token", "接口请求头部token信息key"),
    BEARER_PREFIX("Bearer ", "Bearer前缀"),
    IS_TOKEN_NULL("1", "token为空, 解析失败!"),
    ERROR_TOKEN_OVERDUE("2", "token过期,请重新登陆!"),
    ERROR_TOKEN_ANALYSIS("3", "token错误, 解析失败!"),
    ERROR_TOKEN_SIGN("4", "token错误, 签名无效!"),
    ERROR_TOKEN_KEY("5", "token生成签名失败!"),
    LOGIN_ERROR("6", "该帐号已在别处登录，请重新登录!");

    private String code;

    private String message;

    TokenEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
