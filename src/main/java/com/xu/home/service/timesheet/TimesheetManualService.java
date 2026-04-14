package com.xu.home.service.timesheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.timesheet.TimesheetManualWorklog;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import com.xu.home.mapper.timesheet.TimesheetManualWorklogMapper;
import com.xu.home.param.timesheet.ManualWorklogItemRequest;
import com.xu.home.param.timesheet.ManualWorklogSaveRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人工记工业务服务
 */
@Service
public class TimesheetManualService {

    private final TimesheetManualWorklogMapper manualWorklogMapper;
    private final TimesheetAccessService accessService;

    public TimesheetManualService(TimesheetManualWorklogMapper manualWorklogMapper, TimesheetAccessService accessService) {
        this.manualWorklogMapper = manualWorklogMapper;
        this.accessService = accessService;
    }

    /**
     * 保存某天的人工记工结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWorklog(ManualWorklogSaveRequest request, String account, String userName) {
        TimesheetProject project = accessService.requireProject(request.getProjectId());
        accessService.requireCreator(project, account);
        accessService.requireEditable(project);
        if (!Integer.valueOf(2).equals(project.getMode())) {
            throw new IllegalArgumentException("当前项目不是人工记工模式");
        }
        if (request.getWorkDate() == null) {
            throw new IllegalArgumentException("记工日期不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("记工明细不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ManualWorklogItemRequest item : request.getItems()) {
            TimesheetProjectMember member = accessService.requireMemberById(request.getProjectId(), item.getMemberId());
            validateWorkUnits(item.getWorkUnits());
            TimesheetManualWorklog existing = manualWorklogMapper.selectOne(new LambdaQueryWrapper<TimesheetManualWorklog>()
                    .eq(TimesheetManualWorklog::getProjectId, request.getProjectId())
                    .eq(TimesheetManualWorklog::getMemberId, item.getMemberId())
                    .eq(TimesheetManualWorklog::getWorkDate, request.getWorkDate())
                    .last("limit 1"));
            if (existing == null) {
                existing = new TimesheetManualWorklog();
                existing.setProjectId(request.getProjectId());
                existing.setMemberId(member.getId());
                existing.setMemberAccount(member.getMemberAccount());
                existing.setMemberName(member.getMemberName());
                existing.setWorkDate(request.getWorkDate());
                existing.setCreateTime(now);
                existing.setIsDelete(0);
            }
            existing.setWorkUnits(item.getWorkUnits());
            existing.setRemark(item.getRemark());
            existing.setMaintainedBy(account);
            existing.setMaintainedByName(userName);
            existing.setUpdateTime(now);
            if (existing.getId() == null) {
                manualWorklogMapper.insert(existing);
            } else {
                manualWorklogMapper.updateById(existing);
            }
        }
        return true;
    }

    /**
     * 查询人工记工记录
     */
    public List<TimesheetManualWorklog> listWorklog(Long projectId, String account) {
        TimesheetProject project = accessService.requireProject(projectId);
        if (Integer.valueOf(2).equals(project.getMode())) {
            accessService.requireMember(projectId, account);
        }
        return manualWorklogMapper.selectList(new LambdaQueryWrapper<TimesheetManualWorklog>()
                .eq(TimesheetManualWorklog::getProjectId, projectId)
                .eq(TimesheetManualWorklog::getIsDelete, 0)
                .orderByDesc(TimesheetManualWorklog::getWorkDate)
                .orderByAsc(TimesheetManualWorklog::getMemberName));
    }

    /**
     * 人工记工仅允许 0、0.5、1 三种工数
     */
    private void validateWorkUnits(BigDecimal workUnits) {
        if (workUnits == null) {
            throw new IllegalArgumentException("工数不能为空");
        }
        if (workUnits.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("工数不能小于0");
        }
        if (!BigDecimal.ZERO.equals(workUnits)
                && !BigDecimal.valueOf(0.5).equals(workUnits)
                && !BigDecimal.ONE.equals(workUnits)) {
            throw new IllegalArgumentException("人工记工只支持0、0.5、1工");
        }
    }
}
