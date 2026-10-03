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

    /**
     * Spring Security의 요청 인증/ 인가 규칙과
     * JWT 인증 필터의 실행 위치를 설정한다.
     */
    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                /*
                 * 서버에서 로그인 화면과 세션기반 폼 인증을 제공하지 않고
                 * 별도의 REST 로그인 API와 JWT를 사용하므로 비활성화한다.
                 */
                .formLogin(AbstractHttpConfigurer::disable)

                /*
                 * 현재 인증 토큰을 Cookie가 아니라 Authorization 헤더로 전달하므로
                 * CSRF 보호 기능을 비활성화 한다.
                 */
                .csrf(CsrfConfigurer::disable)
                /*
                 * 회원가입·로그인 등 공개 API는 인증 없이 허용하고,
                 * 나머지 요청은 인증된 사용자만 접근할 수 있게 한다.
                 */
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
                /*
                 * Controller 실행 전에 JWT를 검증하고 인증 객체를 생성하도록
                 * 커스텀 필터를 기존 인증 필터 앞에 배치한다.
                 */
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }


}
