package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.flashcard.FlashcardListResponse;
import com.example.springboot_api.dto.user.flashcard.FlashcardResponse;
import com.example.springboot_api.mappers.FlashcardMapper;
import com.example.springboot_api.models.Flashcard;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.generation.FlashcardGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý các thao tác liên quan đến Flashcards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final FlashcardMapper flashcardMapper;
    private final FlashcardGenerationService flashcardGenerationService;

    // ================================
    // GENERATE FLASHCARDS (ASYNC)
    // ================================

    /**
     * Tạo flashcards từ các notebook files (chạy nền).
     */
    public Map<String, Object> generateFlashcards(UUID notebookId, UUID userId, List<UUID> fileIds,
            String numberOfCards, String additionalRequirements) {
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

            NotebookAiSet savedAiSet = createFlashcardAiSet(notebook, user, selectedFiles, fileIds,
                    numberOfCards, additionalRequirements);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Flashcards đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            log.info("📤 [FLASHCARD] Gọi async method - Thread: {}", Thread.currentThread().getName());

            flashcardGenerationService.processFlashcardGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, numberOfCards, additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo flashcards: " + e.getMessage());
            log.error("❌ [FLASHCARD] Error: {}", e.getMessage(), e);
        }

        return result;
    }

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
    // GET FLASHCARDS BY AI SET ID
    // ================================

    @Transactional(readOnly = true)
    public FlashcardListResponse getFlashcardsByAiSetId(UUID userId, UUID notebookId, UUID notebookAiSetId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);
        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        NotebookAiSet aiSet = aiSetRepository.findById(notebookAiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + notebookAiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        List<Flashcard> cards = flashcardRepository.findByAiSetId(notebookAiSetId);
        List<FlashcardResponse> cardResponses = flashcardMapper.toFlashcardResponseList(cards);

        return flashcardMapper.toFlashcardListResponse(aiSet, cardResponses);
    }
}
