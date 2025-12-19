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
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMindmap;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.MindmapRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý mindmap generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final MindmapRepository mindmapRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final JsonParsingService jsonParsingService;
    private final AiSetStatusService statusService;

    /**
     * Xử lý mindmap generation ở background.
     */
    @Async
    @Transactional
    public void processMindmapGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String additionalRequirements) {

        log.info("🚀 [MINDMAP] Bắt đầu tạo mindmap - AiSet: {}", aiSetId);

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                log.error("❌ [MINDMAP] Không tìm thấy AiSet: {}", aiSetId);
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
            String summaryText = summarizationService.summarizeDocuments(files, null);
            if (summaryText == null || summaryText.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt tài liệu");
                return;
            }

            // Tạo mindmap qua LLM
            String mindmapPrompt = buildMindmapPrompt(summaryText, additionalRequirements);
            String llmResponse = aiModelService.callGeminiModel(mindmapPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                statusService.markFailed(aiSetId, "LLM trả về response rỗng");
                return;
            }

            // Parse JSON
            Map<String, Object> mindmapData = jsonParsingService.parseMindmapJson(llmResponse);
            if (mindmapData == null) {
                statusService.markFailed(aiSetId, "Không thể parse mindmap JSON");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rootNode = (Map<String, Object>) mindmapData.get("root");
            String title = rootNode != null ? (String) rootNode.getOrDefault("title", "Mindmap") : "Mindmap";

            // Lưu vào database
            NotebookMindmap mindmapEntity = NotebookMindmap.builder()
                    .notebook(notebook)
                    .title(title)
                    .mindmap(mindmapData)
                    .layout(Map.of("direction", "horizontal", "spacing", 100))
                    .sourceAiSet(aiSet)
                    .createdBy(user)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            mindmapRepository.save(mindmapEntity);

            // Update AiSet title
            statusService.updateTitle(aiSetId, title);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("mindmapId", mindmapEntity.getId().toString());
            outputStats.put("title", title);
            statusService.markDone(aiSetId, outputStats);

            log.info("✅ [MINDMAP] Hoàn thành - AiSet: {}", aiSetId);

        } catch (Exception e) {
            statusService.markFailed(aiSetId, "Lỗi: " + e.getMessage());
            log.error("❌ [MINDMAP] {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo prompt cho mindmap generation.
     */
    public String buildMindmapPrompt(String summaryText, String additionalRequirements) {
        String extraNote = (additionalRequirements != null && !additionalRequirements.isBlank())
                ? "\nYêu cầu thêm: " + additionalRequirements
                : "";

        return String.format("""
                Bạn là chuyên gia tạo sơ đồ tư duy (mindmap) cho học tập.

                Dựa trên nội dung sau, tạo một mindmap có cấu trúc phân cấp:

                NỘI DUNG:
                %s
                %s

                YÊU CẦU:
                - Tạo cấu trúc mindmap với node gốc (root) và các nhánh con
                - Mỗi node có: id (UUID), title (tiêu đề ngắn), summary (tóm tắt 1-2 câu), children (mảng node con)
                - Tối đa 3-4 cấp độ sâu
                - Mỗi nhánh chính có 2-5 nhánh con
                - Title: 3-7 từ, summary: 1-2 câu

                TRẢ VỀ JSON (KHÔNG có markdown wrapper):
                {
                  "root": {
                    "id": "uuid-format",
                    "title": "Tiêu đề chính",
                    "summary": "Tóm tắt tổng quan",
                    "children": [
                      {
                        "id": "uuid-format",
                        "title": "Nhánh 1",
                        "summary": "Tóm tắt nhánh 1",
                        "children": []
                      }
                    ]
                  }
                }

                CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC.
                """, summaryText, extraNote);
    }
}
