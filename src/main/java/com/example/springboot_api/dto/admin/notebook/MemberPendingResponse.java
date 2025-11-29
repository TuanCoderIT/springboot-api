package com.example.springboot_api.dto.admin.notebook;

import java.util.UUID;

import lombok.Data;

@Data
public class MemberPendingResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String role;
    private String status;
}
