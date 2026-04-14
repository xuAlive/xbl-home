package com.xu.home.service.blog;

import com.xu.home.domain.blog.DeepseekDialogueInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.param.blog.po.deepseek.DialogueInfoPO;
import com.xu.home.param.blog.vo.ds.CompletionHistoryVO;
import com.xu.home.param.blog.vo.ds.CompletionVO;

import java.util.List;

/**
* @author xubaolin
* @description 针对表【deepseek_dialogue_info(Dee-seek对话信息)】的数据库操作Service
* @createDate 2025-03-10 17:28:24
*/
public interface DeepseekDialogueInfoService extends IService<DeepseekDialogueInfo> {
    /**
     * 保存对话信息
     * @param dialogueInfoPO
     * @return
     */
    Long saveDialogueInfo(DialogueInfoPO dialogueInfoPO);

    /**
     * 根据对话id获取每次对话信息
     * @param dialogueId
     * @param account 用户账号
     * @return
     */
    List<CompletionVO> getDialogueInfo(Long dialogueId, String account);

    /**
     * 获取对话历史列表
     * @param account 用户账号
     * @return
     */
    List<CompletionHistoryVO> getCompletionHistoryList(String account);

    /**
     * 删除对话
     * @param dialogueId 对话ID
     * @param account 用户账号
     * @return 是否删除成功
     */
    boolean deleteDialogue(Long dialogueId, String account);

    /**
     * 统计用户对话数量
     * @param account 用户账号
     * @return 对话数量
     */
    int countDialogueByAccount(String account);

    /**
     * 检查用户是否为管理员
     * @param account 用户账号
     * @return 是否是管理员
     */
    boolean isAdmin(String account);
}
