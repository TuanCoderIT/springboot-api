package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.chatbot.AiTaskResponse;
import com.example.springboot_api.models.AiTask;
import com.example.springboot_api.models.AiTaskFile;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.AiTaskFileRepository;
import com.example.springboot_api.repositories.shared.AiTaskRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.AiAsyncTaskService;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các tính năng AI Generation (Quiz, Summary, Flashcards, TTS,
 * Video...).
 * Tách riêng để quản lý nghiệp vụ AI generation độc lập với ChatBot.
 * 
 * Lưu ý: Các methods async được delegate sang AiAsyncTaskService để đảm bảo
 * 
 * @Async hoạt động (tránh self-invocation problem).
 */
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final AiTaskRepository aiTaskRepository;
    private final AiTaskFileRepository aiTaskFileRepository;
    private final AiAsyncTaskService aiAsyncTaskService;

    // ================================
    // QUIZ GENERATION
    // ================================

    /**
     * Tạo quiz từ các notebook files (chạy nền).
     * API trả về taskId ngay lập tức, việc tạo quiz xử lý ở background.
     * 
     * @param notebookId             Notebook ID
     * @param userId                 ID của user tạo quiz
     * @param fileIds                Danh sách file IDs
     * @param numberOfQuestions      Số lượng câu hỏi: "few" | "standard" | "many"
     * @param difficultyLevel        Độ khó: "easy" | "medium" | "hard"
     * @param additionalRequirements Yêu cầu bổ sung từ người dùng (optional)
     * @return Map chứa taskId để track tiến trình
     */
    public Map<String, Object> generateQuiz(UUID notebookId, UUID userId, List<UUID> fileIds,
            String numberOfQuestions, String difficultyLevel, String additionalRequirements) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate notebook và user
            Notebook notebook = notebookRepository.findById(notebookId)
                    .orElseThrow(() -> new NotFoundException("Notebook không tồn tại: " + notebookId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

            if (fileIds == null || fileIds.isEmpty()) {
                result.put("error", "Danh sách file IDs không được để trống");
                return result;
            }

            // Lấy files từ fileIds
            List<NotebookFile> selectedFiles = new ArrayList<>();
            for (UUID fileId : fileIds) {
                NotebookFile file = notebookFileRepository.findById(fileId).orElse(null);
                if (file != null && file.getNotebook() != null && file.getNotebook().getId().equals(notebookId)) {
                    selectedFiles.add(file);
                }
            }

            if (selectedFiles.isEmpty()) {
                result.put("error", "Không tìm thấy file hợp lệ nào");
                return result;
            }

            // Tạo AI Task với trạng thái queued
            AiTask savedTask = createQuizAiTask(notebook, user, selectedFiles, fileIds, numberOfQuestions,
                    difficultyLevel, additionalRequirements);

            // Trả về taskId ngay lập tức
            result.put("taskId", savedTask.getId());
            result.put("status", "queued");
            result.put("message", "Quiz đang được tạo ở nền. Sử dụng taskId để theo dõi tiến trình.");
            result.put("success", true);

            // Log để debug
            System.out.println("📤 [MAIN] Gọi async method - Thread: " + Thread.currentThread().getName());

            // Chạy quiz generation ở background (delegate sang AiAsyncTaskService)
            // QUAN TRỌNG: Chỉ truyền IDs, không truyền managed entities để tránh
            // LazyInitializationException
            aiAsyncTaskService.processQuizGenerationAsync(
                    savedTask.getId(),
                    notebookId,
                    userId,
                    fileIds,
                    numberOfQuestions,
                    difficultyLevel,
                    additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo quiz: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tạo AiTask và liên kết files.
     */
    @Transactional
    public AiTask createQuizAiTask(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String numberOfQuestions, String difficultyLevel, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("numberOfQuestions", numberOfQuestions);
        inputConfig.put("difficultyLevel", difficultyLevel);
        inputConfig.put("fileIds", fileIds);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        AiTask aiTask = AiTask.builder()
                .notebook(notebook)
                .user(user)
                .taskType("quiz")
                .status("queued")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        AiTask savedTask = aiTaskRepository.save(aiTask);

        // Liên kết tất cả files với AI Task
        for (NotebookFile file : selectedFiles) {
            AiTaskFile aiTaskFile = AiTaskFile.builder()
                    .task(savedTask)
                    .file(file)
                    .role("source")
                    .createdAt(now)
                    .build();
            aiTaskFileRepository.save(aiTaskFile);
        }

        return savedTask;
    }

    // ================================
    // AI TASK MANAGEMENT
    // ================================

    /**
     * Lấy danh sách AI Tasks theo notebook.
     * - Tasks của user hiện tại: Hiển thị tất cả status
     * - Tasks của người khác: Chỉ hiển thị done
     */
    public List<AiTaskResponse> getAiTasks(UUID notebookId, UUID userId, String taskType) {
        List<AiTaskResponse> result = new ArrayList<>();

        // Lấy tất cả tasks của user hiện tại trong notebook
        List<AiTask> myTasks = aiTaskRepository.findByNotebookIdAndUserId(notebookId, userId);

        // Lấy tasks đã hoàn thành của người khác
        List<AiTask> otherTasks = aiTaskRepository.findCompletedByNotebookIdExcludeUser(notebookId, userId);

        // Convert tasks của user hiện tại
        for (AiTask task : myTasks) {
            if (taskType != null && !taskType.isEmpty() && !taskType.equals(task.getTaskType())) {
                continue;
            }
            result.add(convertToAiTaskResponse(task, true));
        }

        // Convert tasks đã hoàn thành của người khác
        for (AiTask task : otherTasks) {
            if (taskType != null && !taskType.isEmpty() && !taskType.equals(task.getTaskType())) {
                continue;
            }
            result.add(convertToAiTaskResponse(task, false));
        }

        // Sort theo createdAt DESC
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return result;
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Convert AiTask entity sang AiTaskResponse DTO.
     */
    private AiTaskResponse convertToAiTaskResponse(AiTask task, boolean isOwner) {
        String userFullName = null;
        String userAvatar = null;
        UUID taskUserId = null;

        if (task.getUser() != null) {
            taskUserId = task.getUser().getId();
            userFullName = task.getUser().getFullName();
            userAvatar = task.getUser().getAvatarUrl();
        }

        int fileCount = aiTaskFileRepository.findByTaskId(task.getId()).size();

        return AiTaskResponse.builder()
                .id(task.getId())
                .notebookId(task.getNotebook() != null ? task.getNotebook().getId() : null)
                .userId(taskUserId)
                .userFullName(userFullName)
                .userAvatar(userAvatar)
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .fileCount(fileCount)
                .isOwner(isOwner)
                .build();
    }
}
