package com.example.springboot_api.dto.lecturer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportResult {
    private int totalRows;
    private int successCount;
    private int duplicateCount;
    private int errorCount;
    
    @Builder.Default
    private List<StudentImportError> duplicates = new ArrayList<>();
    
    @Builder.Default
    private List<StudentImportError> errors = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentImportError {
        private int rowNumber;
        private String studentCode;
        private String fullName;
        private String reason;
    }
}