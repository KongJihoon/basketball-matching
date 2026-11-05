package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Security 인증/ 인가 HTTP 흐름 통합 테스트")
class SecurityFlowIntegrationTest {

    private static final String MY_PAGE = "/api/v1/users/mypage";

    private static final String ADMIN_REPORTS = "/api/v1/admin/reports";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Test
    @DisplayName("토큰 없이 보호 API 접근 시 401 UNAUTHORIZED 상태코드 반환")
    void protectedApi_withoutToken() throws Exception {
        // given

        ResultActions result = mockMvc.perform(get(MY_PAGE));

        expectError(result, NOT_FOUND_TOKEN);

        // when

        result.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));


        // then

        Assertions.assertNull(result.andReturn().getRequest().getSession(false));

    }

    @Test
    @DisplayName("잘못된 JWT를 보내면 공통 JSON으로 401을 반환한다")
    void protectedApi_withInvalidToken() throws Exception {
        // given

        ResultActions result = mockMvc.perform(get(MY_PAGE).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"));


        // when

        // then

        expectError(result, INVALID_TOKEN);

    }

    @Test
    @DisplayName("만료된 Access Token을 보내면 EXPIRED_TOKEN을 반환한다")
    void protectedApi_withExpiredAccessToken() throws Exception {
        // given

        UserEntity user = saveUser();

        String expiredAccessToken = createExpiredAccessToken(user.getEmail());

        // when

        ResultActions result = mockMvc.perform(
                get(MY_PAGE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken))
        );

        // then

        expectError(result, EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token을 일반 API에 보내면 인증 거절")
    void protectedApi_withRefreshToken() throws Exception {
        // given
        UserEntity user = saveUser();

        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // when

        ResultActions result = mockMvc.perform(
                get(MY_PAGE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshToken))
        );

        // then

        expectError(result, INVALID_TOKEN);
    }

    @Test
    @DisplayName("USER가 ADMIN API에 접근하면 403 FORBIDDEN 상태코드 반환")
    void adminApi_withUserRole() throws Exception {
        // given

        UserEntity user = saveUser();

        String accessToken = createAccessToken(user);

        // when

        ResultActions result = mockMvc.perform(
                get(ADMIN_REPORTS)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
        );

        // then

        expectError(result, ACCESS_DENIED);

    }

    @Test
    @DisplayName("정상 USER Access Token으로 마이페이지에 접근할 수 있다")
    void protectedApi_withValidAccessToken() throws Exception {
        // given
        UserEntity user = saveUser();

        String accessToken = createAccessToken(user);

        // when

        mockMvc.perform(get(MY_PAGE)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // then

    }

    private void expectError(ResultActions result, ErrorCode errorCode) throws Exception {
        result.andExpect(status().is(errorCode.getStatusCode()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(errorCode.getStatusCode()))
                .andExpect(jsonPath("$.errorCode").value(errorCode.name()))
                .andExpect(jsonPath("$.errorMessage").value(errorCode.getErrorMessage()));
    }

    private String createAccessToken(UserEntity user) {
        return tokenProvider.createAccessToken(user.getEmail(), user.getName(), user.getUserType());
    }

    private String createExpiredAccessToken(String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(email)
                .claim("token-use", TokenUse.ACCESS.getClaimValue())
                .setIssuedAt(Date.from(now.minusSeconds(120)))
                .setExpiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                .compact();
    }

    private UserEntity saveUser() {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12);

        UserEntity user = UserEntity.create(
                "security-" + suffix + "@example.test",
                "unused-password",
                "보안" + suffix,
                "보안테스트",
                LocalDate.of(1995, 1, 1),
                "010-1234-5678",
                "서울특별시",
                Position.GUARD,
                GenderType.MALE
        );

        user.setEmailAuth();

        return userRepository.saveAndFlush(user);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
