package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.SysRole;
import com.xu.home.domain.blog.SysUserRole;
import com.xu.home.mapper.blog.SysRoleMapper;
import com.xu.home.mapper.blog.SysUserRoleMapper;
import com.xu.home.service.blog.SysRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色Service实现
 */
@Slf4j
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    public SysRoleServiceImpl(SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public List<SysRole> getRolesByAccount(String account) {
        return baseMapper.selectRolesByAccount(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleToUser(String account, Integer roleId) {
        try {
            // 检查是否已存在
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("account", account).eq("role_id", roleId);
            SysUserRole existingRole = sysUserRoleMapper.selectOne(queryWrapper);

            if (existingRole != null) {
                return true;
            }

            SysUserRole userRole = new SysUserRole();
            userRole.setAccount(account);
            userRole.setRoleId(roleId);
            return sysUserRoleMapper.insert(userRole) > 0;
        } catch (Exception e) {
            log.error("分配角色失败", e);
            throw new RuntimeException("分配角色失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoleFromUser(String account, Integer roleId) {
        try {
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("account", account).eq("role_id", roleId);
            return sysUserRoleMapper.delete(queryWrapper) >= 0;
        } catch (Exception e) {
            log.error("移除角色失败", e);
            throw new RuntimeException("移除角色失败");
        }
    }

    @Override
    public String getRoleCodeByAccount(String account) {
        List<SysRole> roles = getRolesByAccount(account);
        if (roles == null || roles.isEmpty()) {
            return "GUEST";
        }
        var roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .collect(java.util.stream.Collectors.toSet());
        if (roleCodes.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roleCodes.contains("USER")) {
            return "USER";
        }
        return "GUEST";
    }

    @Override
    public SysRole selectByRoleCode(String roleCode) {
        return baseMapper.selectByRoleCode(roleCode);
    }
}
