package com.example.springboot_api.dto.admin.regulation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameRegulationFileRequest {
    @NotBlank(message = "New filename cannot be empty")
    private String newFilename;
}
