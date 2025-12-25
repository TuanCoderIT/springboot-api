package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.*;
import com.example.springboot_api.models.exam.Exam;
import com.example.springboot_api.models.exam.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ExamService {
    
    /**
     * Tạo kỳ thi mới
     */
    ExamResponse createExam(CreateExamRequest request, UUID lecturerId);
    
    /**
     * Sinh câu hỏi cho kỳ thi từ notebook files
     */
    ExamResponse generateQuestions(UUID examId, GenerateQuestionsRequest request, UUID lecturerId);
    
    /**
     * Xuất bản kỳ thi (chuyển từ DRAFT sang PUBLISHED)
     */
    ExamResponse publishExam(UUID examId, UUID lecturerId);
    
    /**
     * Kích hoạt kỳ thi (chuyển từ PUBLISHED sang ACTIVE)
     */
    ExamResponse activateExam(UUID examId, UUID lecturerId);
    
    /**
     * Hủy kỳ thi
     */
    ExamResponse cancelExam(UUID examId, UUID lecturerId);
    
    /**
     * Lấy danh sách kỳ thi theo lớp học phần
     */
    Page<ExamResponse> getExamsByClass(UUID classId, UUID lecturerId, Pageable pageable);
    
    /**
     * Lấy danh sách kỳ thi của giảng viên
     */
    Page<ExamResponse> getExamsByLecturer(UUID lecturerId, Pageable pageable);
    
    /**
     * Lấy chi tiết kỳ thi
     */
    ExamResponse getExamById(UUID examId, UUID userId);
    
    /**
     * Xem trước kỳ thi với câu hỏi, đáp án và thông tin chấm điểm (chỉ dành cho giảng viên)
     */
    ExamPreviewResponse previewExam(UUID examId, UUID lecturerId);
    
    /**
     * Lấy danh sách kỳ thi có thể tham gia cho sinh viên
     */
    List<ExamResponse> getAvailableExamsForStudent(String studentCode);
    
    /**
     * Kiểm tra sinh viên có thể tham gia kỳ thi không
     */
    boolean canStudentTakeExam(UUID examId, String studentCode);
    
    /**
     * Bắt đầu làm bài thi
     */
    ExamAttemptResponse startExam(UUID examId, StartExamRequest request, String studentCode);
    
    /**
     * Nộp bài thi
     */
    ExamResultResponse submitExam(SubmitExamRequest request, String studentCode);
    
    /**
     * Tự động nộp bài khi hết giờ
     */
    void autoSubmitExpiredAttempts();
    
    /**
     * Lấy kết quả thi
     */
    ExamResultResponse getExamResult(UUID examId, String studentCode);
    
    /**
     * Lấy tất cả kết quả của kỳ thi (cho giảng viên)
     */
    Page<ExamResultResponse> getExamResults(UUID examId, UUID lecturerId, Pageable pageable);
    
    /**
     * Cập nhật trạng thái kỳ thi tự động
     */
    void updateExamStatuses();
    
    /**
     * Xóa kỳ thi
     */
    void deleteExam(UUID examId, UUID lecturerId);
}