package com.pm.auth_service.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pm.auth_service.dto.Request.LoginRequest;
import com.pm.auth_service.dto.Response.LoginResponse;
import com.pm.auth_service.service.AuthService;


@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> Login(@RequestBody  LoginRequest req) {
     System.out.println("email: " + req.getEmail());
    System.out.println("pass: " + req.getPassword());
    // return ResponseEntity.ok(authService.login(req));
    System.out.println("this is login controller");
    Optional<String> tokenOptional = authService.login(req);
    if(tokenOptional.isEmpty()){
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String token = tokenOptional.get();

    return ResponseEntity.ok(new LoginResponse(token));
  }
}
