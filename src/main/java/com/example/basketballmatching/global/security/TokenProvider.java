package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.type.UserType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.access-token-expiration}")
    private Long accessTokenExpirationMillis;

    @Value("${spring.jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMillis;


    private final UserInfoDetailsService userInfoDetailsService;



    private Key key;

    @PostConstruct
    public void init() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);

        this.key = Keys.hmacShaKeyFor(bytes);
    }


    /**
     * 사용자 식별 정보와 권한을 Claim에 담아 AccessToken 생성
     */
    public String createAccessToken(String email, String name, UserType userType) {

        // 이메일을 JWT의 주 식별값인 subject로 사용한다.
        Claims claims = Jwts.claims().setSubject(email);

        claims.put("name", name);
        claims.put("userType", userType);

        return createToken(claims, accessTokenExpirationMillis);


    }
    /**
     * Access Token 재발급에 사용할 Refresh Token을 생성한다.
     * 사용자 식별에 필요한 이메일만 subject에 포함한다.
     */
    public String createRefreshToken(String email) {


        Claims claims = Jwts.claims()
                .setSubject(email);


        return createToken(claims, refreshTokenExpirationMillis);


    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }


    /**
     * 서버의 서명 키를 사용해 JWT를 검증하고 Claim을 반환
     *
     * 만료된 JWT는 만료 정보를 구분할 수 있도록
     * ExpiredJwtException에 포함된 Claim을 반환한다.
     */
    public Claims parseToken(String token) {

        try {

            log.info("토큰 파싱 시작");

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {
            log.error("토큰 파싱 오류 : {}", e.getMessage());

            return e.getClaims();
        }

    }

    /**
     * JWT 이메일로 현재 사용자 정보를 조회하고
     * Spring Security Authentication 객체로 변환한다.
     */
    public Authentication getAuthentication(String token) {

        /*
         * 토큰 Claim의 권한을 바로 신뢰하지 않고
         * DB에서 활성 사용자의 현재 권한을 다시 조회한다.
         */
        UserDetails userDetails = userInfoDetailsService.loadUserByUsername(getEmailFromToken(token));

        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities()
        );
    }

    public void validateToken(String token) {

        try {

            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            if (claims.getBody().getExpiration().before(new Date())) {
                throw new CustomException(EXPIRED_TOKEN);
            }

        } catch (IllegalArgumentException e) {
            throw new CustomException(NOT_FOUND_TOKEN);
        } catch (MalformedJwtException e) {
            throw new CustomException(INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(UNSUPPORTED_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new CustomException(EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(INVALID_TOKEN);
        }

    }

    /**
     * JWT의 subject에 저장된 사용자 이메일을 반환한다.
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }


    /**
     * RefreshToken의 존재여부와 만료되지 않은 JWT토큰인지 검증한다.
     *
     * parseToken() 과정에서 서명과 JWT 구조가 함께 검증된다.
     */
    public void validateRefreshToken(String refreshToken) {

        if (refreshToken == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        Claims claims = parseToken(refreshToken);

        if (claims.getExpiration().before(new Date())) {
            throw new CustomException(EXPIRED_TOKEN);
        }

    }


    public long getRemainingTime(String token) {

        return parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    }


    /**
     * Claim과 만료시간을 기준으로 서명된 JWT를 생성한다.
     */
    private String createToken(Claims claims, Long expirationMillis) {

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                // 서버의 비밀키로 서명해 토큰 위변조 여부를 검증할 수 있게 한다.
                .signWith(key)
                .compact();
    }



}
