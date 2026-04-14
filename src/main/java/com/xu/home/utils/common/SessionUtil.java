package com.xu.home.utils.common;

import com.xu.home.utils.common.context.UserContext;
import com.xu.home.param.common.UserToken;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Session工具类，用于获取会话中的用户信息
 */
public class SessionUtil {

    /**
     * 获取当前登录用户信息（从ThreadLocal获取）
     * 推荐使用此方法，无需传递HttpServletRequest参数
     *
     * @return 当前登录的用户信息，如果未登录则返回null
     */
    public static UserToken getCurrentUser() {
        return UserContext.getCurrentUser();
    }

    /**
     * 获取当前登录用户的账号（从ThreadLocal获取）
     * 推荐使用此方法，无需传递HttpServletRequest参数
     *
     * @return 当前登录用户的账号，如果未登录则返回null
     */
    public static String getCurrentAccount() {
        return UserContext.getCurrentAccount();
    }

    /**
     * 获取当前登录用户的用户名（从ThreadLocal获取）
     *
     * @return 当前登录用户的用户名，如果未登录则返回null
     */
    public static String getCurrentUserName() {
        return UserContext.getCurrentUserName();
    }

    /**
     * 获取当前登录用户的手机号（从ThreadLocal获取）
     *
     * @return 当前登录用户的手机号，如果未登录则返回null
     */
    public static String getCurrentPhone() {
        return UserContext.getCurrentPhone();
    }

    // ==================== 以下为兼容旧代码的方法，建议使用上面的无参方法 ====================

    /**
     * 从HttpServletRequest的session中获取当前登录用户信息（兼容旧代码）
     * 推荐使用无参的getCurrentUser()方法
     *
     * @param request HttpServletRequest对象
     * @return 当前登录的用户信息，如果未登录则返回null
     */
    @Deprecated
    public static UserToken getCurrentUser(HttpServletRequest request) {
        if (request == null || request.getSession() == null) {
            return null;
        }
        Object currentUser = request.getSession().getAttribute("Token");
        if (currentUser instanceof UserToken userToken) {
            return userToken;
        }
        return null;
    }

    /**
     * 从HttpServletRequest的session中获取当前登录用户的账号（兼容旧代码）
     * 推荐使用无参的getCurrentAccount()方法
     *
     * @param request HttpServletRequest对象
     * @return 当前登录用户的账号，如果未登录则返回null
     */
    @Deprecated
    public static String getCurrentAccount(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 优先从request attribute获取account（拦截器已设置）
        Object accountAttr = request.getAttribute("currentAccount");
        if (accountAttr instanceof String account) {
            return account;
        }
        // 降级方案：从session的UserToken获取
        UserToken userToken = getCurrentUser(request);
        return userToken != null ? userToken.getAccount() : null;
    }
}
