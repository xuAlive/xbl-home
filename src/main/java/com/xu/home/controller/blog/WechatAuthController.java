package com.xu.home.controller.blog;

import com.xu.home.service.blog.WechatAuthService;
import com.xu.home.param.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 微信授权Controller
 */
@Slf4j
@Controller
@RequestMapping("/blog/wechat")
public class WechatAuthController {

    private final WechatAuthService wechatAuthService;

    public WechatAuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    /**
     * 获取微信授权链接
     * @param redirectPath 授权后跳转的前端路径，如 /schedule
     * @return 授权链接
     */
    @GetMapping("/authUrl")
    @ResponseBody
    public Response getAuthUrl(@RequestParam(defaultValue = "/index") String redirectPath) {
        String authUrl = wechatAuthService.getAuthUrl(redirectPath);
        return Response.success(authUrl);
    }

    /**
     * 微信授权回调
     * 微信会将用户重定向到此接口，携带code参数
     * @param code 微信授权码
     * @param target 目标跳转路径
     * @param response HttpServletResponse
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(defaultValue = "/index") String target,
                         HttpServletResponse response) throws IOException {
        try {
            log.info("收到微信授权回调, code: {}, target: {}", code, target);

            String token = wechatAuthService.handleCallback(code);
            String loginCode = wechatAuthService.issueLoginCode(token);

            String redirectUrl = target + (target.contains("?") ? "&" : "?") + "authCode=" + loginCode;
            log.info("微信授权成功，重定向到目标页面");

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("微信授权失败", e);
            // 授权失败，重定向到登录页
            response.sendRedirect("/login?error=wechat_auth_failed");
        }
    }

    /**
     * 前端入口 - 跳转到微信授权
     * 公众号菜单可直接配置此链接
     * @param target 目标应用路径
     * @param response HttpServletResponse
     */
    @GetMapping("/auth")
    public void auth(@RequestParam(defaultValue = "/index") String target,
                     HttpServletResponse response) throws IOException {
        String authUrl = wechatAuthService.getAuthUrl(target);
        log.info("跳转微信授权, target: {}, authUrl: {}", target, authUrl);
        response.sendRedirect(authUrl);
    }

    @PostMapping("/exchange")
    @ResponseBody
    public Response exchange(@RequestParam("code") String code) {
        return Response.success(wechatAuthService.exchangeLoginCode(code));
    }
}
