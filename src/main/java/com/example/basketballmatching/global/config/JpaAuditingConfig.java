package com.example.basketballmatching.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(
        dateTimeProviderRef = "auditingDateTimeProvider"
)
@RequiredArgsConstructor
public class JpaAuditingConfig {

    private final Clock clock;

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(
                LocalDateTime.now(clock)
        );
    }
}
