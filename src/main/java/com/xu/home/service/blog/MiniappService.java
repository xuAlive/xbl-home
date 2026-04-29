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

    /**
     * 校验小程序 ID 是否仍处于上架状态。
     */
    boolean isAvailable(Integer id);

    /**
     * 校验前端路由对应的小程序是否仍处于上架状态。
     */
    boolean isRouteAvailable(String route);

    boolean offline(Integer id);

    boolean online(Integer id);
}
