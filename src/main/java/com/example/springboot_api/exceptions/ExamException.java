package com.example.springboot_api.exceptions;

/**
 * Custom exception for exam-related errors
 */
public class ExamException extends RuntimeException {
    
    public ExamException(String message) {
        super(message);
    }
    
    public ExamException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Specific exam exception types
    
    public static class ExamNotFound extends ExamException {
        public ExamNotFound(String examId) {
            super("Exam not found: " + examId);
        }
    }
    
    public static class ExamNotActive extends ExamException {
        public ExamNotActive(String examId) {
            super("Exam is not active: " + examId);
        }
    }
    
    public static class StudentNotEligible extends ExamException {
        public StudentNotEligible(String studentCode, String examId) {
            super("Student " + studentCode + " is not eligible for exam " + examId);
        }
    }
    
    public static class AttemptLimitExceeded extends ExamException {
        public AttemptLimitExceeded(String studentCode, String examId) {
            super("Student " + studentCode + " has exceeded attempt limit for exam " + examId);
        }
    }
    
    public static class ExamTimeExpired extends ExamException {
        public ExamTimeExpired(String examId) {
            super("Exam time has expired: " + examId);
        }
    }
    
    public static class AttemptNotFound extends ExamException {
        public AttemptNotFound(String attemptId) {
            super("Exam attempt not found: " + attemptId);
        }
    }
    
    public static class AttemptNotInProgress extends ExamException {
        public AttemptNotInProgress(String attemptId) {
            super("Exam attempt is not in progress: " + attemptId);
        }
    }
    
    public static class UnauthorizedAccess extends ExamException {
        public UnauthorizedAccess(String message) {
            super("Unauthorized access: " + message);
        }
    }
    
    public static class InvalidExamState extends ExamException {
        public InvalidExamState(String message) {
            super("Invalid exam state: " + message);
        }
    }
    
    public static class QuestionGenerationFailed extends ExamException {
        public QuestionGenerationFailed(String message) {
            super("Question generation failed: " + message);
        }
        
        public QuestionGenerationFailed(String message, Throwable cause) {
            super("Question generation failed: " + message, cause);
        }
    }
}