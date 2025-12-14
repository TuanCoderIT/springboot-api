package com.example.springboot_api.services.user;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.ai.MindmapResponse;
import com.example.springboot_api.mappers.MindmapMapper;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookMindmap;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.shared.MindmapRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các tính năng liên quan đến Mindmap.
 */
@Service
@RequiredArgsConstructor
public class MindmapService {

    private final MindmapRepository mindmapRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookMemberRepository memberRepository;
    private final MindmapMapper mindmapMapper;

    /**
     * Lấy mindmap theo AI Set ID.
     * Kiểm tra user có phải approved member của notebook.
     * 
     * @param userId     User ID
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID
     * @return MindmapResponse
     */
    @Transactional(readOnly = true)
    public MindmapResponse getMindmapByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        // Validate user là approved member
        validateMembership(userId, notebookId);

        // Validate AI Set thuộc về notebook
        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + aiSetId));

        if (!aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc về notebook này.");
        }

        // Lấy mindmap
        List<NotebookMindmap> mindmaps = mindmapRepository.findByAiSetId(aiSetId);
        if (mindmaps.isEmpty()) {
            throw new NotFoundException("Không tìm thấy mindmap cho AI Set này.");
        }

        return mindmapMapper.toMindmapResponse(mindmaps.get(0));
    }

    /**
     * Lấy danh sách mindmaps theo notebook ID.
     * 
     * @param userId     User ID
     * @param notebookId Notebook ID
     * @return List<MindmapResponse>
     */
    @Transactional(readOnly = true)
    public List<MindmapResponse> getMindmapsByNotebookId(UUID userId, UUID notebookId) {
        // Validate user là approved member
        validateMembership(userId, notebookId);

        List<NotebookMindmap> mindmaps = mindmapRepository.findByNotebookId(notebookId);
        return mindmaps.stream()
                .map(mindmapMapper::toMindmapResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate user là approved member của notebook.
     */
    private void validateMembership(UUID userId, UUID notebookId) {
        var member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên của notebook này."));

        if (!"approved".equals(member.getStatus())) {
            throw new BadRequestException("Bạn chưa được duyệt tham gia notebook này.");
        }
    }
}
