package com.pm.auth_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pm.auth_service.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtUtil {
  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration:3600000}")
  private Long expiration;

  public String generateToken(User user) {
    SecretKey key = Keys.hmacShaKeyFor(
        secret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(user.getEmail())
        .claims(Map.of("role", user.getRole(), "userId", user.getId()))
        .issuedAt(new Date())
        .expiration(
            new Date(System.currentTimeMillis() + expiration))
        .signWith(key)
        .compact();
  }
}
