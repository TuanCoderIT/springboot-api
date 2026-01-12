package com.example.springboot_api.services.shared.ai;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý status của NotebookAiSet.
 * Tách riêng để tái sử dụng trong nhiều generation services.
 * 
 * Tự động push WebSocket events khi status thay đổi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiSetStatusService {

    private final NotebookAiSetRepository aiSetRepository;
    private final AiTaskProgressService progressService;

    /**
     * Cập nhật status của NotebookAiSet.
     * 
     * @param aiSetId      ID của AI Set
     * @param status       Status mới (pending, processing, done, failed)
     * @param errorMessage Thông báo lỗi (nếu status = failed)
     * @param outputStats  Output statistics (nếu status = done)
     */
    @Transactional
    public void updateStatus(UUID aiSetId, String status, String errorMessage, Map<String, Object> outputStats) {
        aiSetRepository.findById(aiSetId).ifPresent(aiSet -> {
            aiSet.setStatus(status);
            aiSet.setErrorMessage(errorMessage);
            aiSet.setUpdatedAt(OffsetDateTime.now());

            if ("processing".equals(status)) {
                aiSet.setStartedAt(OffsetDateTime.now());
            }
            if ("done".equals(status) || "failed".equals(status)) {
                aiSet.setFinishedAt(OffsetDateTime.now());
            }
            if (outputStats != null) {
                aiSet.setOutputStats(outputStats);
            }
            aiSetRepository.save(aiSet);
            log.debug("Updated AiSet {} status to: {}", aiSetId, status);

            // Push WebSocket events
            pushWebSocketEvents(aiSet, status, errorMessage, outputStats);
        });
    }

    /**
     * Đánh dấu AI Set đang xử lý.
     */
    @Transactional
    public void markProcessing(UUID aiSetId) {
        updateStatus(aiSetId, "processing", null, null);
    }

    /**
     * Đánh dấu AI Set hoàn thành.
     */
    @Transactional
    public void markDone(UUID aiSetId, Map<String, Object> outputStats) {
        updateStatus(aiSetId, "done", null, outputStats);
    }

    /**
     * Đánh dấu AI Set thất bại.
     */
    @Transactional
    public void markFailed(UUID aiSetId, String errorMessage) {
        updateStatus(aiSetId, "failed", errorMessage, null);
    }

    /**
     * Cập nhật title cho AI Set.
     */
    @Transactional
    public void updateTitle(UUID aiSetId, String title) {
        aiSetRepository.findById(aiSetId).ifPresent(aiSet -> {
            aiSet.setTitle(title);
            aiSet.setUpdatedAt(OffsetDateTime.now());
            aiSetRepository.save(aiSet);
        });
    }

    /**
     * Push WebSocket events dựa trên status thay đổi.
     */
    private void pushWebSocketEvents(NotebookAiSet aiSet, String status, String errorMessage,
            Map<String, Object> outputStats) {
        try {
            switch (status) {
                case "processing" -> {
                    // Gửi cho Task Owner: đang xử lý
                    progressService.sendProgress(aiSet.getId(), "processing", 10, "Đang xử lý...");
                }
                case "done" -> {
                    // Gửi cho Task Owner: hoàn thành
                    progressService.sendDone(aiSet.getId(), aiSet.getSetType(), outputStats);
                    // Notify tất cả Notebook Members
                    progressService.notifyDone(aiSet);
                }
                case "failed" -> {
                    // Gửi cho Task Owner: thất bại
                    progressService.sendFailed(aiSet.getId(), errorMessage);
                }
                default -> {
                    // Các status khác (queued, pending)
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to push events for {}: {}", aiSet.getId(), e.getMessage());
        }
    }
}
