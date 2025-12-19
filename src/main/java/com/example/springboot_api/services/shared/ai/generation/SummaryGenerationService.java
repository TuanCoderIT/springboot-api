package com.example.springboot_api.services.shared.ai.generation;

import java.time.OffsetDateTime;
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
import com.example.springboot_api.models.NotebookAiSummary;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSummaryRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.GeminiTtsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý summary generation với TTS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSummaryRepository summaryRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final GeminiTtsService ttsService;
    private final AiSetStatusService statusService;
    private final ObjectMapper objectMapper;

    /**
     * Xử lý summary generation ở background.
     */
    @Async
    @Transactional
    public void processSummaryGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String voiceId, String language, String additionalRequirements) {

        log.info("🚀 [SUMMARY] Bắt đầu tạo summary - AiSet: {}", aiSetId);

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Notebook/User không tồn tại");
                return;
            }

            List<NotebookFile> files = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    files.add(asf.getFile());
                }
            });

            if (files.isEmpty()) {
                statusService.markFailed(aiSetId, "Không có file");
                return;
            }

            // Tóm tắt documents
            String rawSummary = summarizationService.summarizeDocuments(files, null);
            if (rawSummary == null || rawSummary.isBlank()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt tài liệu");
                return;
            }

            // Tạo summary content (markdown + TTS script)
            boolean needTts = voiceId != null && !voiceId.isBlank();
            SummaryContent content = generateSummaryContent(rawSummary, language, additionalRequirements, needTts);

            // Tạo audio nếu cần
            String audioUrl = null;
            Integer durationMs = null;

            if (needTts && content.scriptTts != null) {
                try {
                    GeminiTtsService.AudioResult audio = ttsService.callGeminiTts(content.scriptTts, voiceId);
                    audioUrl = audio.url();
                    durationMs = audio.durationMs();
                } catch (Exception e) {
                    log.warn("TTS failed, continue without audio: {}", e.getMessage());
                }
            }

            // Lưu summary
            String title = extractTitle(content.contentMd);

            NotebookAiSummary summary = NotebookAiSummary.builder()
                    .notebookAiSets(aiSet)
                    .contentMd(content.contentMd)
                    .scriptTts(content.scriptTts)
                    .language(language != null ? language : "vi")
                    .audioUrl(audioUrl)
                    .audioFormat(audioUrl != null ? "wav" : null)
                    .audioDurationMs(durationMs)
                    .ttsProvider("gemini")
                    .ttsModel("gemini-2.5-flash-preview-tts")
                    .voiceId(voiceId)
                    .voiceLabel(voiceId)
                    .createdAt(OffsetDateTime.now())
                    .createBy(user)
                    .build();
            summaryRepository.save(summary);

            // Update AiSet title
            statusService.updateTitle(aiSetId, title);

            Map<String, Object> stats = new HashMap<>();
            stats.put("summaryId", aiSetId.toString());
            stats.put("title", title);
            stats.put("hasAudio", audioUrl != null);
            if (audioUrl != null) {
                stats.put("audioUrl", audioUrl);
                stats.put("audioDurationMs", durationMs);
            }
            statusService.markDone(aiSetId, stats);

            log.info("✅ [SUMMARY] Hoàn thành - AiSet: {}", aiSetId);

        } catch (Exception e) {
            statusService.markFailed(aiSetId, e.getMessage());
            log.error("❌ [SUMMARY] {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo summary content: Markdown + TTS Script trong 1 lần call LLM.
     */
    private SummaryContent generateSummaryContent(String raw, String lang, String extra, boolean needTts) {
        String langNote = "vi".equals(lang) ? "tiếng Việt" : "English";
        String extraNote = (extra != null && !extra.isBlank()) ? "\nYêu cầu thêm: " + extra : "";

        String ttsSection = needTts ? """

                2. "scriptTts": Bản tóm tắt văn xuôi để đọc TTS
                   - KHÔNG có markdown, bullets, ký hiệu đặc biệt
                   - Văn xuôi tự nhiên như đang nói chuyện
                   - Xưng "mình" với "các bạn" (nếu tiếng Việt)
                   - Khoảng 300-400 từ
                """ : "";

        String prompt = String.format("""
                Tóm tắt nội dung sau thành JSON với format:
                {
                  "contentMd": "bản markdown có cấu trúc",
                  "scriptTts": "bản văn xuôi cho TTS" %s
                }

                NỘI DUNG:
                %s
                %s

                YÊU CẦU:
                1. "contentMd": Markdown có cấu trúc
                   - Viết bằng %s
                   - Dùng ## heading, ### subheading
                   - Dùng bullet points cho ý chính
                   - Bold từ khóa quan trọng
                %s

                CHỈ TRẢ VỀ JSON, KHÔNG CÓ MARKDOWN WRAPPER.
                """,
                needTts ? "" : "(bỏ qua nếu không cần TTS)",
                raw, extraNote, langNote, ttsSection);

        try {
            String resp = aiModelService.callGeminiModel(prompt);
            if (resp != null && !resp.isBlank()) {
                resp = stripCodeBlock(resp.trim());
                JsonNode json = objectMapper.readTree(resp);
                String contentMd = json.has("contentMd") ? json.get("contentMd").asText() : raw;
                String scriptTts = needTts && json.has("scriptTts") ? json.get("scriptTts").asText() : null;
                if (scriptTts != null) {
                    scriptTts = ttsService.prepareTtsText(scriptTts);
                }
                return new SummaryContent(contentMd, scriptTts);
            }
        } catch (Exception e) {
            log.warn("Failed to generate summary content: {}", e.getMessage());
        }
        return new SummaryContent(raw, null);
    }

    private record SummaryContent(String contentMd, String scriptTts) {
    }

    private String extractTitle(String md) {
        if (md == null || md.isBlank())
            return "Tóm tắt tài liệu";

        for (String line : md.split("\n")) {
            line = line.trim();
            if (line.startsWith("## "))
                return line.substring(3).trim();
            if (line.startsWith("# "))
                return line.substring(2).trim();
        }

        String clean = md.replaceAll("[#*\\-•]", "").trim();
        return clean.length() > 50 ? clean.substring(0, 47) + "..." : (clean.isEmpty() ? "Tóm tắt tài liệu" : clean);
    }

    private String stripCodeBlock(String text) {
        if (text == null)
            return "";

        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text.trim();
    }
}
