package com.xu.home.controller.timesheet;

import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.domain.timesheet.TimesheetSettlement;
import com.xu.home.param.timesheet.SalaryCalcRequest;
import com.xu.home.param.timesheet.SettlementRequest;
import com.xu.home.service.timesheet.TimesheetSettlementService;
import com.xu.home.param.timesheet.vo.SettlementDetailVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 结算管理接口
 */
@RestController
@RequestMapping("/timesheet/settlement")
public class TimesheetSettlementController {

    private final TimesheetSettlementService settlementService;

    public TimesheetSettlementController(TimesheetSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * 执行单人或批量结算
     */
    @PostMapping("/create")
    public Response<SettlementDetailVO> create(@RequestBody SettlementRequest request) {
        return Response.success(settlementService.settle(request, currentAccount(), currentUserName(), false));
    }

    /**
     * 结算并结束项目
     */
    @PostMapping("/finishProject")
    public Response<SettlementDetailVO> finishProject(@RequestBody SettlementRequest request) {
        return Response.success(settlementService.settle(request, currentAccount(), currentUserName(), true));
    }

    /**
     * 按每工薪资重新计算本次结算薪资
     */
    @PostMapping("/calcSalary")
    public Response<SettlementDetailVO> calcSalary(@RequestBody SalaryCalcRequest request) {
        return Response.success(settlementService.calculateSalary(request));
    }

    /**
     * 查询结算详情
     */
    @GetMapping("/detail/{settlementId}")
    public Response<SettlementDetailVO> detail(@PathVariable Long settlementId) {
        return Response.success(settlementService.buildDetail(settlementId));
    }

    /**
     * 查询项目结算记录
     */
    @GetMapping("/list")
    public Response<List<TimesheetSettlement>> list(@RequestParam("projectId") Long projectId) {
        return Response.success(settlementService.listProjectSettlements(projectId, currentAccount()));
    }

    /**
     * 获取当前登录账号
     */
    private String currentAccount() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("未登录");
        }
        return account;
    }

    /**
     * 获取当前登录用户名，缺省时回退到账号
     */
    private String currentUserName() {
        String userName = SessionUtil.getCurrentUserName();
        return userName == null || userName.isBlank() ? currentAccount() : userName;
    }
}
