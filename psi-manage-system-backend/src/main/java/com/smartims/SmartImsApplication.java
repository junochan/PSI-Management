package com.smartims;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 智链进销存管理系统启动类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.smartims.mapper")
public class SmartImsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartImsApplication.class, args);
    }

}