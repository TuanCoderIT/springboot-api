package com.example.springboot_api.controllers.lecturer;

import com.example.springboot_api.dto.lecturer.ManualClassCreateRequest;
import com.example.springboot_api.dto.lecturer.ManualStudentAddRequest;
import com.example.springboot_api.dto.lecturer.ManualStudentAddResult;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.services.lecturer.ClassManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lecturer/manual-class-management")
@RequiredArgsConstructor
@Tag(name = "Manual Class Management", description = "APIs for manual class and student management")
public class ManualClassManagementController {
    
    private final ClassManagementService classManagementService;
    
    @PostMapping("/create-class")
    @Operation(summary = "Tạo lớp học phần thủ công", 
               description = "Tạo lớp học phần mới không cần file Excel, tự động tạo notebook cộng đồng")
    public ResponseEntity<?> createManualClass(
            @Parameter(description = "ID giảng viên", required = true)
            @RequestHeader("X-User-Id") UUID lecturerId,
            @Valid @RequestBody ManualClassCreateRequest request) {
        
        try {
            Class newClass = classManagementService.createManualClass(request, lecturerId);
            
            return ResponseEntity.ok().body(new CreateClassResponse(
                    true,
                    "Tạo lớp học phần thành công",
                    newClass.getId(),
                    newClass.getClassCode(),
                    newClass.getSubjectName()
            ));
            
        } catch (Exception e) {
            log.error("Lỗi tạo lớp học phần thủ công: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new CreateClassResponse(
                    false,
                    e.getMessage(),
                    null,
                    null,
                    null
            ));
        }
    }
    
    @PostMapping("/add-student")
    @Operation(summary = "Thêm sinh viên thủ công vào lớp", 
               description = "Thêm sinh viên vào lớp học phần, tự động tạo tài khoản và gửi email nếu chưa có")
    public ResponseEntity<ManualStudentAddResult> addManualStudent(
            @Valid @RequestBody ManualStudentAddRequest request) {
        
        try {
            ManualStudentAddResult result = classManagementService.addManualStudent(request);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("Lỗi thêm sinh viên thủ công: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ManualStudentAddResult.builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .userCreated(false)
                            .emailSent(false)
                            .build()
            );
        }
    }
    
    // Response DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateClassResponse {
        private boolean success;
        private String message;
        private UUID classId;
        private String className;
        private String subjectName;
    }
}