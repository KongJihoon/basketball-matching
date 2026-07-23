package com.example.basketballmatching.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
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

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "redis:7.2-alpine"
                    )
            )
                    .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    protected static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );

        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(6379)
        );
    }
}