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
import com.example.springboot_api.models.TimelineEvent;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.TimelineEventRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý Timeline generation.
 * Chuyển nội dung tài liệu thành dòng chảy sự kiện.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final AiSetStatusService statusService;
    private final JsonParsingService jsonParsingService;

    /**
     * Xử lý timeline generation ở background (async).
     */
    @Async
    @Transactional
    public void processTimelineGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String mode, int maxEvents, String additionalRequirements) {

        log.info("🚀 [TIMELINE] Bắt đầu tạo Timeline - AiSet: {}", aiSetId);

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                log.error("❌ [TIMELINE] Không tìm thấy AiSet: {}", aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();
            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Không tìm thấy notebook hoặc user từ AiSet");
                return;
            }

            // Lấy files từ AiSet
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

            // Summarize documents
            log.info("📝 [TIMELINE] Step 1: Tóm tắt {} files...", selectedFiles.size());
            String summary = summarizationService.summarizeDocuments(selectedFiles, null);
            if (summary == null || summary.isBlank()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt nội dung");
                return;
            }

            // Gọi LLM để sinh timeline
            log.info("🤖 [TIMELINE] Step 2: Gọi LLM sinh timeline (mode={}, max={})...", mode, maxEvents);
            String prompt = buildTimelinePrompt(summary, mode, maxEvents, additionalRequirements);
            String llmResponse = aiModelService.callGeminiModel(prompt);

            // Parse JSON response
            log.info("📊 [TIMELINE] Step 3: Parse JSON response...");
            Map<String, Object> timelineData = jsonParsingService.parseJsonObject(llmResponse);

            if (timelineData == null || !timelineData.containsKey("events")) {
                statusService.markFailed(aiSetId, "LLM không trả về timeline hợp lệ");
                return;
            }

            // Cập nhật title từ LLM
            String timelineTitle = (String) timelineData.getOrDefault("title", "Timeline");
            statusService.updateTitle(aiSetId, timelineTitle);

            // Lưu events vào database
            log.info("💾 [TIMELINE] Step 4: Lưu events vào database...");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventsData = (List<Map<String, Object>>) timelineData.get("events");

            List<UUID> savedEventIds = saveEventsToDatabase(notebook, user, aiSet, eventsData);

            // Mark done
            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("totalEvents", savedEventIds.size());
            outputStats.put("mode", mode);
            statusService.markDone(aiSetId, outputStats);

            log.info("✅ [TIMELINE] Hoàn thành - AiSet: {} | Events: {}", aiSetId, savedEventIds.size());

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo Timeline: " + e.getMessage();
            statusService.markFailed(aiSetId, errorMsg);
            log.error("❌ [TIMELINE] {}", errorMsg, e);
        }
    }

    /**
     * Build prompt cho LLM để sinh timeline.
     */
    private String buildTimelinePrompt(String content, String mode, int maxEvents, String extra) {
        String modeDesc = "logic".equals(mode)
                ? "theo tiến trình logic/học tập"
                : "ưu tiên mốc thời gian rõ ràng";

        String additional = (extra != null && !extra.isBlank())
                ? "\nYêu cầu bổ sung: " + extra
                : "";

        return String.format(
                """
                        Bạn là AI trích xuất timeline học tập. Trả về JSON hợp lệ, không markdown, không giải thích.

                        Bạn sẽ nhận nội dung tài liệu học. Hãy tạo timeline %s.

                        Yêu cầu:
                        - Output đúng JSON theo schema bên dưới.
                        - Không tự bịa sự kiện. Nếu không có mốc thời gian rõ, đặt date="unknown" và datePrecision="unknown".
                        - Sắp xếp theo thứ tự hợp lý. Nếu có date thì ưu tiên sort theo date, còn lại dùng order.
                        - Mỗi description <= 180 ký tự, viết rõ, dễ hiểu.
                        - importance ∈ {minor, normal, major, critical}
                        - icon là optional, chỉ dùng một trong: {history, network, protocol, release, concept, law, event, warning, milestone, process}
                        %s

                        Schema:
                        {
                          "title": "Tiêu đề timeline",
                          "events": [
                            {
                              "order": 1,
                              "date": "1945",
                              "datePrecision": "year",
                              "title": "Tên sự kiện",
                              "description": "Mô tả ngắn",
                              "importance": "major",
                              "icon": "milestone"
                            }
                          ]
                        }

                        MAX_EVENTS = %d

                        Nội dung tài liệu:
                        <<<
                        %s
                        >>>
                        """,
                modeDesc, additional, maxEvents, content);
    }

    /**
     * Lưu events vào database.
     */
    @Transactional
    public List<UUID> saveEventsToDatabase(Notebook notebook, User user, NotebookAiSet aiSet,
            List<Map<String, Object>> eventsData) {

        List<UUID> savedIds = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        int orderIndex = 1;
        for (Map<String, Object> eventData : eventsData) {
            Integer order = eventData.get("order") != null
                    ? ((Number) eventData.get("order")).intValue()
                    : orderIndex;

            TimelineEvent event = TimelineEvent.builder()
                    .notebook(notebook)
                    .notebookAiSets(aiSet)
                    .createdBy(user)
                    .eventOrder(order)
                    .date((String) eventData.get("date"))
                    .datePrecision((String) eventData.getOrDefault("datePrecision", "unknown"))
                    .title((String) eventData.get("title"))
                    .description((String) eventData.get("description"))
                    .importance((String) eventData.getOrDefault("importance", "normal"))
                    .icon((String) eventData.get("icon"))
                    .createdAt(now)
                    .build();

            TimelineEvent saved = timelineEventRepository.save(event);
            savedIds.add(saved.getId());
            orderIndex++;
        }

        return savedIds;
    }
}
