package com.xu.home.Interceptor.common;

import com.alibaba.fastjson2.JSON;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xu.home.utils.common.context.UserContext;
import com.xu.home.utils.common.enums.TokenEnum;
import com.xu.home.param.common.UserToken;
import com.xu.home.utils.common.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 通用 Token 鉴权拦截器。
 * 负责解析请求头中的 token，并把用户上下文写入当前线程。
 */
@Slf4j
@Component
public class CommonTokenHandlerInterceptor implements HandlerInterceptor {

    private static final String TOKEN_SPLIT = "##";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求不做鉴权，直接放行。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
        // 从请求中解析出token
            String token = resolveToken(request);
        // 如果token为空，返回未登录错误
            if (StringUtils.isBlank(token)) {
                sendError(response, 401, "未登录，请先登录");
                return false;
            }

        // 解析token中的数据
            Map<String, Claim> map = JWTUtil.analysisToken(token);
        // 检查token中是否包含用户信息
            if (map.containsKey("user")) {
            // 获取用户信息字符串
                String tokenStr = map.get("user").asString();
            // 将用户信息字符串转换为UserToken对象
                UserToken userToken = JSON.parseObject(tokenStr, UserToken.class);
            // 设置当前用户到上下文
                UserContext.setCurrentUser(userToken);
            // 将当前用户账号设置到请求属性中
                request.setAttribute("currentAccount", userToken != null ? userToken.getAccount() : null);
                return true;
            }

        // Token解析失败返回错误
            sendError(response, 401, "Token解析失败");
            return false;
        } catch (TokenExpiredException e) {
        // 处理token过期异常
            sendError(response, 401, TokenEnum.ERROR_TOKEN_OVERDUE.getMessage());
            return false;
        } catch (JWTDecodeException e) {
            sendError(response, 401, TokenEnum.ERROR_TOKEN_ANALYSIS.getMessage());
            return false;
        } catch (SignatureVerificationException e) {
            sendError(response, 401, TokenEnum.ERROR_TOKEN_SIGN.getMessage());
        // 处理token签名验证异常
            return false;
        } catch (Exception e) {
            log.error("Token验证异常", e);
        // 处理其他异常
            sendError(response, 401, "Token验证失败");
            return false;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(TokenEnum.HEADER_AUTHORIZATION_KEY.getCode());
        if (StringUtils.isNotBlank(authorization)
                && authorization.startsWith(TokenEnum.BEARER_PREFIX.getCode())) {
            return authorization.substring(TokenEnum.BEARER_PREFIX.getCode().length()).trim();
        }

        String legacyToken = request.getHeader(TokenEnum.HEADER_TOKEN_KEY.getCode());
        if (StringUtils.isBlank(legacyToken)) {
            return null;
        }
        return unwrapLegacyToken(legacyToken);
    }

    private String unwrapLegacyToken(String token) {
        String[] parts = token.split(TOKEN_SPLIT);
        if (parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
            throw new RuntimeException(TokenEnum.ERROR_TOKEN_ANALYSIS.getMessage());
        }

        long now = System.currentTimeMillis();
        int currentMinute = (int) ((now - (now % (1000 * 60))) / 1000);
        long currentMinuteMillis = currentMinute * 1000L;
        long previousMinuteMillis = currentMinuteMillis - 1000 * 60L;

        String currentSign = DigestUtils.md5DigestAsHex((TokenEnum.LEGACY_KEY.getCode() + currentMinuteMillis).getBytes()).toUpperCase();
        if (parts[1].equals(currentSign)) {
            return parts[0];
        }

        String previousSign = DigestUtils.md5DigestAsHex((TokenEnum.LEGACY_KEY.getCode() + previousMinuteMillis).getBytes()).toUpperCase();
        if (parts[1].equals(previousSign)) {
            return parts[0];
        }

        throw new RuntimeException(TokenEnum.ERROR_TOKEN_ANALYSIS.getMessage());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void sendError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + status + ",\"codeMessage\":\"" + message + "\"}");
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
        }
    }
}
