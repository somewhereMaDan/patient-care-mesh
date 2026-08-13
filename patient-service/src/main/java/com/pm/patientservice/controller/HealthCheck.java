package com.pm.patientservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheck {

  @GetMapping("/")
  public ResponseEntity<String> root() {
    return ResponseEntity.ok("Patient Service is running!");
  }
}
