package com.example.springboot_api.dto.lecturer.chapter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChapterRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

}
