package com.xu.home.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.schedule.ScheduleEmployee;

import java.util.List;

/**
 * 员工服务接口
 */
public interface ScheduleEmployeeService extends IService<ScheduleEmployee> {

    /**
     * 获取所有在职员工
     */
    List<ScheduleEmployee> getActiveEmployees();

    /**
     * 分页查询员工
     * @param page 页码
     * @param size 每页大小
     * @param department 部门（可选）
     * @param keyword 关键词（可选，匹配姓名或工号）
     */
    IPage<ScheduleEmployee> pageEmployees(int page, int size, String department, String keyword);

    /**
     * 根据员工编号获取员工
     */
    ScheduleEmployee getByEmployeeCode(String employeeCode);

    /**
     * 获取所有部门列表
     */
    List<String> getAllDepartments();
}
