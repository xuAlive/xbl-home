package com.xu.home.service.blog.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xu.home.config.blog.WechatConfig;
import com.xu.home.domain.blog.SysRole;
import com.xu.home.domain.blog.SysUser;
import com.xu.home.mapper.blog.SysUserMapper;
import com.xu.home.service.blog.SysRoleService;
import com.xu.home.service.blog.WechatAuthService;
import com.xu.home.param.common.UserToken;
import com.xu.home.utils.common.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信授权服务实现
 */
@Slf4j
@Service
public class WechatAuthServiceImpl implements WechatAuthService {

    private static final long LOGIN_CODE_EXPIRE_MILLIS = 60_000L;
    private static final Map<String, LoginCodeEntry> LOGIN_CODE_CACHE = new ConcurrentHashMap<>();

    private final WechatConfig wechatConfig;
    private final SysUserMapper sysUserMapper;
    private final SysRoleService sysRoleService;
    private final RestTemplate restTemplate;

    public WechatAuthServiceImpl(WechatConfig wechatConfig, SysUserMapper sysUserMapper,
                                  SysRoleService sysRoleService) {
        this.wechatConfig = wechatConfig;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleService = sysRoleService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getAuthUrl(String redirectPath) {
        // 默认使用静默授权，如需获取用户信息可改为 snsapi_userinfo
        return wechatConfig.getAuthUrl(redirectPath, "snsapi_userinfo");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String code) {
        // 1. 用code换取access_token和openid
        String tokenUrl = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            wechatConfig.getAppId(),
            wechatConfig.getAppSecret(),
            code
        );

        String tokenResponse = restTemplate.getForObject(tokenUrl, String.class);
        JSONObject tokenJson = JSON.parseObject(tokenResponse);
        log.info("微信授权token响应: {}", tokenResponse);

        if (tokenJson.containsKey("errcode")) {
            log.error("获取access_token失败: {}", tokenResponse);
            throw new RuntimeException("微信授权失败: " + tokenJson.getString("errmsg"));
        }

        String accessToken = tokenJson.getString("access_token");
        String openid = tokenJson.getString("openid");

        // 2. 获取用户信息（如果scope是snsapi_userinfo）
        String nickname = null;
        String avatar = null;
        String userInfoUrl = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
            accessToken, openid
        );

        try {
            String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);
            JSONObject userInfoJson = JSON.parseObject(userInfoResponse);
            log.info("微信用户信息响应: {}", userInfoResponse);

            if (!userInfoJson.containsKey("errcode")) {
                nickname = userInfoJson.getString("nickname");
                avatar = userInfoJson.getString("headimgurl");
            }
        } catch (Exception e) {
            log.warn("获取微信用户信息失败，使用静默授权", e);
        }

        // 3. 获取或创建用户
        SysUser user = getOrCreateUser(openid, nickname, avatar);

        // 4. 生成JWT Token
        UserToken userToken = new UserToken();
        userToken.setAccount(user.getAccount());
        userToken.setPhone(user.getPhone());
        userToken.setUserName(user.getWxNickname() != null ? user.getWxNickname() : user.getAccount());

        return JWTUtil.createToken(userToken);
    }

    @Override
    public String issueLoginCode(String token) {
        String loginCode = UUID.randomUUID().toString().replace("-", "");
        LOGIN_CODE_CACHE.put(loginCode, new LoginCodeEntry(token, Instant.now().toEpochMilli() + LOGIN_CODE_EXPIRE_MILLIS));
        return loginCode;
    }

    @Override
    public String exchangeLoginCode(String loginCode) {
        LoginCodeEntry entry = LOGIN_CODE_CACHE.remove(loginCode);
        if (entry == null || entry.expireAt() < Instant.now().toEpochMilli()) {
            throw new RuntimeException("登录凭证已失效");
        }
        return entry.token();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser getOrCreateUser(String openid, String nickname, String avatar) {
        // 查找已有用户
        SysUser existUser = sysUserMapper.selectByOpenid(openid);
        if (existUser != null) {
            // 更新昵称和头像
            if (nickname != null || avatar != null) {
                existUser.setWxNickname(nickname);
                existUser.setWxAvatar(avatar);
                sysUserMapper.updateById(existUser);
            }
            return existUser;
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        String account = "wx_" + UUID.randomUUID().toString().substring(0, 8);
        newUser.setAccount(account);
        newUser.setOpenid(openid);
        newUser.setWxNickname(nickname);
        newUser.setWxAvatar(avatar);
        newUser.setPassword(UUID.randomUUID().toString()); // 随机密码
        newUser.setPhoneVerified(0);
        newUser.setIsDelete(0);

        sysUserMapper.insert(newUser);

        // 分配GUEST角色
        SysRole guestRole = sysRoleService.selectByRoleCode("GUEST");
        if (guestRole != null) {
            sysRoleService.assignRoleToUser(account, guestRole.getId());
            log.info("微信用户{}创建成功，分配GUEST角色", account);
        }

        return newUser;
    }

    private record LoginCodeEntry(String token, long expireAt) {
    }
}
