package com.pandanav.learning.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String USER_ID = "userId";

    private final AuthProperties authProperties;

    public JwtUtil(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(authProperties.getTokenExpireDays(), ChronoUnit.DAYS);
        return Jwts.builder()
            .claim(USER_ID, userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expireAt))
            .signWith(secretKey())
            .compact();
    }

    public Long parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        Object value = claims.get(USER_ID);
        if (value == null) {
            throw new UnauthorizedException("Invalid token.");
        }
        if (value instanceof Integer integer) {
            return integer.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        return Long.parseLong(value.toString());
    }

    private SecretKey secretKey() {
        byte[] keyBytes = authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
