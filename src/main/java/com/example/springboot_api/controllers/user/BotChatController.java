package com.example.springboot_api.controllers.user;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.chatbot.ChatRequest;
import com.example.springboot_api.dto.user.chatbot.ChatResponse;
import com.example.springboot_api.dto.user.chatbot.ConversationItem;
import com.example.springboot_api.dto.user.chatbot.ListConversationsResponse;
import com.example.springboot_api.dto.user.chatbot.ListMessagesResponse;
import com.example.springboot_api.dto.user.chatbot.LlmModelResponse;
import com.example.springboot_api.services.user.ChatBotService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/notebooks/{notebookId}/bot-chat")
@RequiredArgsConstructor
public class BotChatController {

    private final ChatBotService chatBotService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo conversation mới với bot
     * POST /user/notebooks/{notebookId}/bot-chat/conversations
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param title      Conversation title (optional)
     * @return ConversationItem
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationItem> createConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String title) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        ConversationItem response = chatBotService.createConversation(
                notebookId,
                user.getId(),
                title);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<ListConversationsResponse> getConversations(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) UUID cursorNext) {

        if (user == null) {
            throw new RuntimeException("Us  er chưa đăng nhập.");
        }

        return ResponseEntity.ok(chatBotService.listConversations(notebookId, user.getId(), cursorNext));
    }

    /**
     * Lấy danh sách các LLM models đang active.
     * GET /user/notebooks/{notebookId}/bot-chat/models
     *
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @return List<LlmModelResponse>
     */
    @GetMapping("/models")
    public ResponseEntity<List<LlmModelResponse>> getModels(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        List<LlmModelResponse> models = chatBotService.listModels();
        return ResponseEntity.ok(models);
    }

    /**
     * Gửi tin nhắn chat với bot
     * POST
     * /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}/chat
     * 
     * @param user           Current authenticated user
     * @param notebookId     Notebook ID
     * @param conversationId Conversation ID
     * @param requestJson    JSON string chứa ChatRequest
     * @param images         Danh sách hình ảnh (optional)
     * @return ChatResponse
     */
    @PostMapping(value = "/conversations/{conversationId}/chat", consumes = { "multipart/form-data" })
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
            throws IOException {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        // Parse ChatRequest từ JSON
        ChatRequest request;
        try {
            request = objectMapper.readValue(requestJson, ChatRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Dữ liệu request không hợp lệ: " + e.getMessage());
        }

        // Set conversationId từ path parameter vào request
        request.setConversationId(conversationId);

        // Validate file type nếu có
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String filename = image.getOriginalFilename();
                    if (filename != null) {
                        String lower = filename.toLowerCase();
                        boolean isValid = lower.endsWith(".jpg") ||
                                lower.endsWith(".jpeg") ||
                                lower.endsWith(".png") ||
                                lower.endsWith(".gif") ||
                                lower.endsWith(".pdf") ||
                                lower.endsWith(".doc") ||
                                lower.endsWith(".docx");
                        if (!isValid) {
                            throw new BadRequestException(
                                    "Chỉ chấp nhận file hình ảnh (jpg, jpeg, png, gif) và document (pdf, doc, docx). File không hợp lệ: "
                                            + filename);
                        }
                    }
                }
            }
        }

        // Gọi service với MultipartFile[] - service sẽ tự lưu file
        ChatResponse response = chatBotService.chat(notebookId, user.getId(), request, images);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách messages của conversation với cursor pagination.
     * GET
     * /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}/messages
     * 
     * @param user           Current authenticated user
     * @param notebookId     Notebook ID
     * @param conversationId Conversation ID
     * @param cursorNext     UUID của message cũ nhất từ lần load trước (optional)
     * @return ListMessagesResponse
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ListMessagesResponse> getMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID cursorNext) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        ListMessagesResponse response = chatBotService.listMessages(conversationId, cursorNext);
        return ResponseEntity.ok(response);
    }

    /**
     * Set conversation thành active cho user trong notebook.
     * POST
     * /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}/active
     * 
     * @param user           Current authenticated user
     * @param notebookId     Notebook ID
     * @param conversationId Conversation ID cần set active
     * @return ConversationItem
     */
    @PostMapping("/conversations/{conversationId}/active")
    public ResponseEntity<ConversationItem> setActiveConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        ConversationItem response = chatBotService.setActiveConversation(
                notebookId,
                user.getId(),
                conversationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy conversation đang active cho user trong notebook.
     * GET /user/notebooks/{notebookId}/bot-chat/conversations/active
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @return ConversationItem hoặc null nếu chưa có active conversation
     */
    @GetMapping("/conversations/active")
    public ResponseEntity<ConversationItem> getActiveConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        ConversationItem response = chatBotService.getActiveConversation(
                notebookId,
                user.getId());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Xóa conversation theo ID.
     * Chỉ người tạo conversation mới được xóa.
     * DELETE /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}
     * 
     * @param user           Current authenticated user
     * @param notebookId     Notebook ID
     * @param conversationId Conversation ID cần xóa
     * @return 204 No Content nếu xóa thành công
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        chatBotService.deleteConversation(notebookId, user.getId(), conversationId);
        return ResponseEntity.noContent().build();
    }
}
