package com.example.springboot_api.services.lecturer;

import com.example.springboot_api.dto.lecturer.StudentExcelData;
import com.example.springboot_api.dto.lecturer.StudentImportResult;
import com.example.springboot_api.models.ClassMember;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelPreviewService {
    
    private final ExcelReaderService excelReaderService;
    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    
    public StudentImportResult previewExcelData(MultipartFile excelFile, UUID classId) {
        try {
            // 1. Đọc dữ liệu từ Excel
            List<StudentExcelData> studentsFromExcel = excelReaderService
                    .readStudentDataFromExcel(excelFile);
            
            List<StudentImportResult.StudentImportError> duplicates = new ArrayList<>();
            List<StudentImportResult.StudentImportError> errors = new ArrayList<>();
            
            int validCount = 0;
            int duplicateCount = 0;
            int errorCount = 0;
            
            UUID subjectId = null;
            if (classId != null) {
                // Lấy subject ID từ class để check trùng
                var classEntity = classRepository.findByIdWithDetails(classId);
                if (classEntity.isPresent()) {
                    subjectId = classEntity.get().getTeachingAssignment().getSubject().getId();
                }
            }
            
            for (StudentExcelData studentData : studentsFromExcel) {
                try {
                    // Validate dữ liệu cơ bản
                    if (studentData.getStudentCode() == null || studentData.getStudentCode().trim().isEmpty()) {
                        errorCount++;
                        errors.add(StudentImportResult.StudentImportError.builder()
                                .rowNumber(studentData.getRowNumber())
                                .studentCode(studentData.getStudentCode())
                                .fullName(studentData.getFullName())
                                .reason("Mã sinh viên không được để trống")
                                .build());
                        continue;
                    }
                    
                    if (studentData.getFullName() == null || studentData.getFullName().trim().isEmpty()) {
                        errorCount++;
                        errors.add(StudentImportResult.StudentImportError.builder()
                                .rowNumber(studentData.getRowNumber())
                                .studentCode(studentData.getStudentCode())
                                .fullName(studentData.getFullName())
                                .reason("Họ tên không được để trống")
                                .build());
                        continue;
                    }
                    
                    // Kiểm tra trùng nếu có classId
                    if (subjectId != null) {
                        List<ClassMember> existingMembers = classMemberRepository
                                .findByStudentCodeAndSubjectId(studentData.getStudentCode(), subjectId);
                        
                        if (!existingMembers.isEmpty()) {
                            duplicateCount++;
                            duplicates.add(StudentImportResult.StudentImportError.builder()
                                    .rowNumber(studentData.getRowNumber())
                                    .studentCode(studentData.getStudentCode())
                                    .fullName(studentData.getFullName())
                                    .reason("Sinh viên đã tồn tại trong môn học này")
                                    .build());
                            continue;
                        }
                    }
                    
                    validCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    log.error("Lỗi validate sinh viên {}: {}", studentData.getStudentCode(), e.getMessage());
                    errors.add(StudentImportResult.StudentImportError.builder()
                            .rowNumber(studentData.getRowNumber())
                            .studentCode(studentData.getStudentCode())
                            .fullName(studentData.getFullName())
                            .reason("Lỗi validate: " + e.getMessage())
                            .build());
                }
            }
            
            return StudentImportResult.builder()
                    .totalRows(studentsFromExcel.size())
                    .successCount(validCount)
                    .duplicateCount(duplicateCount)
                    .errorCount(errorCount)
                    .duplicates(duplicates)
                    .errors(errors)
                    .build();
            
        } catch (Exception e) {
            log.error("Lỗi preview Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi preview Excel: " + e.getMessage());
        }
    }
}