package com.xu.home;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * `xbl-home` 单体项目统一启动入口。
 * 这里统一扫描新的 `com.xu.home` 分层包结构和全部 Mapper。
 */
@SpringBootApplication(scanBasePackages = "com.xu.home")
@MapperScan({
        "com.xu.home.mapper.blog",
        "com.xu.home.mapper.calendar",
        "com.xu.home.mapper.schedule",
        "com.xu.home.mapper.timesheet"
})
public class XblHomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XblHomeApplication.class, args);
    }
}
