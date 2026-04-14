package com.xu.home.controller.blog;

import com.xu.home.domain.blog.SystemMessage;
import com.xu.home.param.blog.po.sys.SystemMessagePO;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.blog.SysPermissionService;
import com.xu.home.service.blog.SystemMessageService;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog/system-message")
public class SystemMessageController {

    private final SystemMessageService systemMessageService;
    private final SysPermissionService sysPermissionService;

    public SystemMessageController(SystemMessageService systemMessageService, SysPermissionService sysPermissionService) {
        this.systemMessageService = systemMessageService;
        this.sysPermissionService = sysPermissionService;
    }

    @GetMapping("/list")
    public Response<List<SystemMessage>> list() {
        return Response.success(systemMessageService.listActiveMessages());
    }

    @GetMapping("/latest")
    public Response<SystemMessage> latest() {
        return Response.success(systemMessageService.getLatestActiveMessage());
    }

    @PostMapping("/create")
    public Response create(@RequestBody SystemMessagePO po) {
        String currentAccount = SessionUtil.getCurrentAccount();
        if (!sysPermissionService.hasPermission(currentAccount, "system:user:list")) {
            return Response.error("仅管理员可发布系统消息");
        }
        return systemMessageService.createMessage(currentAccount, po);
    }
}
