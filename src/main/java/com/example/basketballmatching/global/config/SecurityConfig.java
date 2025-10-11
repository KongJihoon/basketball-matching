package com.example.basketballmatching.global.config;

import com.example.basketballmatching.global.security.AuthentificationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final AuthentificationFilter authentificationFilter;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(
                                        "/api/v1/user/signup",
                                        "/api/v1/user/check-email",
                                        "/api/v1/user/check-nickname",
                                        "/api/v1/user/send-mail",
                                        "/api/v1/user/verify-mail",
                                        "/api/v1/user/login",
                                        "/api/v1/user/reissue"

                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(authentificationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }



}
