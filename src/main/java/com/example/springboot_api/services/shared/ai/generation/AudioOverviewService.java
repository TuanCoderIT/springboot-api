package com.example.springboot_api.services.shared.ai.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.GeminiTtsService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý audio overview (podcast) generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioOverviewService {

    private final NotebookAiSetRepository aiSetRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final GeminiTtsService ttsService;
    private final AiSetStatusService statusService;
    private final JsonParsingService jsonParsingService;
    private final ObjectMapper objectMapper;

    /**
     * Xử lý audio overview generation ở background.
     */
    @Async
    @Transactional
    public void processAudioOverviewAsync(UUID aiSetId, UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String outputFormat, String notes) {

        log.info("🚀 [AUDIO] Bắt đầu tạo Audio Overview - AiSet: {}", aiSetId);

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                log.error("❌ [AUDIO] Không tìm thấy AiSet: {}", aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();
            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Không tìm thấy notebook hoặc user từ AiSet");
                return;
            }

            List<NotebookFile> selectedFiles = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    selectedFiles.add(asf.getFile());
                }
            });
            if (selectedFiles.isEmpty()) {
                statusService.markFailed(aiSetId, "Không tìm thấy file nào từ AiSet");
                return;
            }

            // Sinh JSON script overview
            String json = generateAudioOverviewJson(selectedFiles);
            ObjectNode node = objectMapper.readValue(json, ObjectNode.class);
            String script = node.path("voice_script_overview").asText();
            if (script == null || script.isBlank()) {
                statusService.markFailed(aiSetId, "voice_script_overview trống.");
                return;
            }

            // Gọi TTS multi-speaker
            TtsAsset asset = ttsService.generateMultiSpeakerTts(script, voiceId, notebook, user, aiSet);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("audioUrl", asset.getAudioUrl());
            outputStats.put("voiceName", asset.getVoiceName());

            statusService.markDone(aiSetId, outputStats);
            log.info("✅ [AUDIO] Hoàn thành Audio Overview - AiSet: {}", aiSetId);

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo Audio Overview: " + e.getMessage();
            statusService.markFailed(aiSetId, errorMsg);
            log.error("❌ [AUDIO] {}", errorMsg, e);
        }
    }

    /**
     * Tạo prompt và gọi LLM để sinh JSON voice_script_overview.
     */
    public String generateAudioOverviewJson(List<NotebookFile> files) {
        String summarized = summarizationService.summarizeDocuments(files, null);
        if (summarized == null || summarized.isEmpty()) {
            throw new IllegalArgumentException("Không có nội dung để tạo audio overview.");
        }

        String prompt = """
                Bạn là biên tập viên nội dung Podcast giáo dục chuyên nghiệp.

                NHIỆM VỤ:
                Dựa trên nội dung tài liệu, hãy viết một kịch bản hội thoại ngắn (khoảng 150-200 từ) giữa hai nhân vật:
                1. **Host**: Người dẫn chương trình, đóng vai trò tò mò, đặt câu hỏi dẫn dắt hoặc tóm tắt ý.
                2. **Expert**: Chuyên gia, trả lời sâu sắc, giải thích nội dung từ tài liệu.

                DỮ LIỆU ĐẦU VÀO (tóm tắt):
                [SLIDE_JSON]

                YÊU CẦU ĐẦU RA:
                Trả về DUY NHẤT một JSON dạng:
                {
                  "voice_script_overview": "Host: Chào các bạn...\\nExpert: Xin chào..."
                }

                QUY TẮC QUAN TRỌNG:
                - Kịch bản PHẢI theo format chính xác:
                  Host: [Lời thoại]
                  Expert: [Lời thoại]
                - Không dùng markdown, không thêm text ngoài JSON.
                - Giọng văn tự nhiên, như văn nói, có cảm xúc.
                - Host nên hỏi những câu "Tại sao?", "Cụ thể là gì?" để Expert trả lời.
                """
                .replace("[SLIDE_JSON]", summarized);

        String response = aiModelService.callGeminiModel(prompt);
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("LLM không trả về nội dung audio overview.");
        }

        // Strip markdown và validate
        String cleanedResponse = jsonParsingService.extractJsonObjectFromResponse(response);

        try {
            ObjectNode node = objectMapper.readValue(cleanedResponse, ObjectNode.class);
            if (!node.hasNonNull("voice_script_overview")) {
                throw new RuntimeException("JSON không có trường voice_script_overview.");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("LLM trả về JSON không hợp lệ: " + e.getMessage(), e);
        }

        return cleanedResponse.trim();
    }
}
