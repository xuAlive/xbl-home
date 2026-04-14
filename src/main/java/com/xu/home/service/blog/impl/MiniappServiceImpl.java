package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.Miniapp;
import com.xu.home.mapper.blog.MiniappMapper;
import com.xu.home.service.blog.MiniappService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 小程序Service实现
 */
@Service
public class MiniappServiceImpl extends ServiceImpl<MiniappMapper, Miniapp> implements MiniappService {

    @Override
    public List<Miniapp> getValidList() {
        QueryWrapper<Miniapp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0)
                .orderByAsc("sort_order");
        return this.list(queryWrapper);
    }

    @Override
    public List<Miniapp> getManageList() {
        QueryWrapper<Miniapp> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("is_delete")
                .orderByAsc("sort_order");
        return this.list(queryWrapper);
    }

    @Override
    public boolean offline(Integer id) {
        Miniapp miniapp = this.getById(id);
        if (miniapp == null || (miniapp.getIsDelete() != null && miniapp.getIsDelete() == 1)) {
            throw new RuntimeException("小程序不存在");
        }
        miniapp.setIsDelete(1);
        miniapp.setUpdateTime(new Date());
        return this.updateById(miniapp);
    }

    @Override
    public boolean online(Integer id) {
        Miniapp miniapp = this.getById(id);
        if (miniapp == null) {
            throw new RuntimeException("小程序不存在");
        }
        miniapp.setIsDelete(0);
        miniapp.setUpdateTime(new Date());
        return this.updateById(miniapp);
    }
}
