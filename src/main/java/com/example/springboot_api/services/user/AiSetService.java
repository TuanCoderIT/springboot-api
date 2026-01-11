package com.example.springboot_api.services.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.chatbot.AiSetResponse;
import com.example.springboot_api.mappers.AiSetMapper;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.services.shared.ai.AiTaskProgressService;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý các NotebookAiSet.
 * Cung cấp API lấy danh sách, xóa AI sets.
 */
@Service
@RequiredArgsConstructor
public class AiSetService {

    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final UserRepository userRepository;
    private final AiSetMapper aiSetMapper;
    private final AiTaskProgressService progressService;

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
            result.add(aiSetMapper.toAiSetResponse(set, true));
        }

        // Convert sets đã hoàn thành của người khác
        for (NotebookAiSet set : otherSets) {
            if (setType != null && !setType.isEmpty() && !setType.equals(set.getSetType())) {
                continue;
            }
            result.add(aiSetMapper.toAiSetResponse(set, false));
        }

        // Sort theo createdAt DESC
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return result;
    }

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

        // Lấy user để notify
        User deleter = userRepository.findById(userId).orElse(null);

        // Xóa các file liên kết (NotebookAiSetFile)
        aiSetFileRepository.deleteByAiSetId(aiSetId);

        // Notify notebook members trước khi xóa
        progressService.notifyDeleted(aiSet, deleter);

        // Xóa AI Set (cascade sẽ xóa quizzes, options, flashcards, etc.)
        aiSetRepository.delete(aiSet);
    }
}
