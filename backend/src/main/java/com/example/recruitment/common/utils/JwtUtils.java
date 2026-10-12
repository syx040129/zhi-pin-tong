package com.example.recruitment.common.utils;

import com.example.recruitment.common.security.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析工具
 */
@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expire-hours:168}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generateToken(AuthUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("nickname", user.getNickname() == null ? "" : user.getNickname())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，失败返回 null
     */
    public AuthUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            Long uid = claims.get("uid", Long.class);
            if (uid == null) {
                return null;
            }
            return new AuthUser(uid, claims.getSubject(),
                    claims.get("nickname", String.class),
                    claims.get("role", String.class));
        } catch (Exception e) {
            return null;
        }
    }
}
