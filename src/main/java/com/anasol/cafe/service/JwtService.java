package com.anasol.cafe.service;

import com.anasol.cafe.entity.Branch;
import com.anasol.cafe.entity.User;
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
    private final long expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expiration
    ) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(User user) {

        Branch branch = user.getBranch();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("branchId", branch != null ? branch.getId() : null)
                .claim("branchCode", branch != null ? branch.getBranchCode() : null)
                .claim("branchName", branch != null ? branch.getBranchName() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return validateToken(token).getSubject();
    }

    public String extractRole(String token) {
        return validateToken(token).get("role", String.class);
    }

    public Long extractBranchId(String token) {
        return validateToken(token).get("branchId", Long.class);
    }

    public String extractBranchCode(String token) {
        return validateToken(token).get("branchCode", String.class);
    }

    public String extractBranchName(String token) {
        return validateToken(token).get("branchName", String.class);
    }
}