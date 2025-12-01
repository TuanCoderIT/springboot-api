package com.example.springboot_api.dto.shared.chat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private UUID id;
    private String fullName;
    private String email;
    private String avatarUrl;
}

