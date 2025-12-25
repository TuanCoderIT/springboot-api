package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.ExportFormat;
import com.example.springboot_api.dto.exam.ExportRequest;
import com.example.springboot_api.dto.exam.ExportResponse;

import java.util.UUID;

public interface ExamExportService {
    
    /**
     * Export exam results in the specified format
     * 
     * @param examId The exam ID to export results for
     * @param request Export configuration
     * @param lecturerId The lecturer requesting the export
     * @return Export response with file data
     */
    ExportResponse exportExamResults(UUID examId, ExportRequest request, UUID lecturerId);
    
    /**
     * Export results for multiple exams
     * 
     * @param examIds Array of exam IDs to export
     * @param request Export configuration
     * @param lecturerId The lecturer requesting the export
     * @return Export response with file data
     */
    ExportResponse exportMultipleExamResults(UUID[] examIds, ExportRequest request, UUID lecturerId);
}