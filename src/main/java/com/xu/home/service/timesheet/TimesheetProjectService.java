package com.xu.home.service.timesheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import com.xu.home.mapper.timesheet.TimesheetAttendanceRecordMapper;
import com.xu.home.mapper.timesheet.TimesheetManualWorklogMapper;
import com.xu.home.mapper.timesheet.TimesheetProjectMapper;
import com.xu.home.mapper.timesheet.TimesheetProjectMemberMapper;
import com.xu.home.param.timesheet.AddProjectMemberRequest;
import com.xu.home.param.timesheet.CreateProjectRequest;
import com.xu.home.param.timesheet.SyncProjectMembersRequest;
import com.xu.home.param.timesheet.vo.ProjectDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 项目与成员管理服务
 */
@Service
public class TimesheetProjectService {

    private final TimesheetProjectMapper projectMapper;
    private final TimesheetProjectMemberMapper memberMapper;
    private final TimesheetAttendanceRecordMapper attendanceRecordMapper;
    private final TimesheetManualWorklogMapper manualWorklogMapper;
    private final TimesheetAccessService accessService;

    public TimesheetProjectService(TimesheetProjectMapper projectMapper,
                                   TimesheetProjectMemberMapper memberMapper,
                                   TimesheetAttendanceRecordMapper attendanceRecordMapper,
                                   TimesheetManualWorklogMapper manualWorklogMapper,
                                   TimesheetAccessService accessService) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.manualWorklogMapper = manualWorklogMapper;
        this.accessService = accessService;
    }

    /**
     * 创建项目并自动将创建者加入成员列表
     */
    @Transactional(rollbackFor = Exception.class)
    public TimesheetProject createProject(CreateProjectRequest request, String account, String userName, String phone) {
        if (request.getProjectName() == null || request.getProjectName().isBlank()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (request.getMode() == null || (request.getMode() != 1 && request.getMode() != 2)) {
            throw new IllegalArgumentException("项目记录模式不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        TimesheetProject project = new TimesheetProject();
        project.setProjectName(request.getProjectName().trim());
        project.setMode(request.getMode());
        project.setStatus(1);
        project.setCreatorAccount(account);
        project.setCreatorName(userName);
        project.setRemark(request.getRemark());
        project.setCreateTime(now);
        project.setUpdateTime(now);
        project.setIsDelete(0);
        projectMapper.insert(project);

        TimesheetProjectMember creatorMember = new TimesheetProjectMember();
        creatorMember.setProjectId(project.getId());
        creatorMember.setMemberAccount(account);
        creatorMember.setMemberName(userName);
        creatorMember.setMemberPhone(phone);
        creatorMember.setJoinTime(now);
        creatorMember.setCreateTime(now);
        creatorMember.setUpdateTime(now);
        creatorMember.setIsDelete(0);
        memberMapper.insert(creatorMember);
        return project;
    }

    /**
     * 查询当前用户参与的全部项目
     */
    public List<TimesheetProject> listMyProjects(String account) {
        List<TimesheetProjectMember> joinedProjects = memberMapper.selectList(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getMemberAccount, account)
                .eq(TimesheetProjectMember::getIsDelete, 0));
        List<Long> projectIds = joinedProjects.stream()
                .map(TimesheetProjectMember::getProjectId)
                .distinct()
                .toList();

        LambdaQueryWrapper<TimesheetProject> wrapper = new LambdaQueryWrapper<TimesheetProject>()
                .eq(TimesheetProject::getIsDelete, 0)
                .orderByDesc(TimesheetProject::getCreateTime);
        if (projectIds.isEmpty()) {
            wrapper.eq(TimesheetProject::getCreatorAccount, account);
        } else {
            wrapper.and(query -> query.eq(TimesheetProject::getCreatorAccount, account)
                    .or()
                    .in(TimesheetProject::getId, projectIds));
        }
        return projectMapper.selectList(wrapper);
    }

    /**
     * 查询项目详情及成员列表
     */
    public ProjectDetailVO getDetail(Long projectId, String account) {
        TimesheetProject project = accessService.requireProject(projectId);
        accessService.requireMember(projectId, account);
        List<TimesheetProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, projectId)
                .eq(TimesheetProjectMember::getIsDelete, 0)
                .orderByAsc(TimesheetProjectMember::getJoinTime));
        ProjectDetailVO vo = new ProjectDetailVO();
        vo.setProject(project);
        vo.setMembers(members);
        return vo;
    }

    /**
     * 项目创建者新增成员
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addMember(AddProjectMemberRequest request, String currentAccount) {
        TimesheetProject project = accessService.requireProject(request.getProjectId());
        accessService.requireCreator(project, currentAccount);
        accessService.requireEditable(project);
        if (request.getMemberName() == null || request.getMemberName().isBlank()) {
            throw new IllegalArgumentException("成员姓名不能为空");
        }
        if (request.getMemberPhone() == null || request.getMemberPhone().isBlank()) {
            throw new IllegalArgumentException("成员手机号不能为空");
        }
        if (Integer.valueOf(1).equals(project.getMode())
                && (request.getMemberAccount() == null || request.getMemberAccount().isBlank())) {
            throw new IllegalArgumentException("签到记工模式下成员账号不能为空");
        }

        LambdaQueryWrapper<TimesheetProjectMember> existingWrapper = new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, request.getProjectId())
                .eq(TimesheetProjectMember::getMemberPhone, request.getMemberPhone())
                .last("limit 1");
        TimesheetProjectMember existing = memberMapper.selectOne(existingWrapper);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setMemberAccount(request.getMemberAccount());
            existing.setMemberName(request.getMemberName());
            existing.setMemberPhone(request.getMemberPhone());
            existing.setIsDelete(0);
            existing.setUpdateTime(now);
            return memberMapper.updateById(existing) > 0;
        }

        TimesheetProjectMember member = new TimesheetProjectMember();
        member.setProjectId(request.getProjectId());
        member.setMemberAccount(request.getMemberAccount());
        member.setMemberName(request.getMemberName());
        member.setMemberPhone(request.getMemberPhone());
        member.setJoinTime(now);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        member.setIsDelete(0);
        return memberMapper.insert(member) > 0;
    }

    /**
     * 删除未产生记工记录的成员
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMember(Long projectId, Long memberId, String currentAccount) {
        TimesheetProject project = accessService.requireProject(projectId);
        accessService.requireCreator(project, currentAccount);
        accessService.requireEditable(project);
        TimesheetProjectMember member = accessService.requireMemberById(projectId, memberId);
        if (currentAccount.equals(member.getMemberAccount())) {
            throw new IllegalArgumentException("不能移除项目创建者自己");
        }
        if (hasMemberWorkRecords(projectId, memberId)) {
            throw new IllegalArgumentException("该成员已有记工记录，不能删除");
        }
        member.setIsDelete(1);
        member.setUpdateTime(LocalDateTime.now());
        return memberMapper.updateById(member) > 0;
    }

    /**
     * 同步同一创建者历史项目的成员到当前项目，只补充差集
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncMembersFromProject(SyncProjectMembersRequest request, String currentAccount) {
        if (request.getTargetProjectId() == null || request.getSourceProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        if (Objects.equals(request.getTargetProjectId(), request.getSourceProjectId())) {
            throw new IllegalArgumentException("不能从当前项目同步成员");
        }

        TimesheetProject targetProject = accessService.requireProject(request.getTargetProjectId());
        accessService.requireCreator(targetProject, currentAccount);
        accessService.requireEditable(targetProject);

        TimesheetProject sourceProject = accessService.requireProject(request.getSourceProjectId());
        accessService.requireCreator(sourceProject, currentAccount);

        List<TimesheetProjectMember> targetMembers = memberMapper.selectList(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, request.getTargetProjectId())
                .eq(TimesheetProjectMember::getIsDelete, 0));
        Set<String> existingPhones = new HashSet<>();
        for (TimesheetProjectMember member : targetMembers) {
            if (member.getMemberPhone() != null && !member.getMemberPhone().isBlank()) {
                existingPhones.add(member.getMemberPhone());
            }
        }

        List<TimesheetProjectMember> sourceMembers = memberMapper.selectList(new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, request.getSourceProjectId())
                .eq(TimesheetProjectMember::getIsDelete, 0)
                .orderByAsc(TimesheetProjectMember::getJoinTime));

        int addedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TimesheetProjectMember sourceMember : sourceMembers) {
            String phone = sourceMember.getMemberPhone();
            if (phone == null || phone.isBlank() || existingPhones.contains(phone)) {
                continue;
            }

            TimesheetProjectMember member = new TimesheetProjectMember();
            member.setProjectId(request.getTargetProjectId());
            member.setMemberAccount(sourceMember.getMemberAccount());
            member.setMemberName(sourceMember.getMemberName());
            member.setMemberPhone(phone);
            member.setJoinTime(now);
            member.setCreateTime(now);
            member.setUpdateTime(now);
            member.setIsDelete(0);
            memberMapper.insert(member);
            existingPhones.add(phone);
            addedCount++;
        }
        return addedCount;
    }

    /**
     * 删除已结束项目
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProject(Long projectId, String currentAccount) {
        TimesheetProject project = accessService.requireProject(projectId);
        accessService.requireCreator(project, currentAccount);
        if (!Integer.valueOf(2).equals(project.getStatus())) {
            throw new IllegalArgumentException("只有已结束的项目才允许删除");
        }
        project.setIsDelete(1);
        project.setUpdateTime(LocalDateTime.now());
        return projectMapper.updateById(project) > 0;
    }

    /**
     * 判断成员是否已有签到或人工记工记录
     */
    private boolean hasMemberWorkRecords(Long projectId, Long memberId) {
        Long attendanceCount = attendanceRecordMapper.selectCount(new LambdaQueryWrapper<com.xu.home.domain.timesheet.TimesheetAttendanceRecord>()
                .eq(com.xu.home.domain.timesheet.TimesheetAttendanceRecord::getProjectId, projectId)
                .eq(com.xu.home.domain.timesheet.TimesheetAttendanceRecord::getMemberId, memberId)
                .eq(com.xu.home.domain.timesheet.TimesheetAttendanceRecord::getIsDelete, 0));
        if (attendanceCount != null && attendanceCount > 0) {
            return true;
        }
        Long manualCount = manualWorklogMapper.selectCount(new LambdaQueryWrapper<com.xu.home.domain.timesheet.TimesheetManualWorklog>()
                .eq(com.xu.home.domain.timesheet.TimesheetManualWorklog::getProjectId, projectId)
                .eq(com.xu.home.domain.timesheet.TimesheetManualWorklog::getMemberId, memberId)
                .eq(com.xu.home.domain.timesheet.TimesheetManualWorklog::getIsDelete, 0));
        return manualCount != null && manualCount > 0;
    }
}
