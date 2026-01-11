package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.chatbot.ChatResponse;
import com.example.springboot_api.dto.user.chatbot.ConversationItem;
import com.example.springboot_api.dto.user.chatbot.ListConversationsResponse;
import com.example.springboot_api.dto.user.chatbot.ListMessagesResponse;
import com.example.springboot_api.dto.user.chatbot.RegulationChatRequest;
import com.example.springboot_api.dto.user.chatbot.UploadedImageResponse;
import com.example.springboot_api.services.user.ChatBotService;
import com.example.springboot_api.services.user.UserRegulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * API chat lịch sử cho regulation (công văn quy chế).
 * Tận dụng ChatBotService, chỉ thay đổi notebookId là regulation notebook.
 */
@RestController
@RequestMapping("/user/regulation/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Regulation Chat", description = "API chat về công văn quy chế")
public class UserRegulationChatController {

    private final ChatBotService chatBotService;
    private final UserRegulationService regulationService;

    /**
     * Upload ảnh cho chat.
     * POST /user/regulation/chat/images/upload
     */
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh cho chat")
    public ResponseEntity<List<UploadedImageResponse>> uploadImages(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("files") List<MultipartFile> files) {

        // Validate: tối đa 3 ảnh
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 ảnh");
        }
        if (files.size() > 3) {
            throw new BadRequestException("Tối đa 3 ảnh mỗi lần upload");
        }

        // Validate MIME type
        List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String mimeType = file.getContentType();
            if (mimeType == null || !allowedMimeTypes.contains(mimeType.toLowerCase())) {
                throw new BadRequestException(
                        "Chỉ chấp nhận ảnh định dạng: jpeg, png, gif, webp. File không hợp lệ: "
                                + file.getOriginalFilename());
            }
        }

        List<UploadedImageResponse> result = chatBotService.uploadChatImages(files);
        return ResponseEntity.ok(result);
    }

    /**
     * Gửi tin nhắn chat với bot.
     * POST /user/regulation/chat
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gửi tin nhắn chat")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody RegulationChatRequest request) {

        UUID notebookId = regulationService.getRegulationNotebookId();

        ChatResponse response = chatBotService.regulationChat(notebookId, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo conversation mới.
     */
    @PostMapping("/conversations")
    @Operation(summary = "Tạo conversation mới")
    public ResponseEntity<ConversationItem> createConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String title) {

        UUID notebookId = regulationService.getRegulationNotebookId();
        ConversationItem response = chatBotService.createConversation(notebookId, user.getId(), title);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách conversations (cursor pagination).
     */
    @GetMapping("/conversations")
    @Operation(summary = "Lấy danh sách conversations")
    public ResponseEntity<ListConversationsResponse> getConversations(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID cursorNext,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        UUID notebookId = regulationService.getRegulationNotebookId();
        ListConversationsResponse response = chatBotService.listConversations(notebookId, user.getId(), cursorNext,
                limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy conversation đang active.
     */
    @GetMapping("/conversations/active")
    @Operation(summary = "Lấy conversation đang active")
    public ResponseEntity<ConversationItem> getActiveConversation(
            @AuthenticationPrincipal UserPrincipal user) {

        UUID notebookId = regulationService.getRegulationNotebookId();
        ConversationItem response = chatBotService.getActiveConversation(notebookId, user.getId());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Set conversation thành active.
     */
    @PostMapping("/conversations/{conversationId}/active")
    @Operation(summary = "Set conversation thành active")
    public ResponseEntity<ConversationItem> setActiveConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID conversationId) {

        UUID notebookId = regulationService.getRegulationNotebookId();
        ConversationItem response = chatBotService.setActiveConversation(notebookId, user.getId(), conversationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách messages của conversation (cursor pagination).
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Lấy danh sách messages")
    public ResponseEntity<ListMessagesResponse> getMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID cursorNext) {

        ListMessagesResponse response = chatBotService.listMessages(conversationId, cursorNext);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa conversation.
     * Nếu xóa conversation đang active, trả về conversation tiếp theo.
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Xóa conversation")
    public ResponseEntity<ConversationItem> deleteConversation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID conversationId) {

        UUID notebookId = regulationService.getRegulationNotebookId();
        ConversationItem nextActive = chatBotService.deleteConversation(notebookId, user.getId(), conversationId);

        if (nextActive != null) {
            return ResponseEntity.ok(nextActive);
        }
        return ResponseEntity.noContent().build();
    }
}
