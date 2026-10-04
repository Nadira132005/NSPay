package com.nspay.services;

import com.nspay.entities.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserModel userModel) {
        return generateToken(userModel, null);
    }

    public String generateToken(UserModel userModel, String accountCardNumber) {
        long now = System.currentTimeMillis();
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(userModel.getName())
                .claim("userId", userModel.getId())
                .claim("role", userModel.getRole() == null ? "User" : userModel.getRole().name())
                .claim("passwordChangeRequired", userModel.isPasswordChangeRequired())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs));

        if (accountCardNumber != null) {
            builder.claim("accountCardNumber", accountCardNumber);
        }

        return builder
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
