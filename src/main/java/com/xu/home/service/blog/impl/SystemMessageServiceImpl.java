package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.SystemMessage;
import com.xu.home.mapper.blog.SystemMessageMapper;
import com.xu.home.param.blog.po.sys.SystemMessagePO;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.blog.SystemMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemMessageServiceImpl extends ServiceImpl<SystemMessageMapper, SystemMessage> implements SystemMessageService {

    @Override
    public List<SystemMessage> listActiveMessages() {
        return lambdaQuery()
                .eq(SystemMessage::getStatus, 1)
                .orderByDesc(SystemMessage::getCreateTime)
                .list();
    }

    @Override
    public SystemMessage getLatestActiveMessage() {
        return lambdaQuery()
                .eq(SystemMessage::getStatus, 1)
                .orderByDesc(SystemMessage::getCreateTime)
                .last("limit 1")
                .one();
    }

    @Override
    public Response createMessage(String account, SystemMessagePO po) {
        if (po == null || StringUtils.isBlank(po.getTitle()) || StringUtils.isBlank(po.getContent())) {
            return Response.error("标题和内容不能为空");
        }

        SystemMessage message = new SystemMessage();
        message.setTitle(StringUtils.trim(po.getTitle()));
        message.setContent(StringUtils.trim(po.getContent()));
        message.setCreatorAccount(account);
        message.setStatus(1);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        return save(message) ? Response.success(message) : Response.error("系统消息发布失败");
    }
}
