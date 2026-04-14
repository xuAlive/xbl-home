package com.xu.home.controller.blog;

import com.xu.home.domain.blog.Miniapp;
import com.xu.home.service.blog.MiniappService;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序Controller
 */
@RequestMapping("/blog/miniapp")
@RestController
public class MiniappController {

    private final MiniappService miniappService;

    public MiniappController(MiniappService miniappService) {
        this.miniappService = miniappService;
    }

    /**
     * 获取小程序列表
     */
    @GetMapping("/list")
    public Response getList() {
        List<Miniapp> list = miniappService.getValidList();
        return Response.success(list);
    }

    @GetMapping("/manage/list")
    public Response getManageList() {
        return Response.success(miniappService.getManageList());
    }

    @PostMapping("/manage/offline")
    public Response offline(@RequestBody IdPO po) {
        return Response.checkResult(miniappService.offline(po.getId().intValue()));
    }

    @PostMapping("/manage/online")
    public Response online(@RequestBody IdPO po) {
        return Response.checkResult(miniappService.online(po.getId().intValue()));
    }
}
