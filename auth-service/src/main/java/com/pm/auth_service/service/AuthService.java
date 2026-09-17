package com.pm.auth_service.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pm.auth_service.Repository.UserRepository;
import com.pm.auth_service.dto.Request.LoginRequest;
import com.pm.auth_service.dto.Response.LoginResponse;
import com.pm.auth_service.model.User;
import com.pm.auth_service.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AuthService {
  private final UserRepository userModal;
  private final PasswordEncoder encoder;
  private final JwtUtil jwt;
  
  public Optional<String> login(LoginRequest req) {
    System.out.println("this is login service");

    System.out.println("email: " + req.getEmail());
    System.out.println("pass: " + req.getPassword());

    User user = userModal.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid Email"));

    System.out.println("after finding user");

    System.out.println("Password valid: " +
    encoder.matches("maddy@1234", user.getPassword()));

    boolean valid = encoder.matches(req.getPassword(), user.getPassword());

    if (!valid) {
      throw new RuntimeException("password did not match");
    }

    String token = jwt.generateToken(user);
    return Optional.of(token);
  }
}
