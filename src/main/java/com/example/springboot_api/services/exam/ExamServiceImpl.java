package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.*;
import com.example.springboot_api.models.*;
import com.example.springboot_api.models.exam.*;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import com.example.springboot_api.repositories.exam.ExamRepository;
import com.example.springboot_api.repositories.exam.ExamQuestionRepository;
import com.example.springboot_api.repositories.exam.ExamAttemptRepository;
import com.example.springboot_api.repositories.exam.ExamAnswerRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExamServiceImpl implements ExamService {
    
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ClassMemberRepository classMemberRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final QuestionGenerationService questionGenerationService;
    private final ObjectMapper objectMapper;
    
    @Override
    public ExamResponse createExam(CreateExamRequest request, UUID lecturerId) {
        log.info("Creating exam for class {} by lecturer {}", request.getClassId(), lecturerId);
        
        // Validate class exists and lecturer has permission
        com.example.springboot_api.models.Class classEntity = classRepository.findById(request.getClassId())
            .orElseThrow(() -> new RuntimeException("Class not found"));
        
        User lecturer = userRepository.findById(lecturerId)
            .orElseThrow(() -> new RuntimeException("Lecturer not found"));
        
        // Check if lecturer teaches this class
        if (!isLecturerTeachingClass(lecturerId, request.getClassId())) {
            throw new RuntimeException("Lecturer does not have permission to create exam for this class");
        }
        
        // Create exam
        Exam exam = new Exam();
        exam.setClassEntity(classEntity);
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setPassingScore(request.getPassingScore());
        exam.setShuffleQuestions(request.getShuffleQuestions());
        exam.setShuffleOptions(request.getShuffleOptions());
        exam.setShowResultsImmediately(request.getShowResultsImmediately());
        exam.setAllowReview(request.getAllowReview());
        exam.setMaxAttempts(request.getMaxAttempts());
        exam.setEnableProctoring(request.getEnableProctoring());
        exam.setEnableLockdown(request.getEnableLockdown());
        exam.setEnablePlagiarismCheck(request.getEnablePlagiarismCheck());
        exam.setCreatedBy(lecturer);
        exam.setStatus(ExamStatus.DRAFT);
        
        exam = examRepository.save(exam);
        
        log.info("Created exam {} successfully", exam.getId());
        return mapToExamResponse(exam, lecturerId);
    }
    
    @Override
    public ExamResponse generateQuestions(UUID examId, GenerateQuestionsRequest request, UUID lecturerId) {
        log.info("Generating questions for exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can generate questions");
        }
        
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Can only generate questions for draft exams");
        }
        
        // Validate notebook files exist
        List<NotebookFile> notebookFiles = notebookFileRepository.findAllById(request.getNotebookFileIds());
        if (notebookFiles.size() != request.getNotebookFileIds().size()) {
            throw new RuntimeException("Some notebook files not found");
        }
        
        // Clear existing questions
        examQuestionRepository.deleteByExamId(examId);
        
        try {
            // Generate questions using AI service
            List<ExamQuestion> generatedQuestions = questionGenerationService.generateQuestions(
                exam, notebookFiles, request);
            
            // Save questions
            examQuestionRepository.saveAll(generatedQuestions);
            
            // Update exam totals
            updateExamTotals(exam);
            
            log.info("Generated {} questions for exam {}", generatedQuestions.size(), examId);
            return mapToExamResponse(exam, lecturerId);
            
        } catch (Exception e) {
            log.error("Error generating questions for exam {}: {}", examId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate questions: " + e.getMessage());
        }
    }
    
    @Override
    public ExamResponse publishExam(UUID examId, UUID lecturerId) {
        log.info("Publishing exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can publish exam");
        }
        
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Can only publish draft exams");
        }
        
        // Validate exam has questions
        long questionCount = examQuestionRepository.countByExamId(examId);
        if (questionCount == 0) {
            throw new RuntimeException("Cannot publish exam without questions");
        }
        
        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);
        
        log.info("Published exam {} successfully", examId);
        return mapToExamResponse(exam, lecturerId);
    }
    
    @Override
    public ExamResponse activateExam(UUID examId, UUID lecturerId) {
        log.info("Activating exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can activate exam");
        }
        
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Can only activate published exams");
        }
        
        exam.setStatus(ExamStatus.ACTIVE);
        exam = examRepository.save(exam);
        
        log.info("Activated exam {} successfully", examId);
        return mapToExamResponse(exam, lecturerId);
    }
    
    @Override
    public ExamResponse cancelExam(UUID examId, UUID lecturerId) {
        log.info("Cancelling exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can cancel exam");
        }
        
        exam.setStatus(ExamStatus.CANCELLED);
        exam = examRepository.save(exam);
        
        log.info("Cancelled exam {} successfully", examId);
        return mapToExamResponse(exam, lecturerId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> getExamsByClass(UUID classId, UUID lecturerId, Pageable pageable) {
        Page<Exam> exams = examRepository.findByClassIdAndLecturerId(classId, lecturerId, pageable);
        List<ExamResponse> responses = exams.getContent().stream()
            .map(exam -> mapToExamResponse(exam, lecturerId))
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, exams.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> getExamsByLecturer(UUID lecturerId, Pageable pageable) {
        Page<Exam> exams = examRepository.findByLecturerId(lecturerId, pageable);
        List<ExamResponse> responses = exams.getContent().stream()
            .map(exam -> mapToExamResponse(exam, lecturerId))
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, exams.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamById(UUID examId, UUID userId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        return mapToExamResponse(exam, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExamPreviewResponse previewExam(UUID examId, UUID lecturerId) {
        log.info("Previewing exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions - only exam creator can preview
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can preview exam");
        }
        
        // Load questions with options
        List<ExamQuestion> questions = examQuestionRepository.findByExamIdWithOptions(examId);
        
        return mapToExamPreviewResponse(exam, questions);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExamResponse> getAvailableExamsForStudent(String studentCode) {
        List<Exam> exams = examRepository.findAvailableExamsForStudent(studentCode);
        System.out.println("Found " + exams.size() + " exams for student " + studentCode);
        
        return exams.stream()
            .map(exam -> mapToExamResponseForStudent(exam, studentCode))
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canStudentTakeExam(UUID examId, String studentCode) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Check if exam is active and within time window
        if (!exam.canStudentTakeExam()) {
            return false;
        }
        
        // Check if student is in class
        if (!examRepository.isStudentInClass(exam.getClassEntity().getId(), studentCode)) {
            return false;
        }
        
        // Check attempt limits
        long attemptCount = examAttemptRepository.countByExamIdAndStudentCode(examId, studentCode);
        return attemptCount < exam.getMaxAttempts();
    }
    
    @Override
    public ExamAttemptResponse startExam(UUID examId, StartExamRequest request, String studentCode) {
        log.info("Starting exam {} for student {}", examId, studentCode);
        
        // Fetch exam with questions only (options will be fetched separately when needed)
        Exam exam = examRepository.findByIdWithQuestions(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate student can take exam
        if (!canStudentTakeExam(examId, studentCode)) {
            throw new RuntimeException("Student cannot take this exam");
        }
        
        // Check if student already has an active attempt
        Optional<ExamAttempt> existingAttempt = examAttemptRepository.findCurrentAttemptByStudentCode(examId, studentCode);
        if (existingAttempt.isPresent()) {
            return mapToExamAttemptResponse(existingAttempt.get());
        }
        
        // Get student
        User student = userRepository.findByStudentCode(studentCode);
        if (student == null) {
            throw new RuntimeException("Student not found");
        }
        
        // Create new attempt
        ExamAttempt attempt = createExamAttempt(exam, student, request);
        attempt = examAttemptRepository.save(attempt);
        
        log.info("Started exam attempt {} for student {}", attempt.getId(), studentCode);
        return mapToExamAttemptResponse(attempt);
    }
    
    @Override
    public ExamResultResponse submitExam(SubmitExamRequest request, String studentCode) {
        log.info("Submitting exam attempt {} for student {}", request.getAttemptId(), studentCode);
        
        ExamAttempt attempt = examAttemptRepository.findByIdWithAnswers(request.getAttemptId())
            .orElseThrow(() -> new RuntimeException("Exam attempt not found"));
        
        // Validate student owns this attempt
        if (!attempt.getStudent().getStudentCode().equals(studentCode)) {
            throw new RuntimeException("Student does not own this attempt");
        }
        
        if (!attempt.isInProgress()) {
            throw new RuntimeException("Attempt is not in progress");
        }
        
        // Save answers
        saveExamAnswers(attempt, request.getAnswers());
        
        // Submit attempt
        attempt.submit(request.getIsAutoSubmit());
        attempt = examAttemptRepository.save(attempt);
        
        // Grade attempt
        gradeAttempt(attempt);
        
        log.info("Submitted and graded exam attempt {} for student {}", request.getAttemptId(), studentCode);
        return mapToExamResultResponse(attempt);
    }
    
    @Override
    public void autoSubmitExpiredAttempts() {
        log.info("Auto-submitting expired attempts");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(8); // Safety buffer
        List<ExamAttempt> expiredAttempts = examAttemptRepository.findOverdueAttempts(cutoffTime);
        
        for (ExamAttempt attempt : expiredAttempts) {
            try {
                if (attempt.isTimeUp(attempt.getExam().getDurationMinutes())) {
                    attempt.submit(true);
                    examAttemptRepository.save(attempt);
                    gradeAttempt(attempt);
                    log.info("Auto-submitted expired attempt {}", attempt.getId());
                }
            } catch (Exception e) {
                log.error("Error auto-submitting attempt {}: {}", attempt.getId(), e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExamResultResponse getExamResult(UUID examId, String studentCode) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentCode(examId, studentCode);
        
        if (attempts.isEmpty()) {
            throw new RuntimeException("No exam attempts found");
        }
        
        // Get best attempt (highest score)
        ExamAttempt bestAttempt = attempts.stream()
            .filter(a -> a.getStatus() == AttemptStatus.GRADED)
            .max(Comparator.comparing(ExamAttempt::getTotalScore))
            .orElse(attempts.get(0));
        
        return mapToExamResultResponse(bestAttempt);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExamResultResponse> getExamResults(UUID examId, UUID lecturerId, Pageable pageable) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can view results");
        }
        
        Page<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId, pageable);
        List<ExamResultResponse> responses = attempts.getContent().stream()
            .map(this::mapToExamResultResponse)
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, attempts.getTotalElements());
    }
    
    @Override
    public void updateExamStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        // Activate published exams that should start
        List<Exam> toActivate = examRepository.findUpcomingExams(now, now.plusMinutes(5));
        for (Exam exam : toActivate) {
            if (exam.getStartTime().isBefore(now) || exam.getStartTime().isEqual(now)) {
                exam.setStatus(ExamStatus.ACTIVE);
                examRepository.save(exam);
                log.info("Auto-activated exam {}", exam.getId());
            }
        }
        
        // Complete active exams that have ended
        List<Exam> toComplete = examRepository.findExpiredActiveExams(now);
        for (Exam exam : toComplete) {
            exam.setStatus(ExamStatus.COMPLETED);
            examRepository.save(exam);
            log.info("Auto-completed exam {}", exam.getId());
        }
    }
    
    @Override
    public void deleteExam(UUID examId, UUID lecturerId) {
        log.info("Deleting exam {} by lecturer {}", examId, lecturerId);
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        // Validate permissions
        if (!exam.getCreatedBy().getId().equals(lecturerId)) {
            throw new RuntimeException("Only exam creator can delete exam");
        }
        
        // Can only delete draft exams or exams with no attempts
        if (exam.getStatus() != ExamStatus.DRAFT) {
            long attemptCount = examAttemptRepository.countByExamIdAndStudentId(examId, null);
            if (attemptCount > 0) {
                throw new RuntimeException("Cannot delete exam with existing attempts");
            }
        }
        
        examRepository.delete(exam);
        log.info("Deleted exam {} successfully", examId);
    }
    
    // Helper methods
    
    private boolean isLecturerTeachingClass(UUID lecturerId, UUID classId) {
        // Implementation depends on your class-lecturer relationship
        // For now, assume all lecturers can create exams for any class
        return true;
    }
    
    private void updateExamTotals(Exam exam) {
        Long questionCount = examQuestionRepository.countByExamId(exam.getId());
        Double totalPoints = examQuestionRepository.sumPointsByExamId(exam.getId());
        
        exam.setTotalQuestions(questionCount.intValue());
        exam.setTotalPoints(BigDecimal.valueOf(totalPoints != null ? totalPoints : 0));
        
        examRepository.save(exam);
    }
    
    private ExamAttempt createExamAttempt(Exam exam, User student, StartExamRequest request) {
        // Get attempt number
        long attemptCount = examAttemptRepository.countByExamIdAndStudentId(exam.getId(), student.getId());
        
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setStudent(student);
        attempt.setAttemptNumber((int) attemptCount + 1);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        
        // Create snapshots
        try {
            attempt.setExamSnapshot(objectMapper.writeValueAsString(createExamSnapshot(exam)));
            attempt.setQuestionsSnapshot(objectMapper.writeValueAsString(createQuestionsSnapshot(exam)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create exam snapshots", e);
        }
        
        // Set browser info
        Map<String, Object> browserInfo = new HashMap<>();
        browserInfo.put("browserName", request.getBrowserName());
        browserInfo.put("browserVersion", request.getBrowserVersion());
        browserInfo.put("operatingSystem", request.getOperatingSystem());
        browserInfo.put("screenResolution", request.getScreenResolution());
        browserInfo.put("deviceType", request.getDeviceType());
        
        try {
            attempt.setBrowserInfo(objectMapper.writeValueAsString(browserInfo));
        } catch (JsonProcessingException e) {
            attempt.setBrowserInfo("{}");
        }
        
        attempt.setIpAddress(request.getIpAddress());
        attempt.setUserAgent(request.getUserAgent());
        
        return attempt;
    }
    
    private Map<String, Object> createExamSnapshot(Exam exam) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", exam.getId());
        snapshot.put("title", exam.getTitle());
        snapshot.put("durationMinutes", exam.getDurationMinutes());
        snapshot.put("totalQuestions", exam.getTotalQuestions());
        snapshot.put("totalPoints", exam.getTotalPoints());
        snapshot.put("shuffleQuestions", exam.getShuffleQuestions());
        snapshot.put("shuffleOptions", exam.getShuffleOptions());
        return snapshot;
    }
    
    private List<Map<String, Object>> createQuestionsSnapshot(Exam exam) {
        List<ExamQuestion> questions = examQuestionRepository.findByExamIdWithOptions(exam.getId());
        return questions.stream().map(this::mapQuestionToSnapshot).collect(Collectors.toList());
    }
    
    private Map<String, Object> mapQuestionToSnapshot(ExamQuestion question) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", question.getId());
        snapshot.put("questionType", question.getQuestionType());
        snapshot.put("questionText", question.getQuestionText());
        snapshot.put("points", question.getPoints());
        snapshot.put("orderIndex", question.getOrderIndex());
        
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<Map<String, Object>> options = question.getOptions().stream()
                .map(this::mapOptionToSnapshot)
                .collect(Collectors.toList());
            snapshot.put("options", options);
        }
        
        return snapshot;
    }
    
    private Map<String, Object> mapOptionToSnapshot(ExamQuestionOption option) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", option.getId());
        snapshot.put("optionText", option.getOptionText());
        snapshot.put("orderIndex", option.getOrderIndex());
        snapshot.put("isCorrect", option.getIsCorrect());
        return snapshot;
    }
    
    private void saveExamAnswers(ExamAttempt attempt, List<SubmitExamRequest.SubmitAnswerRequest> answers) {
        for (SubmitExamRequest.SubmitAnswerRequest answerRequest : answers) {
            ExamQuestion question = examQuestionRepository.findById(answerRequest.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found: " + answerRequest.getQuestionId()));
            
            ExamAnswer answer = examAnswerRepository.findByAttemptIdAndQuestionId(
                attempt.getId(), answerRequest.getQuestionId())
                .orElse(new ExamAnswer());
            
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setAnswerType(AnswerType.valueOf(question.getQuestionType().name()));
            
            try {
                answer.setAnswerData(objectMapper.writeValueAsString(answerRequest.getAnswerData()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize answer data", e);
            }
            
            answer.setTimeSpentSeconds(answerRequest.getTimeSpentSeconds());
            
            examAnswerRepository.save(answer);
        }
    }
    
    private void gradeAttempt(ExamAttempt attempt) {
        List<ExamAnswer> answers = examAnswerRepository.findByAttemptIdWithQuestion(attempt.getId());
        BigDecimal totalScore = BigDecimal.ZERO;
        
        for (ExamAnswer answer : answers) {
            if (answer.isAutoGradable()) {
                boolean isCorrect = gradeAnswer(answer);
                BigDecimal points = isCorrect ? answer.getQuestion().getPoints() : BigDecimal.ZERO;
                answer.autoGrade(isCorrect, points);
                totalScore = totalScore.add(points);
                examAnswerRepository.save(answer);
            }
        }
        
        // Update attempt with final score
        attempt.grade(totalScore, attempt.getExam().getTotalPoints());
        
        // Check if passed
        attempt.setIsPassed(totalScore.compareTo(attempt.getExam().getPassingScore()) >= 0);
        
        examAttemptRepository.save(attempt);
    }
    
    private boolean gradeAnswer(ExamAnswer answer) {
        try {
            if (answer.isMCQ()) {
                return gradeMCQAnswer(answer);
            } else if (answer.isTrueFalse()) {
                return gradeTrueFalseAnswer(answer);
            }
            return false;
        } catch (Exception e) {
            log.error("Error grading answer {}: {}", answer.getId(), e.getMessage());
            return false;
        }
    }
    
    private boolean gradeMCQAnswer(ExamAnswer answer) throws JsonProcessingException {
        Map<String, Object> answerData = objectMapper.readValue(answer.getAnswerData(), Map.class);
        String selectedOptionId = (String) answerData.get("selectedOptionId");
        
        if (selectedOptionId == null) return false;
        
        // Find the correct option from question snapshot
        String questionsSnapshot = answer.getAttempt().getQuestionsSnapshot();
        List<Map<String, Object>> questions = objectMapper.readValue(questionsSnapshot, List.class);
        
        for (Map<String, Object> question : questions) {
            if (answer.getQuestion().getId().toString().equals(question.get("id").toString())) {
                List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
                if (options != null) {
                    for (Map<String, Object> option : options) {
                        if (selectedOptionId.equals(option.get("id").toString())) {
                            return Boolean.TRUE.equals(option.get("isCorrect"));
                        }
                    }
                }
                break;
            }
        }
        
        return false;
    }
    
    private boolean gradeTrueFalseAnswer(ExamAnswer answer) throws JsonProcessingException {
        Map<String, Object> answerData = objectMapper.readValue(answer.getAnswerData(), Map.class);
        Boolean selectedAnswer = (Boolean) answerData.get("answer");
        
        if (selectedAnswer == null) return false;
        
        // Get correct answer from question
        String correctAnswerJson = answer.getQuestion().getCorrectAnswer();
        if (correctAnswerJson != null) {
            Map<String, Object> correctAnswer = objectMapper.readValue(correctAnswerJson, Map.class);
            Boolean correctValue = (Boolean) correctAnswer.get("answer");
            return selectedAnswer.equals(correctValue);
        }
        
        return false;
    }
    
    // Mapping methods
    
    private ExamResponse mapToExamResponse(Exam exam, UUID userId) {
        ExamResponse response = new ExamResponse();
        response.setId(exam.getId());
        response.setClassId(exam.getClassEntity().getId());
        response.setClassName(exam.getClassEntity().getClassCode());
        response.setSubjectCode(exam.getClassEntity().getSubjectCode());
        response.setSubjectName(exam.getClassEntity().getSubjectName());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setDurationMinutes(exam.getDurationMinutes());
        response.setTotalQuestions(exam.getTotalQuestions());
        response.setTotalPoints(exam.getTotalPoints());
        response.setPassingScore(exam.getPassingScore());
        response.setMaxAttempts(exam.getMaxAttempts());
        response.setShuffleQuestions(exam.getShuffleQuestions());
        response.setShuffleOptions(exam.getShuffleOptions());
        response.setShowResultsImmediately(exam.getShowResultsImmediately());
        response.setAllowReview(exam.getAllowReview());
        response.setEnableProctoring(exam.getEnableProctoring());
        response.setEnableLockdown(exam.getEnableLockdown());
        response.setEnablePlagiarismCheck(exam.getEnablePlagiarismCheck());
        response.setStatus(exam.getStatus());
        response.setCreatedById(exam.getCreatedBy().getId());
        response.setCreatedByName(exam.getCreatedBy().getFullName());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        response.setCanTakeExam(exam.canStudentTakeExam());
        response.setIsActive(exam.isActive());
        response.setIsTimeUp(exam.isTimeUp());
        
        return response;
    }
    
    private ExamResponse mapToExamResponseForStudent(Exam exam, String studentCode) {
        ExamResponse response = mapToExamResponse(exam, null);
        
        // Add student-specific information
        long attemptCount = examAttemptRepository.countByExamIdAndStudentCode(exam.getId(), studentCode);
        response.setRemainingAttempts(exam.getMaxAttempts() - (int) attemptCount);
        response.setCanTakeExam(canStudentTakeExam(exam.getId(), studentCode));
        
        return response;
    }
    
    private ExamAttemptResponse mapToExamAttemptResponse(ExamAttempt attempt) {
        ExamAttemptResponse response = new ExamAttemptResponse();
        response.setAttemptId(attempt.getId());
        response.setExamId(attempt.getExam().getId());
        response.setExamTitle(attempt.getExam().getTitle());
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setStatus(attempt.getStatus());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setTimeSpentSeconds(attempt.getTimeSpentSeconds());
        response.setDurationMinutes(attempt.getExam().getDurationMinutes());
        response.setShuffleQuestions(attempt.getExam().getShuffleQuestions());
        response.setShuffleOptions(attempt.getExam().getShuffleOptions());
        response.setAllowReview(attempt.getExam().getAllowReview());
        response.setStudentCode(attempt.getStudent().getStudentCode());
        response.setStudentName(attempt.getStudent().getFullName());
        response.setIsTimeUp(attempt.isTimeUp(attempt.getExam().getDurationMinutes()));
        response.setCanSubmit(attempt.isInProgress());
        response.setAutoSubmitEnabled(true);
        
        // Calculate remaining time
        if (attempt.isInProgress() && attempt.getStartedAt() != null) {
            int elapsedMinutes = (int) java.time.Duration.between(attempt.getStartedAt(), LocalDateTime.now()).toMinutes();
            int remainingMinutes = Math.max(0, attempt.getExam().getDurationMinutes() - elapsedMinutes);
            response.setRemainingTimeSeconds(remainingMinutes * 60);
        }
        
        // Load questions from snapshot
        try {
            String questionsSnapshot = attempt.getQuestionsSnapshot();
            List<Map<String, Object>> questionMaps = objectMapper.readValue(questionsSnapshot, List.class);
            List<ExamAttemptResponse.ExamQuestionResponse> questions = questionMaps.stream()
                .map(this::mapSnapshotToQuestionResponse)
                .collect(Collectors.toList());
            response.setQuestions(questions);
        } catch (JsonProcessingException e) {
            log.error("Error parsing questions snapshot for attempt {}: {}", attempt.getId(), e.getMessage());
            response.setQuestions(new ArrayList<>());
        }
        
        return response;
    }
    
    private ExamAttemptResponse.ExamQuestionResponse mapSnapshotToQuestionResponse(Map<String, Object> questionMap) {
        ExamAttemptResponse.ExamQuestionResponse question = new ExamAttemptResponse.ExamQuestionResponse();
        question.setQuestionId(UUID.fromString(questionMap.get("id").toString()));
        question.setQuestionType(questionMap.get("questionType").toString());
        question.setQuestionText(questionMap.get("questionText").toString());
        question.setOrderIndex((Integer) questionMap.get("orderIndex"));
        question.setPoints(((Number) questionMap.get("points")).doubleValue());
        
        // Map options if present
        List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) questionMap.get("options");
        if (optionMaps != null) {
            List<ExamAttemptResponse.ExamQuestionResponse.QuestionOptionResponse> options = optionMaps.stream()
                .map(this::mapSnapshotToOptionResponse)
                .collect(Collectors.toList());
            question.setOptions(options);
        }
        
        return question;
    }
    
    private ExamAttemptResponse.ExamQuestionResponse.QuestionOptionResponse mapSnapshotToOptionResponse(Map<String, Object> optionMap) {
        ExamAttemptResponse.ExamQuestionResponse.QuestionOptionResponse option = 
            new ExamAttemptResponse.ExamQuestionResponse.QuestionOptionResponse();
        option.setOptionId(UUID.fromString(optionMap.get("id").toString()));
        option.setOptionText(optionMap.get("optionText").toString());
        option.setOrderIndex((Integer) optionMap.get("orderIndex"));
        // Don't include isCorrect for students
        return option;
    }
    
    private ExamResultResponse mapToExamResultResponse(ExamAttempt attempt) {
        ExamResultResponse response = new ExamResultResponse();
        response.setAttemptId(attempt.getId());
        response.setExamId(attempt.getExam().getId());
        response.setExamTitle(attempt.getExam().getTitle());
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setStatus(attempt.getStatus());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setTimeSpentSeconds(attempt.getTimeSpentSeconds());
        response.setTimeSpentFormatted(formatDuration(attempt.getTimeSpentSeconds()));
        response.setTotalScore(attempt.getTotalScore());
        response.setTotalPossibleScore(attempt.getExam().getTotalPoints());
        response.setPercentageScore(attempt.getPercentageScore());
        response.setIsPassed(attempt.getIsPassed());
        response.setStudentId(attempt.getStudent().getId());
        response.setStudentCode(attempt.getStudent().getStudentCode());
        response.setStudentName(attempt.getStudent().getFullName());
        response.setShowDetailedResults(attempt.getExam().getShowResultsImmediately());
        response.setAllowReview(attempt.getExam().getAllowReview());
        
        // Calculate question statistics
        List<ExamAnswer> answers = examAnswerRepository.findByAttemptIdWithQuestion(attempt.getId());
        response.setTotalQuestions(attempt.getExam().getTotalQuestions());
        response.setAnsweredQuestions((int) answers.stream().filter(a -> a.getAnswerData() != null).count());
        response.setCorrectAnswers((int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count());
        response.setIncorrectAnswers((int) answers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count());
        response.setSkippedQuestions(response.getTotalQuestions() - response.getAnsweredQuestions());
        
        return response;
    }
    
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) return "0s";
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");
        
        return sb.toString().trim();
    }
    
    private ExamPreviewResponse mapToExamPreviewResponse(Exam exam, List<ExamQuestion> questions) {
        ExamPreviewResponse response = new ExamPreviewResponse();
        
        // Map basic exam information
        response.setId(exam.getId());
        response.setClassId(exam.getClassEntity().getId());
        response.setClassName(exam.getClassEntity().getClassCode());
        response.setSubjectCode(exam.getClassEntity().getSubjectCode());
        response.setSubjectName(exam.getClassEntity().getSubjectName());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setDurationMinutes(exam.getDurationMinutes());
        response.setTotalQuestions(exam.getTotalQuestions());
        response.setTotalPoints(exam.getTotalPoints());
        response.setPassingScore(exam.getPassingScore());
        response.setMaxAttempts(exam.getMaxAttempts());
        response.setShuffleQuestions(exam.getShuffleQuestions());
        response.setShuffleOptions(exam.getShuffleOptions());
        response.setShowResultsImmediately(exam.getShowResultsImmediately());
        response.setAllowReview(exam.getAllowReview());
        response.setEnableProctoring(exam.getEnableProctoring());
        response.setEnableLockdown(exam.getEnableLockdown());
        response.setEnablePlagiarismCheck(exam.getEnablePlagiarismCheck());
        response.setStatus(exam.getStatus());
        response.setCreatedById(exam.getCreatedBy().getId());
        response.setCreatedByName(exam.getCreatedBy().getFullName());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        
        // Map questions with answers and scoring information
        List<ExamPreviewResponse.QuestionPreview> questionPreviews = questions.stream()
            .map(this::mapToQuestionPreview)
            .collect(Collectors.toList());
        response.setQuestions(questionPreviews);
        
        return response;
    }
    
    private ExamPreviewResponse.QuestionPreview mapToQuestionPreview(ExamQuestion question) {
        ExamPreviewResponse.QuestionPreview preview = new ExamPreviewResponse.QuestionPreview();
        preview.setId(question.getId());
        preview.setQuestionType(question.getQuestionType());
        preview.setQuestionText(question.getQuestionText());
        preview.setQuestionImageUrl(question.getQuestionImageUrl());
        preview.setQuestionAudioUrl(question.getQuestionAudioUrl());
        preview.setPoints(question.getPoints());
        preview.setOrderIndex(question.getOrderIndex());
        preview.setTimeLimitSeconds(question.getTimeLimitSeconds());
        preview.setDifficultyLevel(question.getDifficultyLevel());
        preview.setExplanation(question.getExplanation());
        preview.setCorrectAnswer(question.getCorrectAnswer());
        
        // Map options with correct answer information
        List<ExamPreviewResponse.OptionPreview> optionPreviews = question.getOptions().stream()
            .map(this::mapToOptionPreview)
            .collect(Collectors.toList());
        preview.setOptions(optionPreviews);
        
        return preview;
    }
    
    private ExamPreviewResponse.OptionPreview mapToOptionPreview(ExamQuestionOption option) {
        ExamPreviewResponse.OptionPreview preview = new ExamPreviewResponse.OptionPreview();
        preview.setId(option.getId());
        preview.setOptionText(option.getOptionText());
        preview.setOptionImageUrl(option.getOptionImageUrl());
        preview.setOptionAudioUrl(option.getOptionAudioUrl());
        preview.setOrderIndex(option.getOrderIndex());
        preview.setIsCorrect(option.getIsCorrect());
        
        return preview;
    }
}