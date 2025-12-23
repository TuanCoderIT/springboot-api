package com.example.springboot_api.controllers.lecturer;

import com.example.springboot_api.common.dto.ApiResponse;
import com.example.springboot_api.dto.lecturer.*;
import com.example.springboot_api.services.lecturer.ClassManagementService;
import com.example.springboot_api.services.lecturer.ExcelPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lecturer/class-management")
@RequiredArgsConstructor
@Tag(name = "Class Management", description = "API quản lý lớp học phần cho giảng viên")
public class ClassManagementController {
    
    private final ClassManagementService classManagementService;
    private final ExcelPreviewService excelPreviewService;
    
    @PostMapping(value = "/create-with-students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo lớp học phần và import sinh viên từ Excel", 
               description = "Luồng A: Upload Excel để tạo lớp học phần mới và import sinh viên cùng lúc")
    public ResponseEntity<ApiResponse<StudentImportResult>> createClassWithStudents(
            @Valid @ModelAttribute ClassImportRequest request,
            @RequestHeader("X-User-Id") UUID lecturerId) {
        
        try {
            log.info("Tạo lớp học phần mới với {} sinh viên từ Excel", 
                    request.getExcelFile().getOriginalFilename());
            
            StudentImportResult result = classManagementService
                    .createClassWithStudents(request, lecturerId);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                    "Tạo lớp học phần và import sinh viên thành công"));
            
        } catch (Exception e) {
            log.error("Lỗi tạo lớp học phần: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo lớp học phần: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = "/import-students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import sinh viên vào lớp học phần có sẵn", 
               description = "Luồng B: Import sinh viên từ Excel vào lớp học phần đã tồn tại")
    public ResponseEntity<ApiResponse<StudentImportResult>> importStudentsToClass(
            @Valid @ModelAttribute StudentImportRequest request) {
        
        try {
            log.info("Import sinh viên từ Excel vào lớp học phần ID: {}", request.getClassId());
            
            StudentImportResult result = classManagementService
                    .importStudentsToExistingClass(request);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                    "Import sinh viên thành công"));
            
        } catch (Exception e) {
            log.error("Lỗi import sinh viên: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi import sinh viên: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = "/preview-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview dữ liệu Excel trước khi import", 
               description = "Kiểm tra và preview dữ liệu sinh viên từ file Excel")
    public ResponseEntity<ApiResponse<StudentImportResult>> previewExcelData(
            @RequestParam("excelFile") org.springframework.web.multipart.MultipartFile excelFile,
            @RequestParam(value = "classId", required = false) UUID classId) {
        
        try {
            log.info("Preview dữ liệu Excel: {}", excelFile.getOriginalFilename());
            
            StudentImportResult result = excelPreviewService.previewExcelData(excelFile, classId);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                    "Preview dữ liệu Excel thành công"));
            
        } catch (Exception e) {
            log.error("Lỗi preview Excel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi preview Excel: " + e.getMessage()));
        }
    }
}