package com.example.basketballmatching.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

public class IntegrationTestInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                    DockerImageName.parse("mysql:8.0")
            )
                    .withDatabaseName("basketball_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam(
                            "serverTimezone",
                            "Asia/Seoul"
                    )
                    .withUrlParam(
                            "characterEncoding",
                            "UTF-8"
                    );

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "redis:7.2-alpine"
                    )
            )
                    .withExposedPorts(6379);

    static {
        Startables.deepStart(
                Stream.of(MYSQL, REDIS)
        ).join();
    }

    @Override
    public void initialize(
            ConfigurableApplicationContext context
    ) {
        TestPropertyValues.of(
                "spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "spring.datasource.username=" + MYSQL.getUsername(),
                "spring.datasource.password=" + MYSQL.getPassword(),
                "spring.datasource.driver-class-name="
                        + MYSQL.getDriverClassName(),
                "spring.data.redis.host=" + REDIS.getHost(),
                "spring.data.redis.port="
                        + REDIS.getMappedPort(6379)
        ).applyTo(context.getEnvironment());
    }
}
