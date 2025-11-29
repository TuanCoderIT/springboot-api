package com.example.springboot_api.dto.admin.user;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class UserResponse {
  private UUID id;
  private String fullName;
  private String email;
  private String role;
  private Boolean active;
  private String avatarUrl;
  private Instant createdAt;
}
