package com.example.springboot_api.services.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.springboot_api.dto.user.tts.TtsVoiceResponse;
import com.example.springboot_api.models.TtsVoice;
import com.example.springboot_api.repositories.shared.TtsVoiceRepository;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý logic liên quan đến danh sách giọng đọc TTS (TtsVoice).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserTtsVoiceService {

    private final TtsVoiceRepository ttsVoiceRepository;
    private final UrlNormalizer urlNormalizer;

    /**
     * Lấy danh sách tất cả các giọng đọc đang hoạt động (active).
     * Sắp xếp theo thứ tự hiển thị (sort_order).
     *
     * @return List<TtsVoiceResponse> danh sách các voice active với URL đầy đủ
     */
    public List<TtsVoiceResponse> getAllActiveVoices() {
        log.info("Fetching all active TTS voices for user");
        List<TtsVoice> voices = ttsVoiceRepository.findByIsActiveTrueOrderBySortOrderAsc();

        return voices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert TtsVoice entity sang TtsVoiceResponse DTO với URL đầy đủ.
     */
    private TtsVoiceResponse toResponse(TtsVoice voice) {
        return TtsVoiceResponse.builder()
                .id(voice.getId())
                .voiceId(voice.getVoiceId())
                .voiceName(voice.getVoiceName())
                .description(voice.getDescription())
                .provider(voice.getProvider())
                .gender(voice.getGender())
                .language(voice.getLanguage())
                .accent(voice.getAccent())
                .style(voice.getStyle())
                .ageGroup(voice.getAgeGroup())
                .useCase(voice.getUseCase())
                .sampleAudioUrl(urlNormalizer.normalizeToFull(voice.getSampleAudioUrl()))
                .sampleText(voice.getSampleText())
                .sampleDurationMs(voice.getSampleDurationMs())
                .defaultSpeed(voice.getDefaultSpeed())
                .defaultPitch(voice.getDefaultPitch())
                .isActive(voice.getIsActive())
                .isPremium(voice.getIsPremium())
                .sortOrder(voice.getSortOrder())
                .createdAt(voice.getCreatedAt())
                .updatedAt(voice.getUpdatedAt())
                .build();
    }
}
