package com.example.springboot_api.services.shared.ai;

import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.springboot_api.dto.websocket.AiTaskNotification;
import com.example.springboot_api.dto.websocket.AiTaskNotification.CreatorInfo;
import com.example.springboot_api.dto.websocket.AiTaskProgressMessage;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý push WebSocket events cho AI task progress.
 * 
 * Có 2 loại channels:
 * 1. Task Owner Channel: /topic/ai-task/{aiSetId} - Chi tiết progress
 * 2. Notebook Channel: /topic/notebook/{notebookId}/ai-tasks - Notify members
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==========================================
    // TASK OWNER CHANNEL - Chi tiết progress
    // Topic: /topic/ai-task/{aiSetId}
    // ==========================================

    /**
     * Gửi progress update cho Task Owner.
     */
    public void sendProgress(UUID aiSetId, String step, int progress, String message) {
        if (aiSetId == null)
            return;

        AiTaskProgressMessage msg = AiTaskProgressMessage.progress(aiSetId, step, progress, message);
        String topic = "/topic/ai-task/" + aiSetId;

        try {
            messagingTemplate.convertAndSend(topic, msg);
            log.debug("📡 [WS] Progress → {}: {}% - {}", aiSetId.toString().substring(0, 8), progress, step);
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to send progress: {}", e.getMessage());
        }
    }

    /**
     * Gửi queued status cho Task Owner.
     */
    public void sendQueued(UUID aiSetId, String setType) {
        if (aiSetId == null)
            return;

        AiTaskProgressMessage msg = AiTaskProgressMessage.queued(aiSetId, setType);
        String topic = "/topic/ai-task/" + aiSetId;

        try {
            messagingTemplate.convertAndSend(topic, msg);
            log.debug("📡 [WS] Queued → {}", aiSetId.toString().substring(0, 8));
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to send queued: {}", e.getMessage());
        }
    }

    /**
     * Gửi done status cho Task Owner.
     */
    public void sendDone(UUID aiSetId, String setType, Map<String, Object> data) {
        if (aiSetId == null)
            return;

        AiTaskProgressMessage msg = AiTaskProgressMessage.done(aiSetId, setType, data);
        String topic = "/topic/ai-task/" + aiSetId;

        try {
            messagingTemplate.convertAndSend(topic, msg);
            log.info("📡 [WS] Done → {}", aiSetId.toString().substring(0, 8));
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to send done: {}", e.getMessage());
        }
    }

    /**
     * Gửi failed status cho Task Owner.
     */
    public void sendFailed(UUID aiSetId, String errorMessage) {
        if (aiSetId == null)
            return;

        AiTaskProgressMessage msg = AiTaskProgressMessage.failed(aiSetId, errorMessage);
        String topic = "/topic/ai-task/" + aiSetId;

        try {
            messagingTemplate.convertAndSend(topic, msg);
            log.info("📡 [WS] Failed → {}: {}", aiSetId.toString().substring(0, 8), errorMessage);
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to send failed: {}", e.getMessage());
        }
    }

    // ==========================================
    // NOTEBOOK CHANNEL - Notify all members
    // Topic: /topic/notebook/{notebookId}/ai-tasks
    // ==========================================

    /**
     * Notify notebook members: task created.
     */
    public void notifyCreated(NotebookAiSet aiSet) {
        if (aiSet == null || aiSet.getNotebook() == null)
            return;

        UUID notebookId = aiSet.getNotebook().getId();
        AiTaskNotification notification = AiTaskNotification.created(
                aiSet.getId(),
                notebookId,
                aiSet.getSetType(),
                aiSet.getTitle(),
                toCreatorInfo(aiSet.getCreatedBy()));

        sendToNotebook(notebookId, notification);
    }

    /**
     * Notify notebook members: task done.
     */
    public void notifyDone(NotebookAiSet aiSet) {
        if (aiSet == null || aiSet.getNotebook() == null)
            return;

        UUID notebookId = aiSet.getNotebook().getId();
        AiTaskNotification notification = AiTaskNotification.done(
                aiSet.getId(),
                notebookId,
                aiSet.getSetType(),
                aiSet.getTitle(),
                toCreatorInfo(aiSet.getCreatedBy()));

        sendToNotebook(notebookId, notification);
    }

    /**
     * Notify notebook members: task deleted.
     */
    public void notifyDeleted(NotebookAiSet aiSet, User deletedBy) {
        if (aiSet == null || aiSet.getNotebook() == null)
            return;

        UUID notebookId = aiSet.getNotebook().getId();
        AiTaskNotification notification = AiTaskNotification.deleted(
                aiSet.getId(),
                notebookId,
                aiSet.getSetType(),
                aiSet.getTitle(),
                toCreatorInfo(deletedBy));

        sendToNotebook(notebookId, notification);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void sendToNotebook(UUID notebookId, AiTaskNotification notification) {
        String topic = "/topic/notebook/" + notebookId + "/ai-tasks";

        try {
            messagingTemplate.convertAndSend(topic, notification);
            log.info("📡 [WS] Notebook {} → {} {}",
                    notebookId.toString().substring(0, 8),
                    notification.getType(),
                    notification.getSetType());
        } catch (Exception e) {
            log.warn("⚠️ [WS] Failed to notify notebook: {}", e.getMessage());
        }
    }

    private CreatorInfo toCreatorInfo(User user) {
        if (user == null) {
            return CreatorInfo.builder()
                    .id(null)
                    .fullName("Unknown")
                    .avatarUrl(null)
                    .build();
        }
        return CreatorInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
