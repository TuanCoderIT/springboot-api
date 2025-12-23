package com.example.springboot_api.dto.lecturer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualStudentAddResult {
    private boolean success;
    private String message;
    private boolean userCreated;
    private boolean emailSent;
    private String studentCode;
    private String fullName;
    private String email;
}