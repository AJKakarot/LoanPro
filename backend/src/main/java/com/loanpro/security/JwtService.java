package com.loanpro.security;

import com.loanpro.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";

    private final AppProperties properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(properties.jwt().secret());
        } catch (Exception ex) {
            bytes = properties.jwt().secret().getBytes();
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.jwt().accessTokenMinutes() * 60);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.jwt().issuer())
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .claim("typ", TOKEN_TYPE_ACCESS)
                .claim("ver", principal.getTokenVersion())
                .audience().add(properties.jwt().audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.jwt().issuer())
                .requireAudience(properties.jwt().audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TOKEN_TYPE_ACCESS.equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }
        return claims;
    }

    public UUID parseUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public int parseTokenVersion(Claims claims) {
        Object value = claims.get("ver");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
