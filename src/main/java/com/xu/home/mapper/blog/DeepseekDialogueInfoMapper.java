package com.xu.home.mapper.blog;

import com.xu.home.domain.blog.DeepseekDialogueInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.param.blog.vo.ds.CompletionHistoryVO;
import com.xu.home.param.blog.vo.ds.CompletionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author xubaolin
* @description 针对表【deepseek_dialogue_info(Dee-seek对话信息)】的数据库操作Mapper
* @createDate 2025-03-10 17:28:24
* @Entity com.xu.home.domain.blog.DeepseekDialogueInfo
*/
public interface DeepseekDialogueInfoMapper extends BaseMapper<DeepseekDialogueInfo> {

    /**
     * 根据对话ID和账号查询对话详情
     * @param dialogueId 对话ID
     * @param account 用户账号
     * @return
     */
    List<CompletionVO> selectCompletion(@Param("dialogueId") Long dialogueId, @Param("account") String account);

    /**
     * 根据账号查询对话历史列表
     * 按dialogue_id分组，每组只获取时间最早的content，content超过10个字符截取前10个字符
     * @param account 用户账号
     * @return
     */
    List<CompletionHistoryVO> selectCompletionHistory(@Param("account") String account);

    /**
     * 根据对话ID和账号删除对话
     * @param dialogueId 对话ID
     * @param account 用户账号
     * @return 删除的记录数
     */
    int deleteByDialogueIdAndAccount(@Param("dialogueId") Long dialogueId, @Param("account") String account);

    /**
     * 统计用户对话数量
     * @param account 用户账号
     * @return 对话数量
     */
    int countDialogueByAccount(@Param("account") String account);
}




