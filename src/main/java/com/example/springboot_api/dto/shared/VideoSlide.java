package com.example.springboot_api.dto.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đơn giản cho video slide.
 * Dùng để truyền dữ liệu giữa các bước trong video generation pipeline.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoSlide {

    private int index;
    private String title;
    private String body;
    private String imagePrompt;
    private String audioScript;

    // Paths sau khi generate
    private String imagePath;
    private String audioPath;
    private double audioDuration;

    // Status
    private boolean imageReady;
    private boolean audioReady;
    private String error;
}
