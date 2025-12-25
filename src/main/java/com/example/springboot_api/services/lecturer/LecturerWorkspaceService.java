package com.example.springboot_api.services.lecturer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.lecturer.workspace.LecturerWorkspaceFileRequest;
import com.example.springboot_api.dto.lecturer.workspace.LecturerWorkspaceFileResponse;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceAiRequest;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceAiResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;
import com.example.springboot_api.services.shared.ai.generation.AudioOverviewService;
import com.example.springboot_api.services.shared.ai.generation.FlashcardGenerationService;
import com.example.springboot_api.services.shared.ai.generation.QuizGenerationService;
import com.example.springboot_api.services.shared.ai.generation.SummaryGenerationService;
import com.example.springboot_api.services.shared.ai.generation.VideoGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý AI Workspace cho giảng viên.
 * Tái sử dụng toàn bộ logic AI từ notebook user, không duplicate code.
 * 
 * Concept: Mỗi lớp học phần = 1 notebook với type="class"
 * - Giảng viên = owner của notebook
 * - Sinh viên = members của notebook
 * - Tất cả AI features (summary, quiz, flashcard, video) đều dùng chung logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LecturerWorkspaceService {

    // Core repositories
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final NotebookFileRepository fileRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final UserRepository userRepository;
    
    // File handling
    private final FileStorageService fileStorageService;
    private final FileProcessingTaskService fileProcessingTaskService;
    
    // AI Generation Services (reuse existing logic)
    private final SummaryGenerationService summaryGenerationService;
    private final QuizGenerationService quizGenerationService;
    private final FlashcardGenerationService flashcardGenerationService;
    private final VideoGenerationService videoGenerationService;
    private final AudioOverviewService audioOverviewService;

    /**
     * Upload và quản lý tài liệu cho workspace của giảng viên.
     * Tái sử dụng logic upload file từ notebook user.
     */
    @Transactional
    public LecturerWorkspaceFileResponse uploadFile(
            UUID notebookId,
            UUID lecturerId,
            LecturerWorkspaceFileRequest request,
            MultipartFile file) {
        
        log.info("Lecturer {} uploading file to workspace {}", lecturerId, notebookId);
        
        // Validate lecturer permission
        validateLecturerPermission(notebookId, lecturerId);
        
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        
        try {
            // Store file using existing service
            String fileUrl = fileStorageService.storeFile(file);
            
            // Create NotebookFile entity (reuse existing structure)
            NotebookFile notebookFile = new NotebookFile();
            notebookFile.setNotebook(getNotebook(notebookId));
            notebookFile.setUploadedBy(getUser(lecturerId));
            notebookFile.setOriginalFilename(file.getOriginalFilename());
            notebookFile.setStorageUrl(fileUrl);
            notebookFile.setFileSize(file.getSize());
            notebookFile.setMimeType(file.getContentType());
            notebookFile.setStatus("uploaded");
            notebookFile.setCreatedAt(OffsetDateTime.now());
            notebookFile.setUpdatedAt(OffsetDateTime.now());
            
            // Set lecturer-specific metadata
            if (request.getChapter() != null) {
                notebookFile.setExtraMetadata(java.util.Map.of(
                    "chapter", request.getChapter(),
                    "lecturerWorkspace", true,
                    "purpose", request.getPurpose() != null ? request.getPurpose() : "teaching_material"
                ));
            }
            
            NotebookFile saved = fileRepository.save(notebookFile);
            
            // Trigger file processing (reuse existing pipeline)
            fileProcessingTaskService.startAIProcessing(saved);
            
            return mapToFileResponse(saved);
            
        } catch (Exception e) {
            log.error("Error uploading file for lecturer workspace", e);
            throw new BadRequestException("Không thể upload file: " + e.getMessage());
        }
    }

    /**
     * Tạo tóm tắt AI cho workspace của giảng viên.
     * Tái sử dụng hoàn toàn SummaryGenerationService.
     */
    @Transactional
    public WorkspaceAiResponse generateSummary(
            UUID notebookId,
            UUID lecturerId,
            WorkspaceAiRequest request) {
        
        log.info("Lecturer {} generating summary for workspace {}", lecturerId, notebookId);
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        // Create AI Set (reuse existing structure)
        NotebookAiSet aiSet = createAiSet(notebookId, lecturerId, "summary", request);
        
        // Use existing summary generation service with correct signature
        summaryGenerationService.processSummaryGenerationAsync(
            aiSet.getId(),
            notebookId,
            lecturerId,
            request.getFileIds(),
            "vi-VN-Standard-A", // Default voice ID
            "vi", // Default language
            request.getCustomPrompt()
        );
        
        return mapToAiResponse(aiSet, "Đang tạo tóm tắt...");
    }

    /**
     * Tạo quiz AI cho workspace của giảng viên.
     * Tái sử dụng hoàn toàn QuizGenerationService.
     */
    @Transactional
    public WorkspaceAiResponse generateQuiz(
            UUID notebookId,
            UUID lecturerId,
            WorkspaceAiRequest request) {
        
        log.info("Lecturer {} generating quiz for workspace {}", lecturerId, notebookId);
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        // Create AI Set
        NotebookAiSet aiSet = createAiSet(notebookId, lecturerId, "quiz", request);
        
        // Use existing quiz generation service with correct signature
        quizGenerationService.processQuizGenerationAsync(
            aiSet.getId(),
            notebookId,
            lecturerId,
            request.getFileIds(),
            request.getQuizCount() != null ? request.getQuizCount().toString() : "10",
            "medium", // Default difficulty level
            request.getCustomPrompt()
        );
        
        return mapToAiResponse(aiSet, "Đang tạo quiz...");
    }

    /**
     * Tạo flashcard AI cho workspace của giảng viên.
     * Tái sử dụng hoàn toàn FlashcardGenerationService.
     */
    @Transactional
    public WorkspaceAiResponse generateFlashcard(
            UUID notebookId,
            UUID lecturerId,
            WorkspaceAiRequest request) {
        
        log.info("Lecturer {} generating flashcard for workspace {}", lecturerId, notebookId);
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        // Create AI Set
        NotebookAiSet aiSet = createAiSet(notebookId, lecturerId, "flashcard", request);
        
        // Use existing flashcard generation service with correct signature
        flashcardGenerationService.processFlashcardGenerationAsync(
            aiSet.getId(),
            notebookId,
            lecturerId,
            request.getFileIds(),
            request.getFlashcardCount() != null ? request.getFlashcardCount().toString() : "20",
            request.getCustomPrompt()
        );
        
        return mapToAiResponse(aiSet, "Đang tạo flashcard...");
    }

    /**
     * Tạo video learning content cho workspace của giảng viên.
     * Tái sử dụng hoàn toàn VideoGenerationService.
     */
    @Transactional
    public WorkspaceAiResponse generateVideo(
            UUID notebookId,
            UUID lecturerId,
            WorkspaceAiRequest request) {
        
        log.info("Lecturer {} generating video for workspace {}", lecturerId, notebookId);
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        // Create AI Set
        NotebookAiSet aiSet = createAiSet(notebookId, lecturerId, "video", request);
        
        // Use existing video generation service with correct signature
        videoGenerationService.processVideoGenerationAsync(
            aiSet.getId(),
            notebookId,
            lecturerId,
            request.getFileIds(),
            "CORPORATE", // Default template
            request.getCustomPrompt(),
            10, // Default number of slides
            true // Generate images
        );
        
        return mapToAiResponse(aiSet, "Đang tạo video learning content...");
    }

    /**
     * Lấy danh sách files trong workspace của giảng viên.
     */
    public List<LecturerWorkspaceFileResponse> getWorkspaceFiles(
            UUID notebookId,
            UUID lecturerId,
            String chapter) {
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        List<NotebookFile> files;
        if (chapter != null && !chapter.trim().isEmpty()) {
            // Filter by chapter using metadata
            files = fileRepository.findByNotebookIdAndMetadataChapter(notebookId, chapter);
        } else {
            files = fileRepository.findByNotebookIdOrderByCreatedAtDesc(notebookId);
        }
        
        return files.stream()
                .map(this::mapToFileResponse)
                .toList();
    }

    /**
     * Lấy danh sách AI content đã tạo trong workspace.
     */
    public List<WorkspaceAiResponse> getWorkspaceAiContent(
            UUID notebookId,
            UUID lecturerId,
            String contentType) {
        
        // Validate permission
        validateLecturerPermission(notebookId, lecturerId);
        
        List<NotebookAiSet> aiSets;
        if (contentType != null && !contentType.trim().isEmpty()) {
            aiSets = aiSetRepository.findByNotebookIdAndSetTypeOrderByCreatedAtDesc(notebookId, contentType);
        } else {
            aiSets = aiSetRepository.findByNotebookIdOrderByCreatedAtDesc(notebookId);
        }
        
        return aiSets.stream()
                .map(aiSet -> mapToAiResponse(aiSet, getStatusMessage(aiSet)))
                .toList();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate lecturer có quyền truy cập workspace (notebook).
     */
    private void validateLecturerPermission(UUID notebookId, UUID lecturerId) {
        Notebook notebook = getNotebook(notebookId);
        
        // Check if notebook is class type
        if (!"class".equals(notebook.getType())) {
            throw new BadRequestException("Đây không phải là workspace lớp học phần");
        }
        
        // Check lecturer is owner or has lecturer role
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, lecturerId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập workspace này"));
        
        if (!"owner".equals(member.getRole()) && !"lecturer".equals(member.getRole())) {
            throw new ForbiddenException("Chỉ giảng viên mới có quyền sử dụng workspace này");
        }
        
        if (!"approved".equals(member.getStatus())) {
            throw new ForbiddenException("Tài khoản chưa được duyệt cho workspace này");
        }
    }

    /**
     * Tạo AI Set cho workspace (tái sử dụng cấu trúc existing).
     */
    private NotebookAiSet createAiSet(UUID notebookId, UUID lecturerId, String setType, WorkspaceAiRequest request) {
        NotebookAiSet aiSet = new NotebookAiSet();
        aiSet.setNotebook(getNotebook(notebookId));
        aiSet.setCreatedBy(getUser(lecturerId));
        aiSet.setSetType(setType);
        aiSet.setStatus("queued");
        aiSet.setModelCode(request.getModelCode() != null ? request.getModelCode() : "gemini");
        aiSet.setProvider("google");
        aiSet.setTitle(request.getTitle() != null ? request.getTitle() : generateDefaultTitle(setType));
        aiSet.setDescription(request.getDescription());
        aiSet.setCreatedAt(OffsetDateTime.now());
        aiSet.setUpdatedAt(OffsetDateTime.now());
        
        // Set lecturer workspace metadata
        aiSet.setMetadata(java.util.Map.of(
            "lecturerWorkspace", true,
            "chapter", request.getChapter() != null ? request.getChapter() : "",
            "purpose", "teaching_content"
        ));
        
        // Set input config (reuse existing structure)
        java.util.Map<String, Object> inputConfig = new java.util.HashMap<>();
        inputConfig.put("fileIds", request.getFileIds());
        inputConfig.put("customPrompt", request.getCustomPrompt());
        if (request.getQuizCount() != null) {
            inputConfig.put("quizCount", request.getQuizCount());
        }
        if (request.getFlashcardCount() != null) {
            inputConfig.put("flashcardCount", request.getFlashcardCount());
        }
        aiSet.setInputConfig(inputConfig);
        
        return aiSetRepository.save(aiSet);
    }

    private Notebook getNotebook(UUID notebookId) {
        return notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Workspace không tồn tại"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
    }

    private String generateDefaultTitle(String setType) {
        return switch (setType) {
            case "summary" -> "Tóm tắt nội dung";
            case "quiz" -> "Bộ câu hỏi trắc nghiệm";
            case "flashcard" -> "Bộ thẻ ghi nhớ";
            case "video" -> "Video học tập";
            default -> "Nội dung AI";
        };
    }

    private String getStatusMessage(NotebookAiSet aiSet) {
        return switch (aiSet.getStatus()) {
            case "queued" -> "Đang chờ xử lý...";
            case "processing" -> "Đang tạo nội dung...";
            case "completed" -> "Hoàn thành";
            case "failed" -> "Lỗi: " + (aiSet.getErrorMessage() != null ? aiSet.getErrorMessage() : "Không xác định");
            default -> aiSet.getStatus();
        };
    }

    private LecturerWorkspaceFileResponse mapToFileResponse(NotebookFile file) {
        return LecturerWorkspaceFileResponse.builder()
                .id(file.getId())
                .fileName(file.getOriginalFilename())
                .fileUrl(file.getStorageUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .status(file.getStatus())
                .chapter(file.getExtraMetadata() != null ? (String) file.getExtraMetadata().get("chapter") : null)
                .purpose(file.getExtraMetadata() != null ? (String) file.getExtraMetadata().get("purpose") : null)
                .uploadedAt(file.getCreatedAt())
                .build();
    }

    private WorkspaceAiResponse mapToAiResponse(NotebookAiSet aiSet, String statusMessage) {
        return WorkspaceAiResponse.builder()
                .id(aiSet.getId())
                .contentType(aiSet.getSetType())
                .title(aiSet.getTitle())
                .description(aiSet.getDescription())
                .status(aiSet.getStatus())
                .statusMessage(statusMessage)
                .modelCode(aiSet.getModelCode())
                .chapter(aiSet.getMetadata() != null ? (String) aiSet.getMetadata().get("chapter") : null)
                .createdAt(aiSet.getCreatedAt())
                .finishedAt(aiSet.getFinishedAt())
                .build();
    }
}