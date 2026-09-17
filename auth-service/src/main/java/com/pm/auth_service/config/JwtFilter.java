package com.pm.auth_service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.pm.auth_service.dto.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
  @Value("${jwt.secret}")
  private String secret;

  private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");

    if (header == null || !header.startsWith(("Bearer "))) {
      filterChain.doFilter(req, res);
      return;
    }

    String token = header.substring(7);

    try {
      SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

      String email = claims.getSubject();

      UUID userId = UUID.fromString(claims.get("userId").toString());
      String role = claims.get("role").toString();

      var principal = new UserPrincipal(userId, email, role);

      var auth = new UsernamePasswordAuthenticationToken(principal, null,
          List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      // Current user:
      // userId = ""
      // email = abc@test.com
      // authorities = [ROLE_ADMIN]

      SecurityContextHolder.getContext().setAuthentication(auth);
      // this stores data like req.user (line 51 and 53) so we'll be able to fetch
      // data for the user throughout the login session
      System.out.println("JWT FILTER EXECUTED");

      System.out.println(
          SecurityContextHolder
              .getContext()
              .getAuthentication());
    } catch (ExpiredJwtException e) {
      log.warn("JWT expired for request {}: {}", req.getRequestURI(), e.getMessage());
      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
      return;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT for request {}: {}", req.getRequestURI(), e.getMessage());
      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
      return;
    }
    filterChain.doFilter(req, res);
  }
}
