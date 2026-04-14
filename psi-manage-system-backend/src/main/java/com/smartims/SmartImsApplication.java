package com.smartims;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智链进销存管理系统启动类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@SpringBootApplication
@MapperScan("com.smartims.mapper")
public class SmartImsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartImsApplication.class, args);
        System.out.println("========================================");
        System.out.println("  智链进销存管理系统启动成功！");
        System.out.println("  API文档地址: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }

}