package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.Miniapp;

import java.util.List;

/**
 * 小程序Service
 */
public interface MiniappService extends IService<Miniapp> {

    /**
     * 获取所有有效的小程序列表
     */
    List<Miniapp> getValidList();

    List<Miniapp> getManageList();

    boolean offline(Integer id);

    boolean online(Integer id);
}
