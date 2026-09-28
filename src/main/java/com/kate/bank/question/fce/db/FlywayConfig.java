package com.kate.bank.question.fce.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式接入 Spring Boot 4 的 Flyway 模块。
 * IDEA 必须同步 Maven 依赖；只有 flyway-core 不会触发 Boot 的启动迁移。
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        // 由 Boot 的 FlywayMigrationInitializer 调用一次，不再在 ApplicationRunner 中迁移。
        return flyway -> {
            log.info("开始执行 Flyway 数据库迁移");
            var result = flyway.migrate();
            if (result.migrationsExecuted == 0) {
                log.info("Flyway 数据库迁移完成：没有待执行的迁移");
            } else {
                log.info("Flyway 数据库迁移完成：本次执行 {} 个迁移，目标版本 {}",
                        result.migrationsExecuted, result.targetSchemaVersion);
            }
        };
    }
}
