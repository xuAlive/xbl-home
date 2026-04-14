package com.xu.home.utils.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.xu.home.param.common.UserToken;

import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JWT Token
 */
public class JWTUtil {

    private static final long DEFAULT_TIME_OUT = 2 * 60 * 60 * 1000L;
    private static final String DEFAULT_SECRET = "change-this-in-env";
    private static volatile String secret = DEFAULT_SECRET;
    private static volatile long timeout = DEFAULT_TIME_OUT;

    /**
     * 解析Token，同时也能验证Token
     * 当验证失败会抛出异常
     *
     * @param token JWT token字符串
     * @return Claims信息
     */
    public static Map<String, Claim> analysisToken(String token) {
        JWTVerifier verifier = JWT.require(getAlgorithm()).build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaims();
    }

    /**
     * 根据密钥获取签名算法
     *
     * @return HMAC512算法
     */
    public static Algorithm getAlgorithm() {
        return Algorithm.HMAC512(secret);
    }

    /**
     * 创建Token
     *
     * @param userToken 用户信息
     * @return JWT token字符串
     */
    public static String createToken(UserToken userToken) {
        return JWT.create()
                .withClaim("user", JSON.toJSONString(userToken))
                .withClaim("userName", userToken.getUserName())
                .withExpiresAt(new Date(System.currentTimeMillis() + timeout))
                .sign(getAlgorithm());
    }

    public static void configure(String jwtSecret, Long jwtTimeout) {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            secret = jwtSecret;
        } else {
            secret = DEFAULT_SECRET;
        }
        if (jwtTimeout != null && jwtTimeout > 0) {
            timeout = jwtTimeout;
        } else {
            timeout = DEFAULT_TIME_OUT;
        }
    }

    /**
     * 从Token中解析用户信息
     *
     * @param token JWT token字符串
     * @return 用户信息，解析失败返回null
     */
    public static UserToken parseUserToken(String token) {
        try {
            Map<String, Claim> claims = analysisToken(token);
            if (claims.containsKey("user")) {
                String tokenStr = claims.get("user").asString();
                return JSONObject.parseObject(tokenStr, UserToken.class);
            }
        } catch (Exception e) {
            // Token解析失败
        }
        return null;
    }
}
