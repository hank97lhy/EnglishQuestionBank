package com.kate.bank.question.fce.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.AbstractDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayConfigTests {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withInitializer(context -> {
                    try {
                        var sources = new YamlPropertySourceLoader().load("application",
                                new ClassPathResource("application.yaml"));
                        sources.forEach(source -> context.getEnvironment().getPropertySources().addLast(source));
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .withUserConfiguration(FlywayConfig.class, ProbeConfiguration.class);
    }

    @Test
    void explicitConfigurationInitializesFlywayOnceWithApplicationNamingRules() {
        runner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(Flyway.class)
                    .hasSingleBean(FlywayMigrationInitializer.class);
            assertThat(context.getBean(AtomicInteger.class).get()).isEqualTo(1);
            var config = context.getBean(Flyway.class).getConfiguration();
            assertThat(config.getSqlMigrationPrefix()).isEmpty();
            assertThat(config.getSqlMigrationSeparator()).isEqualTo("_");
            assertThat(config.getSqlMigrationSuffixes()).containsExactly(".sql");
            assertThat(config.isValidateMigrationNaming()).isTrue();
            assertThat(config.isFailOnMissingLocations()).isTrue();
            assertThat(context.getResources("classpath*:db/migration/*.sql")).hasSize(10);
        });
    }

    @Test
    void disabledFlywayDoesNotRunMigration() {
        runner().withPropertyValues("spring.flyway.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(Flyway.class);
            assertThat(context.getBean(AtomicInteger.class).get()).isZero();
        });
    }

    @Test
    void migrationStrategyCallsMigrateOnceAndPropagatesFailure() {
        var calls = new AtomicInteger();
        var strategy = new FlywayConfig().flywayMigrationStrategy();
        var flyway = new Flyway(new ClassicConfiguration()) {
            @Override
            public MigrateResult migrate() {
                calls.incrementAndGet();
                return new MigrateResult();
            }
        };
        strategy.migrate(flyway);
        assertThat(calls.get()).isEqualTo(1);

        var broken = new Flyway(new ClassicConfiguration()) {
            @Override
            public MigrateResult migrate() {
                throw new FlywayException("migration failed");
            }
        };
        assertThatThrownBy(() -> strategy.migrate(broken))
                .isInstanceOf(FlywayException.class).hasMessage("migration failed");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean
        AtomicInteger migrationCalls() {
            return new AtomicInteger();
        }

        @Bean
        AbstractDataSource dataSource() {
            // 本组测试只验证启动生命周期，禁止访问真实数据库。
            return new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new SQLException("Tests must not connect to the business database");
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    return getConnection();
                }
            };
        }

        @Bean
        @Primary
        FlywayMigrationStrategy migrationProbe(AtomicInteger migrationCalls) {
            return flyway -> migrationCalls.incrementAndGet();
        }
    }
}
