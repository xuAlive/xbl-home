package com.xu.home.service.schedule.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.schedule.ScheduleEmployee;
import com.xu.home.mapper.schedule.ScheduleEmployeeMapper;
import com.xu.home.service.schedule.ScheduleEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工服务实现
 */
@Service
public class ScheduleEmployeeServiceImpl extends ServiceImpl<ScheduleEmployeeMapper, ScheduleEmployee>
        implements ScheduleEmployeeService {

    @Override
    public List<ScheduleEmployee> getActiveEmployees() {
        return lambdaQuery()
                .eq(ScheduleEmployee::getStatus, 1)
                .eq(ScheduleEmployee::getIsDelete, 0)
                .orderByAsc(ScheduleEmployee::getEmployeeCode)
                .list();
    }

    @Override
    public IPage<ScheduleEmployee> pageEmployees(int page, int size, String department, String keyword) {
        LambdaQueryWrapper<ScheduleEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEmployee::getIsDelete, 0);

        if (StringUtils.hasText(department)) {
            wrapper.eq(ScheduleEmployee::getDepartment, department);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ScheduleEmployee::getEmployeeName, keyword)
                    .or()
                    .like(ScheduleEmployee::getEmployeeCode, keyword)
            );
        }

        wrapper.orderByDesc(ScheduleEmployee::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public ScheduleEmployee getByEmployeeCode(String employeeCode) {
        return lambdaQuery()
                .eq(ScheduleEmployee::getEmployeeCode, employeeCode)
                .eq(ScheduleEmployee::getIsDelete, 0)
                .one();
    }

    @Override
    public List<String> getAllDepartments() {
        List<ScheduleEmployee> employees = lambdaQuery()
                .select(ScheduleEmployee::getDepartment)
                .eq(ScheduleEmployee::getIsDelete, 0)
                .isNotNull(ScheduleEmployee::getDepartment)
                .groupBy(ScheduleEmployee::getDepartment)
                .list();

        return employees.stream()
                .map(ScheduleEmployee::getDepartment)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
