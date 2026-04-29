package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.Miniapp;
import com.xu.home.mapper.blog.MiniappMapper;
import com.xu.home.service.blog.MiniappService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小程序Service实现
 */
@Service
public class MiniappServiceImpl extends ServiceImpl<MiniappMapper, Miniapp> implements MiniappService {

    private final Map<String, Miniapp> miniappRouteCache = new ConcurrentHashMap<>();
    private final Map<Integer, Miniapp> miniappIdCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initMiniappCache() {
        refreshMiniappCache();
    }

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
    public boolean isAvailable(Integer id) {
        if (id == null) {
            return false;
        }
        Miniapp miniapp = miniappIdCache.get(id);
        return miniapp != null && (miniapp.getIsDelete() == null || miniapp.getIsDelete() == 0);
    }

    @Override
    public boolean isRouteAvailable(String route) {
        String normalizedRoute = normalizeRoute(route);
        if (StringUtils.isBlank(normalizedRoute)) {
            return false;
        }
        Miniapp miniapp = miniappRouteCache.get(normalizedRoute);
        return miniapp != null && (miniapp.getIsDelete() == null || miniapp.getIsDelete() == 0);
    }

    @Override
    public boolean offline(Integer id) {
        Miniapp miniapp = this.getById(id);
        if (miniapp == null || (miniapp.getIsDelete() != null && miniapp.getIsDelete() == 1)) {
            throw new RuntimeException("小程序不存在");
        }
        miniapp.setIsDelete(1);
        miniapp.setUpdateTime(new Date());
        boolean updated = this.updateById(miniapp);
        if (updated) {
            refreshMiniappCache();
        }
        return updated;
    }

    @Override
    public boolean online(Integer id) {
        Miniapp miniapp = this.getById(id);
        if (miniapp == null) {
            throw new RuntimeException("小程序不存在");
        }
        miniapp.setIsDelete(0);
        miniapp.setUpdateTime(new Date());
        boolean updated = this.updateById(miniapp);
        if (updated) {
            refreshMiniappCache();
        }
        return updated;
    }

    private void refreshMiniappCache() {
        List<Miniapp> miniapps = this.list();
        Map<String, Miniapp> latest = new ConcurrentHashMap<>();
        Map<Integer, Miniapp> latestById = new ConcurrentHashMap<>();
        for (Miniapp miniapp : miniapps) {
            latestById.put(miniapp.getId(), miniapp);
            String route = normalizeRoute(miniapp.getRoute());
            if (StringUtils.isNotBlank(route)) {
                latest.put(route, miniapp);
            }
        }
        miniappRouteCache.clear();
        miniappRouteCache.putAll(latest);
        miniappIdCache.clear();
        miniappIdCache.putAll(latestById);
    }

    private String normalizeRoute(String route) {
        if (StringUtils.isBlank(route)) {
            return "";
        }
        String normalized = StringUtils.substringBefore(route.trim(), "?");
        normalized = StringUtils.substringBefore(normalized, "#");
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}
