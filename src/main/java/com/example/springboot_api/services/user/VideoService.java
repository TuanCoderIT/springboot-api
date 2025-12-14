package com.example.springboot_api.services.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.video.VideoResponse;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.VideoAsset;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.VideoAssetRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các tính năng liên quan đến Video.
 */
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoAssetRepository videoAssetRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookMemberRepository memberRepository;
    private final NotebookRepository notebookRepository;
    private final com.example.springboot_api.utils.UrlNormalizer urlNormalizer;

    /**
     * Lấy video theo AI Set ID.
     * Kiểm tra user có phải approved member của notebook.
     *
     * @param userId     User ID
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID
     * @return VideoResponse
     */
    @Transactional(readOnly = true)
    public VideoResponse getVideoByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        // Validate notebook tồn tại
        if (!notebookRepository.existsById(notebookId)) {
            throw new NotFoundException("Notebook không tồn tại");
        }

        // Validate user là approved member
        validateMembership(userId, notebookId);

        // Kiểm tra aiSet tồn tại và thuộc notebook
        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set: " + aiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        if (!"video".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải loại video");
        }

        // Lấy video asset
        VideoAsset video = videoAssetRepository.findByAiSetId(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy video cho AI Set: " + aiSetId));

        return VideoResponse.builder()
                .id(video.getId())
                .aiSetId(aiSetId)
                .videoUrl(urlNormalizer.normalizeToFull(video.getVideoUrl()))
                .title(video.getTextSource())
                .style(video.getStyle())
                .durationSeconds(video.getDurationSeconds())
                .createdAt(video.getCreatedAt())
                .build();
    }

    /**
     * Validate user là approved member của notebook.
     */
    private void validateMembership(UUID userId, UUID notebookId) {
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên của notebook này."));

        if (!"approved".equals(member.getStatus())) {
            throw new BadRequestException("Bạn chưa được duyệt tham gia notebook này.");
        }
    }
}
