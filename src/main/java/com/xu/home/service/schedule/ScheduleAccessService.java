package com.xu.home.service.schedule;

public interface ScheduleAccessService {

    boolean isAdmin(String account);

    void requireAdmin(String account);
}
