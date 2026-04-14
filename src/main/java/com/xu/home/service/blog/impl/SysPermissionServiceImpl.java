package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.SysPermission;
import com.xu.home.domain.blog.SysRolePermission;
import com.xu.home.mapper.blog.SysPermissionMapper;
import com.xu.home.mapper.blog.SysRolePermissionMapper;
import com.xu.home.service.blog.SysPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限Service实现
 */
@Slf4j
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    public SysPermissionServiceImpl(SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Override
    public List<SysPermission> getPermissionsByAccount(String account) {
        return baseMapper.selectPermissionsByAccount(account);
    }

    @Override
    public boolean hasPermission(String account, String permissionCode) {
        List<SysPermission> permissions = getPermissionsByAccount(account);
        return permissions.stream()
                .anyMatch(p -> p.getPermissionCode().equals(permissionCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(Integer roleId, List<Integer> permissionIds) {
        try {
            // 先删除原有的权限分配
            QueryWrapper<SysRolePermission> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("role_id", roleId);
            sysRolePermissionMapper.delete(deleteWrapper);

            // 分配新权限
            if (permissionIds != null && !permissionIds.isEmpty()) {
                for (Integer permissionId : permissionIds) {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    sysRolePermissionMapper.insert(rolePermission);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("分配权限失败", e);
            throw new RuntimeException("分配权限失败");
        }
    }

    @Override
    public List<SysPermission> getAllValidPermissions() {
        QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0)
                .orderByAsc("id");
        return this.list(queryWrapper);
    }

    @Override
    public List<Integer> getPermissionIdsByRoleId(Integer roleId) {
        QueryWrapper<SysRolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(queryWrapper);
        return rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(java.util.stream.Collectors.toList());
    }
}
