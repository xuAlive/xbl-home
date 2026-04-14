package com.xu.home.service.timesheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import com.xu.home.mapper.timesheet.TimesheetProjectMapper;
import com.xu.home.mapper.timesheet.TimesheetProjectMemberMapper;
import org.springframework.stereotype.Service;

/**
 * 项目访问权限校验服务
 */
@Service
public class TimesheetAccessService {

    private final TimesheetProjectMapper projectMapper;
    private final TimesheetProjectMemberMapper memberMapper;

    public TimesheetAccessService(TimesheetProjectMapper projectMapper, TimesheetProjectMemberMapper memberMapper) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
    }

    /**
     * 校验项目存在且未删除
     */
    public TimesheetProject requireProject(Long projectId) {
        TimesheetProject project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getIsDelete())) {
            throw new IllegalArgumentException("项目不存在");
        }
        return project;
    }

    /**
     * 校验项目仍处于可编辑状态
     */
    public void requireEditable(TimesheetProject project) {
        if (!Integer.valueOf(1).equals(project.getStatus())) {
            throw new IllegalArgumentException("项目已结束，不能再编辑");
        }
    }

    /**
     * 校验当前用户为项目创建者
     */
    public void requireCreator(TimesheetProject project, String account) {
        if (account == null || !account.equals(project.getCreatorAccount())) {
            throw new IllegalArgumentException("只有项目创建者可以执行该操作");
        }
    }

    /**
     * 按账号校验项目成员身份
     */
    public TimesheetProjectMember requireMember(Long projectId, String account) {
        TimesheetProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, projectId)
                .eq(TimesheetProjectMember::getMemberAccount, account)
                .eq(TimesheetProjectMember::getIsDelete, 0)
                .last("limit 1"));
        if (member == null) {
            throw new IllegalArgumentException("当前用户不是该项目成员");
        }
        return member;
    }

    /**
     * 按成员ID校验项目成员存在
     */
    public TimesheetProjectMember requireMemberById(Long projectId, Long memberId) {
        TimesheetProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, projectId)
                .eq(TimesheetProjectMember::getId, memberId)
                .eq(TimesheetProjectMember::getIsDelete, 0)
                .last("limit 1"));
        if (member == null) {
            throw new IllegalArgumentException("项目成员不存在");
        }
        return member;
    }
}
