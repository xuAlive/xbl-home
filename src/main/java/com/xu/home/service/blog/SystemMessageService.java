package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.SystemMessage;
import com.xu.home.param.blog.po.sys.SystemMessagePO;
import com.xu.home.param.common.response.Response;

import java.util.List;

public interface SystemMessageService extends IService<SystemMessage> {

    List<SystemMessage> listActiveMessages();

    SystemMessage getLatestActiveMessage();

    Response createMessage(String account, SystemMessagePO po);
}
