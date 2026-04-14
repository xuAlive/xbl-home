package com.xu.home.dao.blog;

import com.xu.home.mapper.blog.DeepseekDialogueInfoMapper;
import com.xu.home.param.blog.vo.ds.CompletionHistoryVO;
import com.xu.home.param.blog.vo.ds.CompletionVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeepseekDialogueInfoDao {

    private final DeepseekDialogueInfoMapper deepseekDialogueInfoMapper;

    public DeepseekDialogueInfoDao(DeepseekDialogueInfoMapper deepseekDialogueInfoMapper) {
        this.deepseekDialogueInfoMapper = deepseekDialogueInfoMapper;
    }

    /**
     * 根据对话ID和账号查询对话详情
     * @param dialogueId 对话ID
     * @param account 用户账号
     * @return
     */
    public List<CompletionVO> selectCompletion(Long dialogueId, String account) {
        return deepseekDialogueInfoMapper.selectCompletion(dialogueId, account);
    }

    /**
     * 根据账号查询对话历史列表
     * 按dialogue_id分组，每组只获取时间最早的content，content超过10个字符截取前10个字符
     * @param account 用户账号
     * @return
     */
    public List<CompletionHistoryVO> selectCompletionHistory(String account) {
        return deepseekDialogueInfoMapper.selectCompletionHistory(account);
    }

    /**
     * 根据对话ID和账号删除对话
     * @param dialogueId 对话ID
     * @param account 用户账号
     * @return 删除的记录数
     */
    public int deleteByDialogueIdAndAccount(Long dialogueId, String account) {
        return deepseekDialogueInfoMapper.deleteByDialogueIdAndAccount(dialogueId, account);
    }

    /**
     * 统计用户对话数量
     * @param account 用户账号
     * @return 对话数量
     */
    public int countDialogueByAccount(String account) {
        return deepseekDialogueInfoMapper.countDialogueByAccount(account);
    }
}
