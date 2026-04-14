package com.xu.home.service.ai.deepseek;

import com.alibaba.fastjson2.JSONObject;
import com.xu.home.param.blog.deepseek.CompletionResponse;
import com.xu.home.param.blog.po.deepseek.DialogueInfoPO;
import com.xu.home.param.blog.vo.ds.CompletionVO;
import com.xu.home.service.blog.DeepseekDialogueInfoService;
import com.xu.home.utils.DeepSeekAPIUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
public class DeepSeekDialogueService {

    private final DeepseekDialogueInfoService deepseekDialogueInfoService;
    private final DeepSeekAPIUtil deepSeekAPIUtil;

    public DeepSeekDialogueService(DeepseekDialogueInfoService deepseekDialogueInfoService, DeepSeekAPIUtil deepSeekAPIUtil) {
        this.deepseekDialogueInfoService = deepseekDialogueInfoService;
        this.deepSeekAPIUtil = deepSeekAPIUtil;
    }

    public CompletionVO sendCompletion(DialogueInfoPO po){
        // 保存本次输入
        if (Objects.isNull(po)){
            return null;
        }
        if (StringUtils.isBlank(po.getContent())){
            return null;
        }
        if (Objects.isNull(po.getDialogueId())){
            long millis = System.currentTimeMillis();
            po.setDialogueId(millis);
        }
        po.setRole("user");
        deepseekDialogueInfoService.saveDialogueInfo(po);
        // 发送ds请求 保存返回
        CompletionVO result = sendMessage(po);
        return result;
    }

    private CompletionVO sendMessage(DialogueInfoPO po){
        if (Objects.isNull(po)){
            return null;
        }
        CompletionVO vo = new CompletionVO();
        BeanUtils.copyProperties(po,vo);
        List<CompletionVO> dialogueInfo = deepseekDialogueInfoService.getDialogueInfo(vo.getDialogueId(), po.getAccount());
        if (CollectionUtils.isEmpty(dialogueInfo)){
            dialogueInfo = new ArrayList<>();
        }
        dialogueInfo.add(vo);
        List<Map<String, String>> mapList = new ArrayList<>();
        for (CompletionVO completionVO : dialogueInfo) {
            Map<String, String> map = new HashMap<>();
            map.put("role", completionVO.getRole());
            map.put("content", completionVO.getContent());
            mapList.add(map);
        }
        // 构造请求体
        JSONObject body = new JSONObject();
        body.put("model", deepSeekAPIUtil.getChatModel());
        body.put("messages", mapList); // 使用直接嵌套对象而非字符串
        log.warn("请求体: {}", body.toJSONString());
        List<CompletionResponse.Choice> completions = deepSeekAPIUtil.completions(body.toJSONString());
        log.warn("响应: {}", completions);
        CompletionVO result = new CompletionVO();
        result.setDialogueId(po.getDialogueId());
        if (!CollectionUtils.isEmpty(completions)){
            CompletionResponse.Choice choice = completions.get(0);
            result.setRole(choice.getMessage().getRole());
            result.setContent(choice.getMessage().getContent());
            DialogueInfoPO resultPo = new DialogueInfoPO();
            BeanUtils.copyProperties(result,resultPo);
            resultPo.setAccount(po.getAccount());
            deepseekDialogueInfoService.saveDialogueInfo(resultPo);
        }
        return result;
    }

}
