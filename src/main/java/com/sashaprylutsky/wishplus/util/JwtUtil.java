package com.sashaprylutsky.wishplus.util;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Generate a secure key
    private final long expirationMs = 86400000*14; // 24 hours * 14 in milliseconds

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
//                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return validateToken(token).getExpiration().before(new Date());
    }
}