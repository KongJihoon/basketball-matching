package com.example.basketballmatching.global.config;

import com.example.basketballmatching.global.security.AuthentificationFilter;
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


    private final AuthentificationFilter authentificationFilter;

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
                                        "/api/v1/users/availability/nickname"
                                ).permitAll()
                                .requestMatchers(HttpMethod.PATCH,
                                        "/api/v1/password-resets"
                                ).permitAll()
                                .requestMatchers(
                                        "/api/oauth2/**",
                                        "/api/v1/game/details",
                                        "/api/v1/game/search",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger"

                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(authentificationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }


}
