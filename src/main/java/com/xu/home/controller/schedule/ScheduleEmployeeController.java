package com.xu.home.controller.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.domain.schedule.ScheduleEmployee;
import com.xu.home.service.schedule.ScheduleAccessService;
import com.xu.home.service.schedule.ScheduleEmployeeService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工管理接口
 */
@RestController
@RequestMapping("/schedule/employee")
public class ScheduleEmployeeController {

    private final ScheduleEmployeeService employeeService;
    private final ScheduleAccessService scheduleAccessService;

    public ScheduleEmployeeController(ScheduleEmployeeService employeeService, ScheduleAccessService scheduleAccessService) {
        this.employeeService = employeeService;
        this.scheduleAccessService = scheduleAccessService;
    }

    /**
     * 获取所有在职员工
     */
    @GetMapping("/list")
    public Response<List<ScheduleEmployee>> getActiveEmployees() {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        List<ScheduleEmployee> employees = employeeService.getActiveEmployees();
        return Response.success(employees);
    }

    /**
     * 分页查询员工
     */
    @GetMapping("/page")
    public Response<IPage<ScheduleEmployee>> pageEmployees(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "keyword", required = false) String keyword) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        IPage<ScheduleEmployee> result = employeeService.pageEmployees(page, size, department, keyword);
        return Response.success(result);
    }

    /**
     * 获取员工详情
     */
    @GetMapping("/detail/{id}")
    public Response<ScheduleEmployee> getEmployeeDetail(@PathVariable Long id) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        ScheduleEmployee employee = employeeService.getById(id);
        return Response.success(employee);
    }

    /**
     * 根据员工编号获取员工
     */
    @GetMapping("/getByCode")
    public Response<ScheduleEmployee> getByEmployeeCode(@RequestParam("code") String code) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        ScheduleEmployee employee = employeeService.getByEmployeeCode(code);
        return Response.success(employee);
    }

    /**
     * 创建员工
     */
    @PostMapping("/create")
    public Response<?> createEmployee(@RequestBody ScheduleEmployee employee) {
        String currentAccount = SessionUtil.getCurrentAccount();
        scheduleAccessService.requireAdmin(currentAccount);
        // 检查员工编号是否已存在
        ScheduleEmployee existing = employeeService.getByEmployeeCode(employee.getEmployeeCode());
        if (existing != null) {
            return Response.error("员工编号已存在");
        }

        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employee.setCreateBy(currentAccount);
        employee.setIsDelete(0);
        if (employee.getStatus() == null) {
            employee.setStatus(1);
        }
        boolean result = employeeService.save(employee);
        return Response.checkResult(result);
    }

    /**
     * 更新员工
     */
    @PostMapping("/update")
    public Response<?> updateEmployee(@RequestBody ScheduleEmployee employee) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        employee.setUpdateTime(LocalDateTime.now());
        boolean result = employeeService.updateById(employee);
        return Response.checkResult(result);
    }

    /**
     * 删除员工（软删除）
     */
    @PostMapping("/delete")
    public Response<?> deleteEmployee(@RequestBody IdPO po) {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        Long id = po.getId();
        ScheduleEmployee employee = new ScheduleEmployee();
        employee.setId(id);
        employee.setIsDelete(1);
        employee.setUpdateTime(LocalDateTime.now());
        boolean result = employeeService.updateById(employee);
        return Response.checkResult(result);
    }

    /**
     * 获取所有部门列表
     */
    @GetMapping("/departments")
    public Response<List<String>> getAllDepartments() {
        scheduleAccessService.requireAdmin(SessionUtil.getCurrentAccount());
        List<String> departments = employeeService.getAllDepartments();
        return Response.success(departments);
    }
}
