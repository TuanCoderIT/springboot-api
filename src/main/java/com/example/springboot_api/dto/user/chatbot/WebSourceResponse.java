package com.example.springboot_api.dto.user.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSourceResponse {
    private String sourceType; // "WEB"
    private Integer webIndex;
    private String url;
    private String title;
    private String snippet;
    private Double score;
    private String provider; // "google"
    private String imageUrl;
    private String favicon;
}

