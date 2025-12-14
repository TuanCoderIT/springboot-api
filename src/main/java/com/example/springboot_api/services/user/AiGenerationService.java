package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.chatbot.AiSetResponse;
import com.example.springboot_api.mappers.AiSetMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.AiAsyncTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các tính năng AI Generation (Quiz, Summary, Flashcards, TTS,
 * Video...).
 * Tách riêng để quản lý nghiệp vụ AI generation độc lập với ChatBot.
 * 
 * Sử dụng NotebookAiSet thay cho AiTask để quản lý các AI generation sets.
 * Mỗi quiz/flashcard/tts/video sẽ có foreign key tới NotebookAiSet.
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
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final AiAsyncTaskService aiAsyncTaskService;
    private final AiSetMapper aiSetMapper;
    private final com.example.springboot_api.utils.UrlNormalizer urlNormalizer;
    private final ObjectMapper objectMapper;

    // ================================
    // QUIZ GENERATION
    // ================================

    /**
     * Tạo quiz từ các notebook files (chạy nền).
     * API trả về aiSetId ngay lập tức, việc tạo quiz xử lý ở background.
     * 
     * @param notebookId             Notebook ID
     * @param userId                 ID của user tạo quiz
     * @param fileIds                Danh sách file IDs
     * @param numberOfQuestions      Số lượng câu hỏi: "few" | "standard" | "many"
     * @param difficultyLevel        Độ khó: "easy" | "medium" | "hard"
     * @param additionalRequirements Yêu cầu bổ sung từ người dùng (optional)
     * @return Map chứa aiSetId để track tiến trình
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

            // Tạo NotebookAiSet với trạng thái queued
            NotebookAiSet savedAiSet = createQuizAiSet(notebook, user, selectedFiles, fileIds, numberOfQuestions,
                    difficultyLevel, additionalRequirements);

            // Trả về aiSetId ngay lập tức
            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Quiz đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            // Log để debug
            System.out.println("📤 [MAIN] Gọi async method - Thread: " + Thread.currentThread().getName());

            // Chạy quiz generation ở background (delegate sang AiAsyncTaskService)
            // QUAN TRỌNG: Chỉ truyền IDs, không truyền managed entities để tránh
            // LazyInitializationException
            aiAsyncTaskService.processQuizGenerationAsync(
                    savedAiSet.getId(),
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

    // ================================
    // AUDIO OVERVIEW (SYNC)
    // ================================

    public Map<String, Object> generateAudioOverview(UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String outputFormat, String notes) {
        Map<String, Object> result = new HashMap<>();
        try {
            Notebook notebook = notebookRepository.findById(notebookId)
                    .orElseThrow(() -> new NotFoundException("Notebook không tồn tại: " + notebookId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

            if (fileIds == null || fileIds.isEmpty()) {
                result.put("error", "Danh sách file IDs không được để trống");
                return result;
            }

            // Lấy files hợp lệ thuộc notebook
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

            // Sinh JSON script overview
            String json = aiAsyncTaskService.generateAudioOverviewJson(selectedFiles, null);
            JsonNode node = objectMapper.readTree(json);
            String script = node.path("voice_script_overview").asText();
            if (script == null || script.isBlank()) {
                result.put("error", "voice_script_overview trống.");
                return result;
            }

            // Gọi ElevenLabs TTS và lưu asset
            var asset = aiAsyncTaskService.generateAudioOverviewAsset(
                    script, voiceId, outputFormat, notebook, user, null);

            result.put("success", true);
            result.put("audioUrl", urlNormalizer.normalizeToFull(asset.getAudioUrl()));
            result.put("voiceName", asset.getVoiceName());
            result.put("setType", "tts");
            return result;
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return result;
        }
    }

    // ================================
    // AUDIO OVERVIEW (ASYNC giống quiz)
    // ================================

    public Map<String, Object> generateAudioOverviewAsync(UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String outputFormat, String notes) {
        Map<String, Object> result = new HashMap<>();
        try {
            Notebook notebook = notebookRepository.findById(notebookId)
                    .orElseThrow(() -> new NotFoundException("Notebook không tồn tại: " + notebookId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

            if (fileIds == null || fileIds.isEmpty()) {
                result.put("error", "Danh sách file IDs không được để trống");
                return result;
            }

            // Lấy files hợp lệ thuộc notebook
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

            OffsetDateTime now = OffsetDateTime.now();
            Map<String, Object> inputConfig = new HashMap<>();
            inputConfig.put("fileIds", fileIds);
            if (voiceId != null && !voiceId.isBlank()) {
                inputConfig.put("voiceId", voiceId);
            }
            if (outputFormat != null && !outputFormat.isBlank()) {
                inputConfig.put("outputFormat", outputFormat);
            }
            if (notes != null && !notes.isBlank()) {
                inputConfig.put("notes", notes);
            }

            NotebookAiSet aiSet = NotebookAiSet.builder()
                    .notebook(notebook)
                    .createdBy(user)
                    .setType("tts")
                    .status("queued")
                    .title("Audio Overview từ " + selectedFiles.size() + " tài liệu")
                    .inputConfig(inputConfig)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

            // Liên kết files
            for (NotebookFile file : selectedFiles) {
                NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                        .aiSet(savedAiSet)
                        .file(file)
                        .createdAt(now)
                        .build();
                aiSetFileRepository.save(aiSetFile);
            }

            // Chạy async
            aiAsyncTaskService.processAudioOverviewAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, voiceId, outputFormat, notes);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("success", true);
            result.put("message", "Audio Overview đang được tạo ở nền. Dùng aiSetId để theo dõi.");
            return result;
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Tạo NotebookAiSet và liên kết files.
     */
    @Transactional
    public NotebookAiSet createQuizAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String numberOfQuestions, String difficultyLevel, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("numberOfQuestions", numberOfQuestions);
        inputConfig.put("difficultyLevel", difficultyLevel);
        inputConfig.put("fileIds", fileIds);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("quiz")
                .status("queued")
                .title("Quiz từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Liên kết tất cả files với AI Set (save qua repository để tránh NPE)
        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }

    // ================================
    // FLASHCARDS GENERATION
    // ================================

    /**
     * Tạo flashcards từ các notebook files (chạy nền).
     * API trả về aiSetId ngay lập tức, việc tạo flashcards xử lý ở background.
     *
     * @param notebookId             Notebook ID
     * @param userId                 ID của user tạo flashcards
     * @param fileIds                Danh sách file IDs
     * @param numberOfCards          Số lượng flashcards: "few" | "standard" |
     *                               "many"
     * @param additionalRequirements Yêu cầu bổ sung từ người dùng (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    public Map<String, Object> generateFlashcards(UUID notebookId, UUID userId, List<UUID> fileIds,
            String numberOfCards, String additionalRequirements) {
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

            // Tạo NotebookAiSet với trạng thái queued
            NotebookAiSet savedAiSet = createFlashcardAiSet(notebook, user, selectedFiles, fileIds, numberOfCards,
                    additionalRequirements);

            // Trả về aiSetId ngay lập tức
            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Flashcards đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            // Chạy flashcard generation ở background
            aiAsyncTaskService.processFlashcardGenerationAsync(
                    savedAiSet.getId(),
                    notebookId,
                    userId,
                    fileIds,
                    numberOfCards,
                    additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo flashcards: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tạo NotebookAiSet cho flashcards và liên kết files.
     */
    @Transactional
    public NotebookAiSet createFlashcardAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String numberOfCards, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("numberOfCards", numberOfCards);
        inputConfig.put("fileIds", fileIds);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("flashcard")
                .status("queued")
                .title("Flashcards từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Liên kết tất cả files với AI Set
        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }

    // ================================
    // AI SET MANAGEMENT
    // ================================

    /**
     * Lấy danh sách AI Sets theo notebook.
     * - Sets của user hiện tại: Hiển thị tất cả status
     * - Sets của người khác: Chỉ hiển thị done
     */
    public List<AiSetResponse> getAiSets(UUID notebookId, UUID userId, String setType) {
        List<AiSetResponse> result = new ArrayList<>();

        // Lấy tất cả AI sets của user hiện tại trong notebook
        List<NotebookAiSet> mySets = aiSetRepository.findByNotebookIdAndUserId(notebookId, userId);

        // Lấy AI sets đã hoàn thành của người khác
        List<NotebookAiSet> otherSets = aiSetRepository.findCompletedByNotebookIdExcludeUser(notebookId, userId);

        // Convert sets của user hiện tại
        for (NotebookAiSet set : mySets) {
            if (setType != null && !setType.isEmpty() && !setType.equals(set.getSetType())) {
                continue;
            }
            result.add(convertToAiSetResponse(set, true));
        }

        // Convert sets đã hoàn thành của người khác
        for (NotebookAiSet set : otherSets) {
            if (setType != null && !setType.isEmpty() && !setType.equals(set.getSetType())) {
                continue;
            }
            result.add(convertToAiSetResponse(set, false));
        }

        // Sort theo createdAt DESC
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return result;
    }

    // ================================
    // MINDMAP GENERATION
    // ================================

    /**
     * Tạo mindmap từ các notebook files (chạy nền).
     * API trả về aiSetId ngay lập tức, việc tạo mindmap xử lý ở background.
     *
     * @param notebookId             Notebook ID
     * @param userId                 ID của user tạo mindmap
     * @param fileIds                Danh sách file IDs
     * @param additionalRequirements Yêu cầu bổ sung từ người dùng (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    public Map<String, Object> generateMindmap(UUID notebookId, UUID userId, List<UUID> fileIds,
            String additionalRequirements) {
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

            // Tạo NotebookAiSet với trạng thái queued
            NotebookAiSet savedAiSet = createMindmapAiSet(notebook, user, selectedFiles, fileIds,
                    additionalRequirements);

            // Trả về aiSetId ngay lập tức
            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Mindmap đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            // Chạy mindmap generation ở background
            aiAsyncTaskService.processMindmapGenerationAsync(
                    savedAiSet.getId(),
                    notebookId,
                    userId,
                    fileIds,
                    additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo mindmap: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tạo NotebookAiSet cho mindmap và liên kết files.
     */
    @Transactional
    public NotebookAiSet createMindmapAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("mindmap")
                .status("queued")
                .title("Mindmap từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Liên kết tất cả files với AI Set
        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }

    // ================================
    // SUGGESTION GENERATION
    // ================================

    /**
     * Tạo câu hỏi gợi mở từ các notebook files (chạy nền).
     * API trả về aiSetId ngay lập tức, việc tạo suggestion xử lý ở background.
     *
     * @param notebookId Notebook ID
     * @param userId     ID của user tạo suggestion
     * @param fileIds    Danh sách file IDs
     * @return Map chứa aiSetId để track tiến trình
     */
    public Map<String, Object> generateSuggestions(UUID notebookId, UUID userId, List<UUID> fileIds,
            String additionalRequirements) {
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

            // Tạo NotebookAiSet với trạng thái queued
            NotebookAiSet savedAiSet = createSuggestionAiSet(notebook, user, selectedFiles, fileIds);

            // Trả về aiSetId ngay lập tức
            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Suggestions đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            // Chạy suggestion generation ở background
            aiAsyncTaskService.processSuggestionGenerationAsync(
                    savedAiSet.getId(),
                    notebookId,
                    userId,
                    fileIds,
                    additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo suggestions: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tạo NotebookAiSet cho suggestion và liên kết files.
     */
    @Transactional
    public NotebookAiSet createSuggestionAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("suggestion")
                .status("queued")
                .title("Câu hỏi gợi mở từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Liên kết tất cả files với AI Set
        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    private AiSetResponse convertToAiSetResponse(NotebookAiSet set, boolean isOwner) {
        return aiSetMapper.toAiSetResponse(set, isOwner);
    }

    // ================================
    // DELETE AI SET
    // ================================

    /**
     * Xóa AI Set và tất cả dữ liệu liên quan.
     * Chỉ cho phép xóa nếu user là người tạo AI Set.
     * 
     * @param userId  ID của user đang request
     * @param aiSetId ID của AI Set cần xóa
     * @throws NotFoundException   nếu không tìm thấy AI Set
     * @throws BadRequestException nếu user không phải người tạo
     */
    @Transactional
    public void deleteAiSet(UUID userId, UUID aiSetId) {
        // Tìm AI Set
        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + aiSetId));

        // Kiểm tra quyền: chỉ người tạo mới được xóa
        if (aiSet.getCreatedBy() == null || !aiSet.getCreatedBy().getId().equals(userId)) {
            throw new BadRequestException("Bạn chỉ có thể xóa AI Set do chính mình tạo");
        }

        // Xóa các file liên kết (NotebookAiSetFile)
        aiSetFileRepository.deleteByAiSetId(aiSetId);

        // Xóa AI Set (cascade sẽ xóa quizzes, options, flashcards, etc.)
        aiSetRepository.delete(aiSet);
    }

    // ================================
    // VIDEO GENERATION
    // ================================

    /**
     * Tạo video từ các notebook files (chạy nền).
     * API trả về aiSetId ngay lập tức, việc tạo video xử lý ở background.
     *
     * @param notebookId             Notebook ID
     * @param userId                 ID của user tạo video
     * @param fileIds                Danh sách file IDs
     * @param numberOfSlides         Số slides (mặc định 5)
     * @param generateImages         Có sinh ảnh AI hay không
     * @param additionalRequirements Yêu cầu bổ sung từ người dùng (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    public Map<String, Object> generateVideo(UUID notebookId, UUID userId, List<UUID> fileIds,
            int numberOfSlides, boolean generateImages, String additionalRequirements) {
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

            // Tạo NotebookAiSet với trạng thái queued
            NotebookAiSet savedAiSet = createVideoAiSet(notebook, user, selectedFiles, fileIds,
                    numberOfSlides, generateImages, additionalRequirements);

            // Trả về aiSetId ngay lập tức
            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Video đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            // Chạy video generation ở background
            aiAsyncTaskService.processVideoGenerationAsync(
                    savedAiSet.getId(),
                    notebookId,
                    userId,
                    fileIds,
                    "CORPORATE",
                    additionalRequirements,
                    numberOfSlides,
                    generateImages);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo video: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tạo NotebookAiSet cho Video generation.
     */
    @Transactional
    public NotebookAiSet createVideoAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, int numberOfSlides, boolean generateImages, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("numberOfSlides", numberOfSlides);
        inputConfig.put("generateImages", generateImages);
        inputConfig.put("additionalRequirements", additionalRequirements);
        inputConfig.put("fileIds", fileIds);

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("video")
                .status("queued")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Liên kết tất cả files với AI Set
        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }
}
