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


    public String createAccessToken(String email, String name, UserType userType) {

        Claims claims = Jwts.claims().setSubject(email);

        claims.put("name", name);
        claims.put("userType", userType);

        return createToken(claims, accessTokenExpirationMillis);


    }

    public String createRefreshToken(String email) {


        Claims claims = Jwts.claims()
                .setSubject(email);


        return createToken(claims, refreshTokenExpirationMillis);


    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
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

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }



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


    private String createToken(Claims claims, Long expirationMillis) {

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(key)
                .compact();
    }



}
