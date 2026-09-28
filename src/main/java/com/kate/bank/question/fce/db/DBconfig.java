package com.kate.bank.question.fce.db;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DBconfig {

    @Bean
    public ApplicationRunner dbCheck(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("连接成功: " + conn.getMetaData().getURL());
                System.out.println("数据库产品: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("数据库版本: " + conn.getMetaData().getDatabaseProductVersion());
            } catch (Exception e) {
                System.err.println("连接失败: " + e.getMessage());
                throw e;
            }
        };
    }
}
