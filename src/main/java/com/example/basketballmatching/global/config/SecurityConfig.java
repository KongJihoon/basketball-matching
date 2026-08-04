package com.example.basketballmatching.global.config;

import com.example.basketballmatching.global.security.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


    private final AuthenticationFilter authenticationFilter;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/users/signup",
                                        "/api/v1/email-verifications",
                                        "/api/v1/email-verifications/confirm",
                                        "/api/v1/password-resets/email-verifications",
                                        "/api/v1/password-resets/email-verifications/confirm",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/token/refresh"
                                ).permitAll()
                                .requestMatchers(HttpMethod.GET,
                                        "/api/v1/users/availability/email",
                                        "/api/v1/users/availability/nickname",
                                        "/api/v1/games",
                                        "/api/v1/games/{gameId}"

                                ).permitAll()
                                .requestMatchers(HttpMethod.PATCH,
                                        "/api/v1/password-resets"
                                ).permitAll()
                                .requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/api/v1/auth/oauth2/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger",
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/prometheus"

                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }


}
