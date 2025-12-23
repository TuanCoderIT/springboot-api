package com.example.springboot_api.services.lecturer;

import com.example.springboot_api.dto.lecturer.*;
import com.example.springboot_api.models.ClassMember;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.Subject;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.admin.SubjectRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassManagementService {
    
    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ExcelReaderService excelReaderService;
    
    @Transactional
    public StudentImportResult createClassWithStudents(ClassImportRequest request, UUID lecturerId) {
        try {
            // 1. Đọc dữ liệu từ Excel
            List<StudentExcelData> studentsFromExcel = excelReaderService
                    .readStudentDataFromExcel(request.getExcelFile());
            
            // 2. Tạo lớp học phần
            com.example.springboot_api.models.Class newClass = createClass(request, lecturerId);
            
            // 3. Lấy notebook từ teaching assignment hoặc tạo mới nếu chưa có
            Notebook notebook = getOrCreateNotebookForClass(newClass, lecturerId);
            
            // 4. Import sinh viên vào lớp và notebook
            return importStudentsToClass(studentsFromExcel, newClass, notebook);
            
        } catch (Exception e) {
            log.error("Lỗi tạo lớp học phần: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo lớp học phần: " + e.getMessage());
        }
    }
    
    @Transactional
    public StudentImportResult importStudentsToExistingClass(StudentImportRequest request) {
        try {
            // 1. Kiểm tra lớp học phần tồn tại
            com.example.springboot_api.models.Class existingClass = classRepository.findByIdWithDetails(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần"));
            
            // 2. Lấy notebook từ teaching assignment
            Notebook notebook = existingClass.getTeachingAssignment().getNotebook();
            if (notebook == null) {
                throw new RuntimeException("Lớp học phần này chưa có notebook được liên kết");
            }
            
            // 3. Đọc dữ liệu từ Excel
            List<StudentExcelData> studentsFromExcel = excelReaderService
                    .readStudentDataFromExcel(request.getExcelFile());
            
            // 4. Import sinh viên
            return importStudentsToClass(studentsFromExcel, existingClass, notebook);
            
        } catch (Exception e) {
            log.error("Lỗi import sinh viên: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi import sinh viên: " + e.getMessage());
        }
    }
    
    private com.example.springboot_api.models.Class createClass(ClassImportRequest request, UUID lecturerId) {
        // Lấy thông tin teaching assignment và subject
        TeachingAssignment teachingAssignment = teachingAssignmentRepository
                .findById(request.getTeachingAssignmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công giảng dạy"));
        
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học"));
        
        com.example.springboot_api.models.Class newClass = com.example.springboot_api.models.Class.builder()
                .teachingAssignment(teachingAssignment)
                .classCode(request.getClassName())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .room(request.getRoom())
                .dayOfWeek(request.getDayOfWeek())
                .periods(request.getPeriods())
                .note(request.getNote())
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        
        return classRepository.save(newClass);
    }
    
    private Notebook getOrCreateNotebookForClass(com.example.springboot_api.models.Class classEntity, UUID lecturerId) {
        // Kiểm tra xem teaching assignment đã có notebook chưa
        Notebook existingNotebook = classEntity.getTeachingAssignment().getNotebook();
        if (existingNotebook != null) {
            return existingNotebook;
        }
        
        // Tạo notebook mới nếu chưa có
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));
        
        Notebook notebook = Notebook.builder()
                .title(classEntity.getSubjectName() + " - " + classEntity.getTeachingAssignment().getTerm().getName())
                .description("Notebook cho môn " + classEntity.getSubjectName())
                .type("community")
                .visibility("private")
                .createdBy(lecturer)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        
        notebook = notebookRepository.save(notebook);
        
        // Cập nhật teaching assignment với notebook mới
        classEntity.getTeachingAssignment().setNotebook(notebook);
        teachingAssignmentRepository.save(classEntity.getTeachingAssignment());
        
        // Thêm giảng viên làm owner của notebook
        NotebookMember lecturerMember = NotebookMember.builder()
                .notebook(notebook)
                .user(lecturer)
                .role("owner")
                .status("approved")
                .joinedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        
        notebookMemberRepository.save(lecturerMember);
        
        return notebook;
    }
    
    private StudentImportResult importStudentsToClass(List<StudentExcelData> studentsFromExcel, 
                                                     com.example.springboot_api.models.Class classEntity, Notebook notebook) {
        List<StudentImportResult.StudentImportError> duplicates = new ArrayList<>();
        List<StudentImportResult.StudentImportError> errors = new ArrayList<>();
        
        int successCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        
        UUID subjectId = classEntity.getTeachingAssignment().getSubject().getId();
        
        for (StudentExcelData studentData : studentsFromExcel) {
            try {
                // Kiểm tra trùng sinh viên trong cùng môn học
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
                
                // Tạo class member
                ClassMember classMember = ClassMember.builder()
                        .classField(classEntity)
                        .studentCode(studentData.getStudentCode())
                        .fullName(studentData.getFullName())
                        .firstName(studentData.getFirstName())
                        .lastName(studentData.getLastName())
                        .dob(studentData.getDateOfBirth())
                        .createdAt(OffsetDateTime.now())
                        .build();
                
                classMemberRepository.save(classMember);
                
                // Thêm vào notebook nếu sinh viên đã có tài khoản
                addStudentToNotebook(studentData.getStudentCode(), notebook);
                
                successCount++;
                
            } catch (Exception e) {
                errorCount++;
                log.error("Lỗi import sinh viên {}: {}", studentData.getStudentCode(), e.getMessage());
                errors.add(StudentImportResult.StudentImportError.builder()
                        .rowNumber(studentData.getRowNumber())
                        .studentCode(studentData.getStudentCode())
                        .fullName(studentData.getFullName())
                        .reason("Lỗi hệ thống: " + e.getMessage())
                        .build());
            }
        }
        
        return StudentImportResult.builder()
                .totalRows(studentsFromExcel.size())
                .successCount(successCount)
                .duplicateCount(duplicateCount)
                .errorCount(errorCount)
                .duplicates(duplicates)
                .errors(errors)
                .build();
    }
    
    private void addStudentToNotebook(String studentCode, Notebook notebook) {
        try {
            // Tìm user theo student code
            User student = userRepository.findByStudentCode(studentCode);
            if (student == null) {
                log.info("Sinh viên {} chưa có tài khoản, bỏ qua thêm vào notebook", studentCode);
                return;
            }
            
            // Kiểm tra đã là member chưa
            if (notebookMemberRepository.findByNotebookIdAndUserId(notebook.getId(), student.getId()).isPresent()) {
                log.info("Sinh viên {} đã là member của notebook", studentCode);
                return;
            }
            
            // Thêm vào notebook với role student
            NotebookMember notebookMember = NotebookMember.builder()
                    .notebook(notebook)
                    .user(student)
                    .role("student")
                    .status("approved")
                    .joinedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            
            notebookMemberRepository.save(notebookMember);
            log.info("Đã thêm sinh viên {} vào notebook", studentCode);
            
        } catch (Exception e) {
            log.error("Lỗi thêm sinh viên {} vào notebook: {}", studentCode, e.getMessage());
        }
    }
}