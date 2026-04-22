package com.xu.home.controller.blog.sys;

import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.dao.blog.SysUserDao;
import com.xu.home.param.blog.po.sys.ChangePasswordPo;
import com.xu.home.param.blog.po.sys.LoginUserPo;
import com.xu.home.param.blog.po.sys.UserInfoPo;
import com.xu.home.param.blog.vo.sys.LoginLocationStatsVO;
import com.xu.home.service.blog.SysUserInfoService;
import com.xu.home.service.blog.SysUserService;
import com.xu.home.service.blog.SysPermissionService;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.common.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RequestMapping("/blog/sys")
@RestController
public class SysController {

    private final SysUserService sysUserService;
    private final SysUserInfoService sysUserInfoService;
    private final SysUserDao sysUserDao;
    private final SysPermissionService sysPermissionService;

    public SysController(SysUserService sysUserService, SysUserInfoService sysUserInfoService,
                         SysUserDao sysUserDao, SysPermissionService sysPermissionService) {
        this.sysUserService = sysUserService;
        this.sysUserInfoService = sysUserInfoService;
        this.sysUserDao = sysUserDao;
        this.sysPermissionService = sysPermissionService;
    }

    @PostMapping("/login")
    public Response login(@RequestBody LoginUserPo po, HttpServletRequest servletRequest){
        return sysUserService.login(po,servletRequest);
    }

    @PostMapping("/register")
    public Response register(@RequestBody LoginUserPo po){
        return sysUserService.register(po);
    }

    @GetMapping("/getUserInfoByAccount")
    public Response getUserInfoByAccount(@RequestParam(value = "account",required = false) String account){
        String currentAccount = SessionUtil.getCurrentAccount();
        if (StringUtils.isBlank( account)){
            account = currentAccount;
        } else if (!StringUtils.equals(account, currentAccount)
                && !sysPermissionService.hasPermission(currentAccount, "system:user:list")) {
            return Response.error("无权查看其他用户信息");
        }
        return sysUserInfoService.getUserInfoByAccount(account);
    }

    @PostMapping("/updateUserInfo")
    public Response updateUserInfo(@RequestBody UserInfoPo po){
        String currentAccount = SessionUtil.getCurrentAccount();
        if (po == null || StringUtils.isBlank(po.getAccount())) {
            return Response.error("账号不能为空");
        }
        if (!StringUtils.equals(po.getAccount(), currentAccount)
                && !sysPermissionService.hasPermission(currentAccount, "system:user:list")) {
            return Response.error("无权修改其他用户信息");
        }
        return sysUserInfoService.updateUserInfo(po);
    }

    @PostMapping("/changePassword")
    public Response changePassword(@RequestBody ChangePasswordPo po) {
        String currentAccount = SessionUtil.getCurrentAccount();
        if (StringUtils.isBlank(currentAccount)) {
            return Response.error("未登录");
        }
        return sysUserService.changePassword(currentAccount, po);
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/getUserList")
    @RequirePermission("system:user:list")
    public Response getUserList(){
        return sysUserInfoService.getUserList();
    }

    /**
     * 查询用户登录记录（按账号和IP分组，分页）
     * @param account 账号（可选，不传则查询所有账号）
     * @param page 页码，默认1
     * @param size 每页大小，默认5
     * @return 分页登录记录
     */
    @GetMapping("/getLoginRecords")
    @RequirePermission("system:login:records")
    public Response getLoginRecords(@RequestParam(value = "account", required = false) String account,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "size", defaultValue = "5") int size){
         Map<String, Object> result = sysUserDao.getLoginRecords(account, page, size);
         return Response.success(result);
    }

    /**
     * 获取登录地点统计信息（地图标点 + 省份饼形图）
     */
    @GetMapping("/getLoginLocationStats")
    @RequirePermission("system:login:stats")
    public Response getLoginLocationStats(@RequestParam(value = "account", required = false) String account){
         LoginLocationStatsVO stats = sysUserDao.getLoginLocationStats(account);
         return Response.success(stats);
    }
}
