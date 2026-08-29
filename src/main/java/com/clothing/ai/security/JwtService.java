package com.clothing.ai.security;

import com.clothing.ai.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties props;

    private SecretKey getKey() {
        String s = props.getJwt().getSecret();
        try {
            byte[] bytes = Decoders.BASE64.decode(s);
            if (bytes.length >= 32) return Keys.hmacShaKeyFor(bytes);
        } catch (Exception ignored) {}
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("email", email, "role", role, "type", "access"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + props.getJwt().getAccessTokenExpiration()))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("type", "refresh"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + props.getJwt().getRefreshTokenExpiration()))
                .signWith(getKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }
}
