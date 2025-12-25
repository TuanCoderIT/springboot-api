package com.example.springboot_api.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ExportRequest {
    
    @NotNull(message = "Export format is required")
    private ExportFormat format;
    
    private UUID classId; // Optional: filter by specific class
    
    private Boolean includeStudentInfo = true;
    
    private Boolean includeScores = true;
    
    private Boolean includeTimings = true;
    
    private Boolean includeAntiCheatEvents = false;
    
    private Boolean includeDetailedAnswers = false;
}