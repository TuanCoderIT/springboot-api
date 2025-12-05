package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.bot.ChatHistoryResponse;
import com.example.springboot_api.dto.user.bot.RagQueryResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.RagQuery;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.RagQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BotChatService {

    private final RagQueryRepository ragQueryRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(
            UUID userId,
            UUID notebookId,
            String cursorNext,
            int limit) {

        // Kiểm tra user có quyền truy cập notebook không
        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

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

        List<RagQuery> queries;
        UUID cursorId = null;
        OffsetDateTime cursorCreatedAt = null;

        // Parse cursor nếu có
        if (cursorNext != null && !cursorNext.isEmpty()) {
            try {
                cursorId = UUID.fromString(cursorNext);
                // Lấy createdAt của cursor record
                Optional<RagQuery> cursorQuery = ragQueryRepository.findById(cursorId);
                if (cursorQuery.isPresent()) {
                    cursorCreatedAt = cursorQuery.get().getCreatedAt();
                } else {
                    throw new BadRequestException("Cursor không hợp lệ");
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Cursor không hợp lệ");
            }
        }

        Pageable pageable = PageRequest.of(0, limit + 1); // Lấy thêm 1 để check hasMore

        if (cursorId != null && cursorCreatedAt != null) {
            // Lấy các message cũ hơn cursor
            queries = ragQueryRepository.findChatHistoryByCursor(
                    notebookId, userId, cursorId, cursorCreatedAt, pageable);
        } else {
            // Lần đầu, lấy các message mới nhất
            queries = ragQueryRepository.findChatHistory(notebookId, userId, pageable);
        }

        // Kiểm tra có thêm message không
        boolean hasMore = queries.size() > limit;
        if (hasMore) {
            queries = queries.subList(0, limit);
        }

        // Map sang DTO
        List<RagQueryResponse> messages = queries.stream()
                .map(this::toResponse)
                .toList();

        // Tìm cursorNext (ID của message cũ nhất trong response)
        String nextCursor = null;
        if (!messages.isEmpty()) {
            RagQuery oldestMessage = queries.get(queries.size() - 1);
            nextCursor = oldestMessage.getId().toString();
        }

        return new ChatHistoryResponse(messages, nextCursor, hasMore);
    }

    private RagQueryResponse toResponse(RagQuery query) {
        return new RagQueryResponse(
                query.getId(),
                query.getQuestion(),
                query.getAnswer(),
                query.getSourceChunks(), // JSONB chứa: file_id, file_name, file_type, chunk_index, metadata, score, bounding_box, ocr_text
                query.getLatencyMs(),
                query.getCreatedAt());
    }
}

