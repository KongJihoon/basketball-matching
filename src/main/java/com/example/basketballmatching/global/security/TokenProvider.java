package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
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
    private Long access_token_expire;

    @Value("${spring.jwt.refresh-token-expiration}")
    private Long refresh_token_expire;


    private final UserInfoDetailsService userInfoDetailsService;

    private final RedisService redisService;


    private Key key;

    @PostConstruct
    public void init() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);

        this.key = Keys.hmacShaKeyFor(bytes);
    }


    public String createAccessToken(String email, String name, UserType userType) {

        log.info("accessToken 생성 시작");

        String accessToken = generateAccessToken(email, name, userType, access_token_expire);

        return accessToken;


    }

    public String createRefreshToken(String email) {

        log.info("refreshToken 생성 시작");

        String refreshToken = generateRefreshToken(email, refresh_token_expire);



        redisService.setDataExpireMillis("refreshToken:" + email, refreshToken, refresh_token_expire);

        return refreshToken;


    }

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

    public Authentication getAuthentication(String token) {

        UserDetails userDetails = userInfoDetailsService.loadUserByUsername(getEmailFromToken(token));

        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities()
        );
    }

    public boolean validateToken(String token) {

        try {

            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !claims.getBody().getExpiration().before(new Date());

        } catch (IllegalArgumentException e) {
            throw new CustomException(NOT_FOUND_TOKEN);
        } catch (MalformedJwtException e) {
            throw new CustomException(UNSUPPORTED_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new CustomException(EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(INVALID_TOKEN);
        }

    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }



    private String generateAccessToken(String email, String name, UserType userType, long access_token_expire) {

        Claims claims = Jwts.claims().setSubject(email);

        claims.put("name", name);
        claims.put("userType", String.valueOf(userType));


        return returnToken(claims, access_token_expire);

    }

    private String generateRefreshToken(String email, long refresh_token_expire) {

        Claims claims = Jwts.claims().setSubject(email);


        return returnToken(claims, refresh_token_expire);

    }

    private String returnToken(Claims claims, long expireTime) {

        var now = new Date();

        var expireDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key)
                .compact();
    }


}
