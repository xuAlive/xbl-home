package com.xu.home.controller.blog.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xu.home.domain.blog.SysRole;
import com.xu.home.domain.blog.SysUser;
import com.xu.home.service.blog.SysRoleService;
import com.xu.home.service.blog.SysUserService;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理Controller
 */
@Slf4j
@RequestMapping("/blog/user")
@RestController
public class UserController {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;

    public UserController(SysUserService sysUserService, SysRoleService sysRoleService) {
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
    }

    /**
     * 验证手机号，将用户从GUEST升级为USER
     * @param phone 手机号
     */
    @PostMapping("/verifyPhone")
    @Transactional(rollbackFor = Exception.class)
    public Response verifyPhone(@RequestParam("phone") String phone) {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            return Response.error("未登录");
        }

        // 校验手机号格式
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Response.error("手机号格式不正确");
        }

        try {
            // 更新用户手机号和验证状态
            SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>().eq("account", account));
            if (user == null) {
                return Response.error("用户不存在");
            }

            if (user.getPhoneVerified() != null && user.getPhoneVerified() == 1) {
                return Response.error("手机号已验证");
            }

            // 检查手机号是否已被其他用户绑定
            SysUser existingUser = sysUserService.getOne(
                    new QueryWrapper<SysUser>().eq("phone", phone).ne("account", account));
            if (existingUser != null) {
                return Response.error("该手机号已被其他用户绑定");
            }

            user.setPhone(phone);
            user.setPhoneVerified(1);
            boolean updateResult = sysUserService.updateById(user);

            if (updateResult) {
                // 游客验证成功后升级为正式用户，并移除游客角色，避免菜单/权限仍然做并集
                SysRole userRole = sysRoleService.selectByRoleCode("USER");
                if (userRole != null) {
                    sysRoleService.assignRoleToUser(account, userRole.getId());
                    SysRole guestRole = sysRoleService.selectByRoleCode("GUEST");
                    if (guestRole != null) {
                        sysRoleService.removeRoleFromUser(account, guestRole.getId());
                    }
                    log.info("用户{}验证手机号成功，升级为USER角色", account);
                    return Response.success("手机号验证成功，已升级为正式用户");
                } else {
                    log.warn("USER角色不存在，请检查数据库");
                    return Response.error("系统配置错误");
                }
            }
            return Response.error("验证失败");
        } catch (Exception e) {
            log.error("验证手机号失败", e);
            throw new RuntimeException("验证手机号失败");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/getCurrentUser")
    public Response getCurrentUser() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            return Response.error("未登录");
        }

        SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>().eq("account", account));
        if (user != null) {
            // 不返回密码
            user.setPassword(null);
            return Response.success(user);
        }
        return Response.error("用户不存在");
    }
}
