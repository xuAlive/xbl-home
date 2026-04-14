package com.xu.home.service.timesheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.timesheet.TimesheetAttendanceRecord;
import com.xu.home.domain.timesheet.TimesheetMakeupRequest;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.domain.timesheet.TimesheetProjectMember;
import com.xu.home.mapper.timesheet.TimesheetAttendanceRecordMapper;
import com.xu.home.mapper.timesheet.TimesheetMakeupRequestMapper;
import com.xu.home.param.timesheet.ApproveMakeupRequest;
import com.xu.home.param.timesheet.AttendanceSignRequest;
import com.xu.home.param.timesheet.MakeupRequestCreate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 签到记工业务服务
 */
@Service
public class TimesheetAttendanceService {

    private final TimesheetAttendanceRecordMapper attendanceRecordMapper;
    private final TimesheetMakeupRequestMapper makeupRequestMapper;
    private final TimesheetAccessService accessService;

    public TimesheetAttendanceService(TimesheetAttendanceRecordMapper attendanceRecordMapper,
                                      TimesheetMakeupRequestMapper makeupRequestMapper,
                                      TimesheetAccessService accessService) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.makeupRequestMapper = makeupRequestMapper;
        this.accessService = accessService;
    }

    /**
     * 执行签到或离班签到
     */
    @Transactional(rollbackFor = Exception.class)
    public TimesheetAttendanceRecord sign(AttendanceSignRequest request, String account) {
        TimesheetProject project = accessService.requireProject(request.getProjectId());
        accessService.requireEditable(project);
        if (!Integer.valueOf(1).equals(project.getMode())) {
            throw new IllegalArgumentException("当前项目不是签到记工模式");
        }
        TimesheetProjectMember member = accessService.requireMember(project.getId(), account);
        LocalDateTime signTime = request.getSignTime() != null ? request.getSignTime() : LocalDateTime.now();
        LocalDate workDate = signTime.toLocalDate();

        TimesheetAttendanceRecord record = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<TimesheetAttendanceRecord>()
                .eq(TimesheetAttendanceRecord::getProjectId, request.getProjectId())
                .eq(TimesheetAttendanceRecord::getMemberId, member.getId())
                .eq(TimesheetAttendanceRecord::getWorkDate, workDate)
                .eq(TimesheetAttendanceRecord::getIsDelete, 0)
                .last("limit 1"));

        if (request.getSignType() == null || (request.getSignType() != 1 && request.getSignType() != 2)) {
            throw new IllegalArgumentException("签到类型不正确");
        }

        if (request.getSignType() == 1) {
            if (record != null) {
                throw new IllegalArgumentException("当天已签到，不能重复签到");
            }
            record = new TimesheetAttendanceRecord();
            record.setProjectId(request.getProjectId());
            record.setMemberId(member.getId());
            record.setMemberAccount(member.getMemberAccount());
            record.setMemberName(member.getMemberName());
            record.setWorkDate(workDate);
            record.setSignInTime(signTime);
            record.setRecordStatus(1);
            record.setRemark(request.getRemark());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            record.setIsDelete(0);
            attendanceRecordMapper.insert(record);
            return record;
        }

        if (record == null || record.getSignInTime() == null) {
            throw new IllegalArgumentException("当天未签到，不能直接离班签到");
        }
        if (record.getSignOutTime() != null) {
            throw new IllegalArgumentException("当天已完成离班签到");
        }
        if (signTime.isBefore(record.getSignInTime())) {
            throw new IllegalArgumentException("离班时间不能早于签到时间");
        }

        fillAttendanceMetrics(record, record.getSignInTime(), signTime);
        record.setRecordStatus(2);
        record.setRemark(request.getRemark());
        record.setUpdateTime(LocalDateTime.now());
        attendanceRecordMapper.updateById(record);
        return record;
    }

    /**
     * 创建补签申请
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createMakeupRequest(MakeupRequestCreate request, String account, String userName) {
        TimesheetProject project = accessService.requireProject(request.getProjectId());
        accessService.requireEditable(project);
        if (!Integer.valueOf(1).equals(project.getMode())) {
            throw new IllegalArgumentException("当前项目不是签到记工模式");
        }
        TimesheetProjectMember member = accessService.requireMember(project.getId(), account);
        if (request.getWorkDate() == null || request.getSignInTime() == null || request.getSignOutTime() == null) {
            throw new IllegalArgumentException("补签时间不能为空");
        }
        if (request.getSignOutTime().isBefore(request.getSignInTime())) {
            throw new IllegalArgumentException("离班时间不能早于签到时间");
        }
        if (!request.getSignInTime().toLocalDate().equals(request.getWorkDate())
                || !request.getSignOutTime().toLocalDate().equals(request.getWorkDate())) {
            throw new IllegalArgumentException("补签时间必须与工时日期一致");
        }

        TimesheetMakeupRequest makeupRequest = new TimesheetMakeupRequest();
        makeupRequest.setProjectId(request.getProjectId());
        makeupRequest.setMemberId(member.getId());
        makeupRequest.setMemberAccount(account);
        makeupRequest.setMemberName(userName);
        makeupRequest.setWorkDate(request.getWorkDate());
        makeupRequest.setMakeupSignInTime(request.getSignInTime());
        makeupRequest.setMakeupSignOutTime(request.getSignOutTime());
        makeupRequest.setReason(request.getReason());
        makeupRequest.setApprovalStatus(1);
        makeupRequest.setCreateTime(LocalDateTime.now());
        makeupRequest.setUpdateTime(LocalDateTime.now());
        makeupRequest.setIsDelete(0);
        return makeupRequestMapper.insert(makeupRequest) > 0;
    }

    /**
     * 审批补签并在通过后回填出勤记录
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveMakeup(ApproveMakeupRequest request, String account, String userName) {
        TimesheetMakeupRequest makeupRequest = makeupRequestMapper.selectById(request.getRequestId());
        if (makeupRequest == null || Integer.valueOf(1).equals(makeupRequest.getIsDelete())) {
            throw new IllegalArgumentException("补签申请不存在");
        }
        TimesheetProject project = accessService.requireProject(makeupRequest.getProjectId());
        accessService.requireCreator(project, account);
        accessService.requireEditable(project);
        if (request.getApprovalStatus() == null || (request.getApprovalStatus() != 2 && request.getApprovalStatus() != 3)) {
            throw new IllegalArgumentException("审批状态不正确");
        }
        makeupRequest.setApprovalStatus(request.getApprovalStatus());
        makeupRequest.setApprovalRemark(request.getApprovalRemark());
        makeupRequest.setApproverAccount(account);
        makeupRequest.setApproverName(userName);
        makeupRequest.setApprovalTime(LocalDateTime.now());
        makeupRequest.setUpdateTime(LocalDateTime.now());
        makeupRequestMapper.updateById(makeupRequest);

        if (request.getApprovalStatus() == 2) {
            TimesheetAttendanceRecord record = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<TimesheetAttendanceRecord>()
                    .eq(TimesheetAttendanceRecord::getProjectId, makeupRequest.getProjectId())
                    .eq(TimesheetAttendanceRecord::getMemberId, makeupRequest.getMemberId())
                    .eq(TimesheetAttendanceRecord::getWorkDate, makeupRequest.getWorkDate())
                    .eq(TimesheetAttendanceRecord::getIsDelete, 0)
                    .last("limit 1"));
            if (record == null) {
                record = new TimesheetAttendanceRecord();
                record.setProjectId(makeupRequest.getProjectId());
                record.setMemberId(makeupRequest.getMemberId());
                record.setMemberAccount(makeupRequest.getMemberAccount());
                record.setMemberName(makeupRequest.getMemberName());
                record.setWorkDate(makeupRequest.getWorkDate());
                record.setCreateTime(LocalDateTime.now());
                record.setIsDelete(0);
            }
            fillAttendanceMetrics(record, makeupRequest.getMakeupSignInTime(), makeupRequest.getMakeupSignOutTime());
            record.setRecordStatus(3);
            record.setRemark(makeupRequest.getReason());
            record.setUpdateTime(LocalDateTime.now());
            if (record.getId() == null) {
                attendanceRecordMapper.insert(record);
            } else {
                attendanceRecordMapper.updateById(record);
            }
        }
        return true;
    }

    /**
     * 查询项目签到记录
     */
    public List<TimesheetAttendanceRecord> listAttendance(Long projectId, String account) {
        accessService.requireMember(projectId, account);
        return attendanceRecordMapper.selectList(new LambdaQueryWrapper<TimesheetAttendanceRecord>()
                .eq(TimesheetAttendanceRecord::getProjectId, projectId)
                .eq(TimesheetAttendanceRecord::getIsDelete, 0)
                .orderByDesc(TimesheetAttendanceRecord::getWorkDate)
                .orderByAsc(TimesheetAttendanceRecord::getMemberName));
    }

    /**
     * 查询补签申请记录
     */
    public List<TimesheetMakeupRequest> listMakeupRequests(Long projectId, String account) {
        accessService.requireMember(projectId, account);
        return makeupRequestMapper.selectList(new LambdaQueryWrapper<TimesheetMakeupRequest>()
                .eq(TimesheetMakeupRequest::getProjectId, projectId)
                .eq(TimesheetMakeupRequest::getIsDelete, 0)
                .orderByDesc(TimesheetMakeupRequest::getCreateTime));
    }

    /**
     * 回填签到时长和工数
     */
    private void fillAttendanceMetrics(TimesheetAttendanceRecord record, LocalDateTime signIn, LocalDateTime signOut) {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(signIn, signOut).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        record.setSignInTime(signIn);
        record.setSignOutTime(signOut);
        record.setWorkHours(hours);
        record.setWorkUnits(calculateWorkUnits(hours));
    }

    /**
     * 根据时长换算工数
     */
    public BigDecimal calculateWorkUnits(BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (hours.compareTo(BigDecimal.valueOf(4)) <= 0) {
            return BigDecimal.valueOf(0.5);
        }
        return BigDecimal.ONE;
    }
}
