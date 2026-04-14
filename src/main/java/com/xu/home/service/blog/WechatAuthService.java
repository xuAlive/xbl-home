package com.xu.home.service.blog;

import com.xu.home.domain.blog.SysUser;

/**
 * 微信授权服务接口
 */
public interface WechatAuthService {

    /**
     * 获取网页授权链接
     * @param redirectPath 授权后跳转的前端路径
     * @return 授权链接
     */
    String getAuthUrl(String redirectPath);

    /**
     * 处理微信授权回调
     * @param code 微信授权码
     * @return 用户信息（包含token）
     */
    String handleCallback(String code);

    String issueLoginCode(String token);

    String exchangeLoginCode(String loginCode);

    /**
     * 根据openid获取或创建用户
     * @param openid 微信openid
     * @param nickname 微信昵称
     * @param avatar 微信头像
     * @return 用户信息
     */
    SysUser getOrCreateUser(String openid, String nickname, String avatar);
}
