package com.xu.home.utils.common.context;

import com.xu.home.param.common.UserToken;

/**
 * 用户上下文，使用ThreadLocal存储当前请求的用户信息
 * 在拦截器中设置，在Controller中通过静态方法获取
 */
public class UserContext {

    /**
     * 使用ThreadLocal存储用户信息，保证线程安全
     */
    private static final ThreadLocal<UserToken> userThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户信息
     * 由TokenHandlerAdapter拦截器调用
     *
     * @param userToken 用户信息
     */
    public static void setCurrentUser(UserToken userToken) {
        userThreadLocal.set(userToken);
    }

    /**
     * 获取当前线程的用户信息
     *
     * @return 当前用户信息，如果未登录则返回null
     */
    public static UserToken getCurrentUser() {
        return userThreadLocal.get();
    }

    /**
     * 获取当前用户的账号
     *
     * @return 当前用户账号，如果未登录则返回null
     */
    public static String getCurrentAccount() {
        UserToken userToken = getCurrentUser();
        return userToken != null ? userToken.getAccount() : null;
    }

    /**
     * 获取当前用户的用户名
     *
     * @return 当前用户名，如果未登录则返回null
     */
    public static String getCurrentUserName() {
        UserToken userToken = getCurrentUser();
        return userToken != null ? userToken.getUserName() : null;
    }

    /**
     * 获取当前用户的手机号
     *
     * @return 当前用户手机号，如果未登录则返回null
     */
    public static String getCurrentPhone() {
        UserToken userToken = getCurrentUser();
        return userToken != null ? userToken.getPhone() : null;
    }

    /**
     * 清除当前线程的用户信息
     * 必须在请求结束后调用，避免内存泄漏
     * 由TokenHandlerAdapter拦截器的afterCompletion方法调用
     */
    public static void clear() {
        userThreadLocal.remove();
    }
}
