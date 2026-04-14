package com.xu.home.controller.blog;

import com.xu.home.param.blog.po.deepseek.DialogueInfoPO;
import com.xu.home.service.blog.DeepseekDialogueInfoService;
import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.param.common.IdPO;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.ai.DeepSeekLangChain4jDemoService;
import com.xu.home.service.ai.deepseek.DeepSeekDialogueService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/blog/ds")
@RestController
public class DeepseekController {

    private final DeepSeekDialogueService deepSeekDialogueService;
    private final DeepSeekLangChain4jDemoService deepSeekLangChain4jDemoService;
    private final DeepseekDialogueInfoService deepseekDialogueInfoService;

    public DeepseekController(DeepSeekDialogueService deepSeekDialogueService,
                              DeepSeekLangChain4jDemoService deepSeekLangChain4jDemoService,
                              DeepseekDialogueInfoService deepseekDialogueInfoService) {
        this.deepSeekDialogueService = deepSeekDialogueService;
        this.deepSeekLangChain4jDemoService = deepSeekLangChain4jDemoService;
        this.deepseekDialogueInfoService = deepseekDialogueInfoService;
    }

    @PostMapping("/sendCompletion")
    @RequirePermission("deepseek:chat")
    public Response sendCompletion(@RequestBody DialogueInfoPO po){
        // 从ThreadLocal获取当前登录用户账号，无需传递HttpServletRequest
        String account = SessionUtil.getCurrentAccount();
        po.setAccount(account);
        return Response.success(deepSeekDialogueService.sendCompletion(po));
    }

    @PostMapping("/langchain4jDemo")
    @RequirePermission("deepseek:chat")
    public Response langchain4jDemo(@RequestBody DialogueInfoPO po){
        if (po == null || !StringUtils.hasText(po.getContent())) {
            return Response.error("内容不能为空");
        }
        return Response.success(deepSeekLangChain4jDemoService.chat(po.getContent()));
    }

    @GetMapping("/getCompletionList")
    public Response getCompletion(@RequestParam("dialogueId") Long dialogueId){
        // 从ThreadLocal获取当前登录用户账号，无需传递HttpServletRequest
        String account = SessionUtil.getCurrentAccount();
        return Response.success(deepseekDialogueInfoService.getDialogueInfo(dialogueId, account));
    }

    @GetMapping("/getCompletionHistoryList")
    public Response getCompletionHistoryList(){
        // 从ThreadLocal获取当前登录用户账号，无需传递HttpServletRequest
        String account = SessionUtil.getCurrentAccount();
        return Response.success(deepseekDialogueInfoService.getCompletionHistoryList(account));
    }

    @PostMapping("/deleteDialogue")
    @RequirePermission("deepseek:delete")
    public Response deleteDialogue(@RequestBody IdPO po){
        String account = SessionUtil.getCurrentAccount();
        boolean success = deepseekDialogueInfoService.deleteDialogue(po.getId(), account);
        if (success) {
            return Response.success("删除成功");
        } else {
            return Response.error("删除失败");
        }
    }

    @GetMapping("/getDialogueCount")
    public Response getDialogueCount(){
        String account = SessionUtil.getCurrentAccount();
        int count = deepseekDialogueInfoService.countDialogueByAccount(account);
        return Response.success(count);
    }

    @GetMapping("/checkAdmin")
    public Response checkAdmin(){
        String account = SessionUtil.getCurrentAccount();
        boolean isAdmin = deepseekDialogueInfoService.isAdmin(account);
        return Response.success(isAdmin);
    }
}
