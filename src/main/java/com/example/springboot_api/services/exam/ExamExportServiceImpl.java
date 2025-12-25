package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.ExportFormat;
import com.example.springboot_api.dto.exam.ExportRequest;
import com.example.springboot_api.dto.exam.ExportResponse;
import com.example.springboot_api.exceptions.ExportGenerationException;
import com.example.springboot_api.exceptions.InvalidExportFormatException;
import com.example.springboot_api.models.exam.Exam;
import com.example.springboot_api.models.exam.ExamAttempt;
import com.example.springboot_api.repositories.exam.ExamRepository;
import com.example.springboot_api.repositories.exam.ExamAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamExportServiceImpl implements ExamExportService {
    
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    public ExportResponse exportExamResults(UUID examId, ExportRequest request, UUID lecturerId) {
        log.info("Exporting exam results for exam {} by lecturer {}", examId, lecturerId);
        
        // Validate exam exists and lecturer has access
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Access denied: You can only export results for your own exams");
        }
        
        // Get exam attempts with optional class filtering
        List<ExamAttempt> attempts;
        if (request.getClassId() != null) {
            attempts = examAttemptRepository.findByExamIdAndClassId(examId, request.getClassId());
        } else {
            attempts = examAttemptRepository.findByExamIdOrderBySubmittedAtDesc(examId);
        }
        
        // Generate export based on format
        switch (request.getFormat()) {
            case EXCEL:
                return generateExcelExport(exam, attempts, request);
            case CSV:
                return generateCsvExport(exam, attempts, request);
            default:
                throw new InvalidExportFormatException("Unsupported export format: " + request.getFormat());
        }
    }
    
    @Override
    public ExportResponse exportMultipleExamResults(UUID[] examIds, ExportRequest request, UUID lecturerId) {
        log.info("Exporting multiple exam results for {} exams by lecturer {}", examIds.length, lecturerId);
        
        // For now, we'll implement single exam export. Multiple exam export can be added later
        if (examIds.length == 1) {
            return exportExamResults(examIds[0], request, lecturerId);
        }
        
        throw new UnsupportedOperationException("Multiple exam export not yet implemented");
    }
    
    private ExportResponse generateExcelExport(Exam exam, List<ExamAttempt> attempts, ExportRequest request) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Exam Results");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Add headers based on request configuration
            if (request.getIncludeStudentInfo()) {
                createHeaderCell(headerRow, colIndex++, "Student Code", headerStyle);
                createHeaderCell(headerRow, colIndex++, "Student Name", headerStyle);
            }
            
            createHeaderCell(headerRow, colIndex++, "Attempt Number", headerStyle);
            createHeaderCell(headerRow, colIndex++, "Status", headerStyle);
            createHeaderCell(headerRow, colIndex++, "Started At", headerStyle);
            createHeaderCell(headerRow, colIndex++, "Submitted At", headerStyle);
            
            if (request.getIncludeTimings()) {
                createHeaderCell(headerRow, colIndex++, "Time Spent (minutes)", headerStyle);
            }
            
            if (request.getIncludeScores()) {
                createHeaderCell(headerRow, colIndex++, "Total Score", headerStyle);
                createHeaderCell(headerRow, colIndex++, "Percentage", headerStyle);
                createHeaderCell(headerRow, colIndex++, "Passed", headerStyle);
            }
            
            if (request.getIncludeAntiCheatEvents()) {
                createHeaderCell(headerRow, colIndex++, "Proctoring Flags", headerStyle);
            }
            
            // Add data rows
            int rowIndex = 1;
            for (ExamAttempt attempt : attempts) {
                Row dataRow = sheet.createRow(rowIndex++);
                int dataColIndex = 0;
                
                if (request.getIncludeStudentInfo()) {
                    dataRow.createCell(dataColIndex++).setCellValue(attempt.getStudent().getStudentCode() != null ? 
                        attempt.getStudent().getStudentCode() : "N/A");
                    dataRow.createCell(dataColIndex++).setCellValue(attempt.getStudent().getFullName() != null ? 
                        attempt.getStudent().getFullName() : "N/A");
                }
                
                dataRow.createCell(dataColIndex++).setCellValue(attempt.getAttemptNumber());
                dataRow.createCell(dataColIndex++).setCellValue(attempt.getStatus().toString());
                dataRow.createCell(dataColIndex++).setCellValue(attempt.getStartedAt().format(DATE_TIME_FORMATTER));
                dataRow.createCell(dataColIndex++).setCellValue(attempt.getSubmittedAt() != null ? 
                    attempt.getSubmittedAt().format(DATE_TIME_FORMATTER) : "Not submitted");
                
                if (request.getIncludeTimings()) {
                    double timeSpentMinutes = attempt.getTimeSpentSeconds() != null ? 
                        attempt.getTimeSpentSeconds() / 60.0 : 0.0;
                    dataRow.createCell(dataColIndex++).setCellValue(timeSpentMinutes);
                }
                
                if (request.getIncludeScores()) {
                    dataRow.createCell(dataColIndex++).setCellValue(attempt.getTotalScore() != null ? 
                        attempt.getTotalScore().doubleValue() : 0.0);
                    dataRow.createCell(dataColIndex++).setCellValue(attempt.getPercentageScore() != null ? 
                        attempt.getPercentageScore().doubleValue() : 0.0);
                    dataRow.createCell(dataColIndex++).setCellValue(attempt.getIsPassed() != null ? 
                        (attempt.getIsPassed() ? "Yes" : "No") : "N/A");
                }
                
                if (request.getIncludeAntiCheatEvents()) {
                    // This would need to be implemented when anti-cheat system is ready
                    dataRow.createCell(dataColIndex++).setCellValue("N/A");
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < colIndex; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            String filename = generateFilename(exam, ExportFormat.EXCEL);
            return new ExportResponse(filename, ExportFormat.EXCEL.getMimeType(), outputStream.toByteArray());
            
        } catch (IOException e) {
            log.error("Error generating Excel export for exam {}", exam.getId(), e);
            throw new ExportGenerationException("Failed to generate Excel export", e);
        }
    }
    
    private ExportResponse generateCsvExport(Exam exam, List<ExamAttempt> attempts, ExportRequest request) {
        try {
            StringBuilder csv = new StringBuilder();
            
            // Add headers
            if (request.getIncludeStudentInfo()) {
                csv.append("Student Code,Student Name,");
            }
            csv.append("Attempt Number,Status,Started At,Submitted At,");
            
            if (request.getIncludeTimings()) {
                csv.append("Time Spent (minutes),");
            }
            
            if (request.getIncludeScores()) {
                csv.append("Total Score,Percentage,Passed,");
            }
            
            if (request.getIncludeAntiCheatEvents()) {
                csv.append("Proctoring Flags,");
            }
            
            // Remove trailing comma and add newline
            if (csv.length() > 0 && csv.charAt(csv.length() - 1) == ',') {
                csv.setLength(csv.length() - 1);
            }
            csv.append("\n");
            
            // Add data rows
            for (ExamAttempt attempt : attempts) {
                if (request.getIncludeStudentInfo()) {
                    csv.append(escapeCsvValue(attempt.getStudent().getStudentCode() != null ? 
                        attempt.getStudent().getStudentCode() : "N/A")).append(",");
                    csv.append(escapeCsvValue(attempt.getStudent().getFullName() != null ? 
                        attempt.getStudent().getFullName() : "N/A")).append(",");
                }
                
                csv.append(attempt.getAttemptNumber()).append(",");
                csv.append(escapeCsvValue(attempt.getStatus().toString())).append(",");
                csv.append(escapeCsvValue(attempt.getStartedAt().format(DATE_TIME_FORMATTER))).append(",");
                csv.append(escapeCsvValue(attempt.getSubmittedAt() != null ? 
                    attempt.getSubmittedAt().format(DATE_TIME_FORMATTER) : "Not submitted")).append(",");
                
                if (request.getIncludeTimings()) {
                    double timeSpentMinutes = attempt.getTimeSpentSeconds() != null ? 
                        attempt.getTimeSpentSeconds() / 60.0 : 0.0;
                    csv.append(String.format("%.2f", timeSpentMinutes)).append(",");
                }
                
                if (request.getIncludeScores()) {
                    csv.append(attempt.getTotalScore() != null ? 
                        attempt.getTotalScore().toString() : "0").append(",");
                    csv.append(attempt.getPercentageScore() != null ? 
                        attempt.getPercentageScore().toString() : "0").append(",");
                    csv.append(attempt.getIsPassed() != null ? 
                        (attempt.getIsPassed() ? "Yes" : "No") : "N/A").append(",");
                }
                
                if (request.getIncludeAntiCheatEvents()) {
                    csv.append("N/A,");
                }
                
                // Remove trailing comma and add newline
                if (csv.length() > 0 && csv.charAt(csv.length() - 1) == ',') {
                    csv.setLength(csv.length() - 1);
                }
                csv.append("\n");
            }
            
            String filename = generateFilename(exam, ExportFormat.CSV);
            return new ExportResponse(filename, ExportFormat.CSV.getMimeType(), csv.toString().getBytes());
            
        } catch (Exception e) {
            log.error("Error generating CSV export for exam {}", exam.getId(), e);
            throw new ExportGenerationException("Failed to generate CSV export", e);
        }
    }
    
    private void createHeaderCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private String escapeCsvValue(String value) {
        if (value == null) return "";
        
        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String generateFilename(Exam exam, ExportFormat format) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String examTitle = exam.getTitle().replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return String.format("exam_results_%s_%s.%s", examTitle, timestamp, format.getExtension());
    }
}