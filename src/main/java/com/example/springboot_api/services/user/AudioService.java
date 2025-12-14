package com.example.springboot_api.services.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.audio.AudioListResponse;
import com.example.springboot_api.dto.user.audio.AudioResponse;
import com.example.springboot_api.mappers.AudioMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các thao tác liên quan đến Audio (TTS).
 */
@Service
@RequiredArgsConstructor
public class AudioService {

    private final TtsAssetRepository ttsAssetRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final AudioMapper audioMapper;

    /**
     * Lấy danh sách audio theo NotebookAiSet ID.
     * Kiểm tra quyền truy cập: user phải là thành viên đã được duyệt của notebook.
     *
     * @param userId          ID của user đang request
     * @param notebookId      ID của notebook
     * @param notebookAiSetId ID của NotebookAiSet chứa audio
     * @return AudioListResponse chứa đầy đủ thông tin audio
     */
    @Transactional(readOnly = true)
    public AudioListResponse getAudioByAiSetId(UUID userId, UUID notebookId, UUID notebookAiSetId) {
        // Validate notebook
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        // Kiểm tra quyền truy cập
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

        // Validate AiSet
        NotebookAiSet aiSet = aiSetRepository.findById(notebookAiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + notebookAiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        // Kiểm tra setType
        if (!"tts".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải là audio/podcast");
        }

        // Lấy danh sách audio và convert sang DTO
        List<TtsAsset> audios = ttsAssetRepository.findByAiSetId(notebookAiSetId);
        List<AudioResponse> audioResponses = audioMapper.toAudioResponseList(audios);

        return audioMapper.toAudioListResponse(aiSet, audioResponses);
    }
}
