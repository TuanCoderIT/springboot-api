package com.example.springboot_api.dto.lecturer.notebook;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerNotebookSummary {
    
    private UUID id;
    private String title;
    private String description;
    private String type; // "personal", "community", "class"
    
    // Thống kê files
    private Long totalFiles;
    private Long readyFiles; // Files có status = 'done'
    
    // Thông tin class (nếu là notebook của class)
    private UUID classId;
    private String className;
    private String subjectCode;
    private String subjectName;
}