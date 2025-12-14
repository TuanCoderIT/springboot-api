package com.example.springboot_api.services.user;

import java.util.List;
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
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các thao tác liên quan đến Flashcards.
 */
@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final FlashcardMapper flashcardMapper;

    /**
     * Lấy danh sách flashcards theo NotebookAiSet ID.
     * Kiểm tra quyền truy cập: user phải là thành viên đã được duyệt của notebook.
     *
     * @param userId          ID của user đang request
     * @param notebookId      ID của notebook
     * @param notebookAiSetId ID của NotebookAiSet chứa flashcards
     * @return FlashcardListResponse chứa đầy đủ thông tin flashcards
     */
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

        // Lấy danh sách flashcards và convert sang DTO
        List<Flashcard> cards = flashcardRepository.findByAiSetId(notebookAiSetId);
        List<FlashcardResponse> cardResponses = flashcardMapper.toFlashcardResponseList(cards);

        return flashcardMapper.toFlashcardListResponse(aiSet, cardResponses);
    }
}
