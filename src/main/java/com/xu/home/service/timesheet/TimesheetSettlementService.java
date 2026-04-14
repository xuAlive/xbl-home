package com.xu.home.service.timesheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.timesheet.TimesheetAttendanceRecord;
import com.xu.home.domain.timesheet.TimesheetManualWorklog;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import com.xu.home.domain.timesheet.TimesheetSettlement;
import com.xu.home.domain.timesheet.TimesheetSettlementItem;
import com.xu.home.mapper.timesheet.TimesheetAttendanceRecordMapper;
import com.xu.home.mapper.timesheet.TimesheetManualWorklogMapper;
import com.xu.home.mapper.timesheet.TimesheetProjectMemberMapper;
import com.xu.home.mapper.timesheet.TimesheetProjectMapper;
import com.xu.home.mapper.timesheet.TimesheetSettlementItemMapper;
import com.xu.home.mapper.timesheet.TimesheetSettlementMapper;
import com.xu.home.param.timesheet.SalaryCalcRequest;
import com.xu.home.param.timesheet.SettlementRequest;
import com.xu.home.param.timesheet.vo.SettlementDetailVO;
import com.xu.home.param.timesheet.vo.SettlementMemberSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工时结算业务服务
 */
@Service
public class TimesheetSettlementService {

    private final TimesheetProjectMapper projectMapper;
    private final TimesheetProjectMemberMapper memberMapper;
    private final TimesheetAttendanceRecordMapper attendanceRecordMapper;
    private final TimesheetManualWorklogMapper manualWorklogMapper;
    private final TimesheetSettlementMapper settlementMapper;
    private final TimesheetSettlementItemMapper settlementItemMapper;
    private final TimesheetAccessService accessService;

    public TimesheetSettlementService(TimesheetProjectMapper projectMapper,
                                      TimesheetProjectMemberMapper memberMapper,
                                      TimesheetAttendanceRecordMapper attendanceRecordMapper,
                                      TimesheetManualWorklogMapper manualWorklogMapper,
                                      TimesheetSettlementMapper settlementMapper,
                                      TimesheetSettlementItemMapper settlementItemMapper,
                                      TimesheetAccessService accessService) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.manualWorklogMapper = manualWorklogMapper;
        this.settlementMapper = settlementMapper;
        this.settlementItemMapper = settlementItemMapper;
        this.accessService = accessService;
    }

    /**
     * 创建结算单，并可按需结束项目
     */
    @Transactional(rollbackFor = Exception.class)
    public SettlementDetailVO settle(SettlementRequest request, String account, String userName, boolean closeProject) {
        TimesheetProject project = accessService.requireProject(request.getProjectId());
        accessService.requireCreator(project, account);
        accessService.requireEditable(project);

        List<TimesheetProjectMember> members = resolveMembers(project.getId(), request.getMemberIds());
        if (members.isEmpty()) {
            throw new IllegalArgumentException("没有可结算的项目成员");
        }

        TimesheetSettlement settlement = new TimesheetSettlement();
        settlement.setProjectId(project.getId());
        settlement.setSettlementNo(generateSettlementNo(project.getId()));
        settlement.setSettlementType(closeProject ? 3 : (members.size() > 1 ? 2 : 1));
        settlement.setSettledBy(account);
        settlement.setSettledByName(userName);
        settlement.setUnitSalary(request.getUnitSalary());
        settlement.setCreateTime(LocalDateTime.now());

        List<TimesheetSettlementItem> items = buildSettlementItems(project, members, request.getUnitSalary());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("没有可结算的工时记录");
        }

        settlement.setItemCount(items.size());
        settlement.setTotalWorkHours(sumHours(items));
        settlement.setTotalWorkUnits(sumUnits(items));
        settlement.setTotalSalary(sumSalary(items));
        settlementMapper.insert(settlement);

        for (TimesheetSettlementItem item : items) {
            item.setSettlementId(settlement.getId());
            settlementItemMapper.insert(item);
        }

        if (closeProject) {
            project.setStatus(2);
            project.setFinishedTime(LocalDateTime.now());
            project.setUpdateTime(LocalDateTime.now());
            projectMapper.updateById(project);
        }

        return buildDetail(settlement.getId());
    }

    /**
     * 组装结算详情
     */
    public SettlementDetailVO buildDetail(Long settlementId) {
        TimesheetSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("结算单不存在");
        }
        List<TimesheetSettlementItem> items = settlementItemMapper.selectList(new LambdaQueryWrapper<TimesheetSettlementItem>()
                .eq(TimesheetSettlementItem::getSettlementId, settlementId)
                .orderByAsc(TimesheetSettlementItem::getMemberName)
                .orderByAsc(TimesheetSettlementItem::getWorkDate));
        SettlementDetailVO vo = new SettlementDetailVO();
        vo.setSettlement(settlement);
        vo.setItems(items);
        vo.setMemberSummaries(buildMemberSummaries(items));
        return vo;
    }

    /**
     * 查询项目的历史结算记录
     */
    public List<TimesheetSettlement> listProjectSettlements(Long projectId, String account) {
        accessService.requireMember(projectId, account);
        return settlementMapper.selectList(new LambdaQueryWrapper<TimesheetSettlement>()
                .eq(TimesheetSettlement::getProjectId, projectId)
                .orderByDesc(TimesheetSettlement::getCreateTime));
    }

    /**
     * 按每工薪资重算结算金额
     */
    @Transactional(rollbackFor = Exception.class)
    public SettlementDetailVO calculateSalary(SalaryCalcRequest request) {
        TimesheetSettlement settlement = settlementMapper.selectById(request.getSettlementId());
        if (settlement == null) {
            throw new IllegalArgumentException("结算单不存在");
        }
        if (request.getUnitSalary() == null || request.getUnitSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("每工薪资不能为空且不能小于0");
        }

        List<TimesheetSettlementItem> items = settlementItemMapper.selectList(new LambdaQueryWrapper<TimesheetSettlementItem>()
                .eq(TimesheetSettlementItem::getSettlementId, request.getSettlementId()));
        for (TimesheetSettlementItem item : items) {
            item.setUnitSalary(request.getUnitSalary());
            item.setSalaryAmount(item.getWorkUnits().multiply(request.getUnitSalary()).setScale(2, RoundingMode.HALF_UP));
            settlementItemMapper.updateById(item);
        }
        settlement.setUnitSalary(request.getUnitSalary());
        settlement.setTotalSalary(sumSalary(items));
        settlementMapper.updateById(settlement);
        return buildDetail(settlement.getId());
    }

    /**
     * 根据前端传入的成员范围解析待结算成员
     */
    private List<TimesheetProjectMember> resolveMembers(Long projectId, List<Long> memberIds) {
        LambdaQueryWrapper<TimesheetProjectMember> wrapper = new LambdaQueryWrapper<TimesheetProjectMember>()
                .eq(TimesheetProjectMember::getProjectId, projectId)
                .eq(TimesheetProjectMember::getIsDelete, 0)
                .orderByAsc(TimesheetProjectMember::getJoinTime);
        if (memberIds != null && !memberIds.isEmpty()) {
            wrapper.in(TimesheetProjectMember::getId, memberIds);
        }
        return memberMapper.selectList(wrapper);
    }

    /**
     * 按项目模式构建结算明细
     */
    private List<TimesheetSettlementItem> buildSettlementItems(TimesheetProject project,
                                                               List<TimesheetProjectMember> members,
                                                               BigDecimal unitSalary) {
        List<TimesheetSettlementItem> items = new ArrayList<>();
        List<Long> memberIds = members.stream().map(TimesheetProjectMember::getId).toList();
        if (Integer.valueOf(1).equals(project.getMode())) {
            List<TimesheetAttendanceRecord> records = attendanceRecordMapper.selectList(new LambdaQueryWrapper<TimesheetAttendanceRecord>()
                    .eq(TimesheetAttendanceRecord::getProjectId, project.getId())
                    .eq(TimesheetAttendanceRecord::getIsDelete, 0)
                    .in(TimesheetAttendanceRecord::getMemberId, memberIds)
                    .isNotNull(TimesheetAttendanceRecord::getSignInTime)
                    .isNotNull(TimesheetAttendanceRecord::getSignOutTime)
                    .orderByAsc(TimesheetAttendanceRecord::getMemberName)
                    .orderByAsc(TimesheetAttendanceRecord::getWorkDate));
            for (TimesheetAttendanceRecord record : records) {
                items.add(createItem(project.getMode(), record.getId(), project.getId(), record.getMemberId(),
                        record.getMemberAccount(), record.getMemberName(), record.getWorkDate(), record.getWorkHours(),
                        record.getWorkUnits(), unitSalary, record.getRemark()));
            }
        } else {
            List<TimesheetManualWorklog> worklogs = manualWorklogMapper.selectList(new LambdaQueryWrapper<TimesheetManualWorklog>()
                    .eq(TimesheetManualWorklog::getProjectId, project.getId())
                    .eq(TimesheetManualWorklog::getIsDelete, 0)
                    .in(TimesheetManualWorklog::getMemberId, memberIds)
                    .orderByAsc(TimesheetManualWorklog::getMemberName)
                    .orderByAsc(TimesheetManualWorklog::getWorkDate));
            for (TimesheetManualWorklog worklog : worklogs) {
                items.add(createItem(project.getMode(), worklog.getId(), project.getId(), worklog.getMemberId(),
                        worklog.getMemberAccount(), worklog.getMemberName(), worklog.getWorkDate(), BigDecimal.ZERO,
                        worklog.getWorkUnits(), unitSalary, worklog.getRemark()));
            }
        }
        return items;
    }

    /**
     * 创建单条结算明细
     */
    private TimesheetSettlementItem createItem(Integer sourceMode,
                                               Long sourceRecordId,
                                               Long projectId,
                                               Long memberId,
                                               String memberAccount,
                                               String memberName,
                                               java.time.LocalDate workDate,
                                               BigDecimal workHours,
                                               BigDecimal workUnits,
                                               BigDecimal unitSalary,
                                               String remark) {
        TimesheetSettlementItem item = new TimesheetSettlementItem();
        item.setProjectId(projectId);
        item.setMemberId(memberId);
        item.setMemberAccount(memberAccount);
        item.setMemberName(memberName);
        item.setWorkDate(workDate);
        item.setSourceMode(sourceMode);
        item.setSourceRecordId(sourceRecordId);
        item.setWorkHours(workHours == null ? BigDecimal.ZERO : workHours);
        item.setWorkUnits(workUnits == null ? BigDecimal.ZERO : workUnits);
        item.setUnitSalary(unitSalary);
        item.setSalaryAmount(unitSalary == null ? null : item.getWorkUnits().multiply(unitSalary).setScale(2, RoundingMode.HALF_UP));
        item.setRemark(remark);
        item.setCreateTime(LocalDateTime.now());
        return item;
    }

    /**
     * 汇总成员维度的工时、工数和薪资
     */
    private List<SettlementMemberSummaryVO> buildMemberSummaries(List<TimesheetSettlementItem> items) {
        Map<Long, SettlementMemberSummaryVO> summaryMap = new LinkedHashMap<>();
        for (TimesheetSettlementItem item : items) {
            SettlementMemberSummaryVO summary = summaryMap.computeIfAbsent(item.getMemberId(), key -> {
                SettlementMemberSummaryVO target = new SettlementMemberSummaryVO();
                target.setMemberId(item.getMemberId());
                target.setMemberAccount(item.getMemberAccount());
                target.setMemberName(item.getMemberName());
                target.setTotalWorkHours(BigDecimal.ZERO);
                target.setTotalWorkUnits(BigDecimal.ZERO);
                target.setTotalSalary(BigDecimal.ZERO);
                return target;
            });
            summary.setTotalWorkHours(summary.getTotalWorkHours().add(item.getWorkHours() == null ? BigDecimal.ZERO : item.getWorkHours()));
            summary.setTotalWorkUnits(summary.getTotalWorkUnits().add(item.getWorkUnits() == null ? BigDecimal.ZERO : item.getWorkUnits()));
            if (item.getSalaryAmount() != null) {
                summary.setUnitSalary(item.getUnitSalary());
                summary.setTotalSalary(summary.getTotalSalary().add(item.getSalaryAmount()));
            }
        }
        return new ArrayList<>(summaryMap.values());
    }

    /**
     * 生成结算单号
     */
    private String generateSettlementNo(Long projectId) {
        return "TS" + projectId + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 汇总结算总工时
     */
    private BigDecimal sumHours(List<TimesheetSettlementItem> items) {
        return items.stream()
                .map(item -> item.getWorkHours() == null ? BigDecimal.ZERO : item.getWorkHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总结算总工数
     */
    private BigDecimal sumUnits(List<TimesheetSettlementItem> items) {
        return items.stream()
                .map(item -> item.getWorkUnits() == null ? BigDecimal.ZERO : item.getWorkUnits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总结算总薪资
     */
    private BigDecimal sumSalary(List<TimesheetSettlementItem> items) {
        boolean hasSalary = items.stream().anyMatch(item -> item.getSalaryAmount() != null);
        if (!hasSalary) {
            return null;
        }
        return items.stream()
                .map(item -> item.getSalaryAmount() == null ? BigDecimal.ZERO : item.getSalaryAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
