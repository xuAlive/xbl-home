package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.dao.blog.DeepseekDialogueInfoDao;
import com.xu.home.domain.blog.DeepseekDialogueInfo;
import com.xu.home.param.blog.po.deepseek.DialogueInfoPO;
import com.xu.home.param.blog.vo.ds.CompletionHistoryVO;
import com.xu.home.param.blog.vo.ds.CompletionVO;
import com.xu.home.service.blog.DeepseekDialogueInfoService;
import com.xu.home.service.blog.SysRoleService;
import com.xu.home.mapper.blog.DeepseekDialogueInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Objects;

/**
* @author xubaolin
* @description 针对表【deepseek_dialogue_info(Dee-seek对话信息)】的数据库操作Service实现
* @createDate 2025-03-10 17:28:24
*/
@Slf4j
@Service
public class DeepseekDialogueInfoServiceImpl extends ServiceImpl<DeepseekDialogueInfoMapper, DeepseekDialogueInfo> implements DeepseekDialogueInfoService{

    private final DeepseekDialogueInfoDao deepseekDialogueInfoDao;
    private final SysRoleService sysRoleService;

    public DeepseekDialogueInfoServiceImpl(DeepseekDialogueInfoDao deepseekDialogueInfoDao, SysRoleService sysRoleService) {
        this.deepseekDialogueInfoDao = deepseekDialogueInfoDao;
        this.sysRoleService = sysRoleService;
    }


    @Override
    public Long saveDialogueInfo(DialogueInfoPO dialogueInfoPO) {
        try {
            if (dialogueInfoPO != null){
                DeepseekDialogueInfo info = new DeepseekDialogueInfo();
                BeanUtils.copyProperties(dialogueInfoPO,info);
                if (Objects.isNull(dialogueInfoPO.getDialogueId())){
                    long millis = System.currentTimeMillis();
                    info.setDialogueId(millis);
                }
                // 设置用户账号
                info.setAccount(dialogueInfoPO.getAccount());
                this.save(info);
                return info.getDialogueId();
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return null;
    }

    @Override
    public List<CompletionVO> getDialogueInfo(Long dialogueId, String account) {
        List<CompletionVO> daoList = deepseekDialogueInfoDao.selectCompletion(dialogueId, account);
        return daoList;
    }

    @Override
    public List<CompletionHistoryVO> getCompletionHistoryList(String account) {
        List<CompletionHistoryVO> historyList = deepseekDialogueInfoDao.selectCompletionHistory(account);
        return historyList;
    }

    @Override
    public boolean deleteDialogue(Long dialogueId, String account) {
        try {
            int deleted = deepseekDialogueInfoDao.deleteByDialogueIdAndAccount(dialogueId, account);
            return deleted > 0;
        } catch (Exception e) {
            log.error("删除对话失败: dialogueId={}, account={}", dialogueId, account, e);
            return false;
        }
    }

    @Override
    public int countDialogueByAccount(String account) {
        return deepseekDialogueInfoDao.countDialogueByAccount(account);
    }

    @Override
    public boolean isAdmin(String account) {
        String roleCode = sysRoleService.getRoleCodeByAccount(account);
        return "ADMIN".equals(roleCode);
    }

}




