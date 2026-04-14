package com.xu.home.param.common.response;

/**
 * 响应状态枚举
 */
public enum ResponseStates {

    SUCCESS(1, "成功"),
    ERROR(2, "异常"),
    ENPTY(3, "参数为空"),
    LOGINERROR(4, "登录失败，用户名或密码错误"),
    LOCKEDERROR(5, "账号已被禁用,请联系管理员"),
    VALIDATIONFAILURE(6, "账户验证失败"),
    INSUFFICIENTPERMISSION(7, "用户权限不足"),
    OPERATIONFAILURE(8, "操作失败"),
    REPETITION(9, "名称重复"),
    UNLOGIN(10, "未登录,请先进行登录操作"),
    ERRORKEY(11, "Lincense不匹配"),
    PASTDUE(12, "Lincense过期"),
    UNACTIVATION(13, "Lincense未激活，请先激活"),
    NOTEXIST(14, "账号不存在"),
    SECURITYPOLICY(15, "安全策略不通过"),
    IPREPETITION(16, "IP已存在"),
    RESULTENPTY(17, "结果为空"),
    IPINFORMAT(18, "IP格式错误"),
    ACCOUNT(19, "请勿操作本账户"),
    ACCOUNTGROUP(20, "请勿操作本账户组"),
    LOCKEDGROUP(21, "账号组已被禁用,请联系管理员"),
    LEVELERROR(22, "您已经拥有最高权限，无需修改"),
    NAME_EXIST(23, "名称已存在!"),
    PASSWORDSTRENGTH(24, "密码强度低"),
    LOCKEDQUARTER(25, "输入密码连续错误5次，账号被锁定15分钟"),
    PASSWORDALTER(26, "该帐号已被锁定，请联系管理员！"),
    PASSWORDNEEDCHANGE(27, "密码与近期更改的三个密码之一相同"),
    KAPTCHAERROR(28, "验证码错误"),
    TOKENERROR(29, "token错误"),
    ;

    private int code;
    private String message;

    ResponseStates(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
