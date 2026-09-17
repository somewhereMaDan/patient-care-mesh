package com.pm.auth_service.dto;

import java.util.UUID;

public record UserPrincipal(
    UUID userId,
    String email,
    String role
  ) {
  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }
}