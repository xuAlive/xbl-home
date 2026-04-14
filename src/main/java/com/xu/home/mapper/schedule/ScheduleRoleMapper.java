package com.xu.home.mapper.schedule;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduleRoleMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.account = #{account}
              AND r.role_code = 'ADMIN'
              AND r.status = 1
              AND r.is_delete = 0
            """)
    int countAdminRole(@Param("account") String account);
}
