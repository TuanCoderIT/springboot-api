package com.example.springboot_api.controllers.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.bot.ChatHistoryResponse;
import com.example.springboot_api.services.user.BotChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/notebooks/{notebookId}/bot-chat")
@RequiredArgsConstructor
public class BotChatController {

    private final BotChatService botChatService;

    /**
     * Gửi message đến bot
     * POST /user/notebooks/{notebookId}/bot-chat
     */
    @PostMapping
    public void sendMessageToBot(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestBody String message) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        // TODO: Implement logic gửi message đến bot
    }

    /**
     * Lấy lịch sử chat với bot với cursor pagination
     * GET /user/notebooks/{notebookId}/bot-chat/history?cursor_next={uuid}&limit=20
     * 
     * - Lần đầu: không có cursor_next, trả về các message mới nhất
     * - Lần sau: gửi cursor_next từ response trước, trả về các message cũ hơn
     * - Càng lướt lên thì càng hiển thị thêm nội dung tin nhắn cũ trước đó
     */
    @GetMapping("/history")
    public ChatHistoryResponse getChatHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String cursorNext,
            @RequestParam(defaultValue = "20") int limit) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        return botChatService.getChatHistory(user.getId(), notebookId, cursorNext, limit);
    }

    /**
     * Xóa lịch sử chat với bot
     * DELETE /user/notebooks/{notebookId}/bot-chat/history
     */
    @PostMapping("/history/clear")
    public void clearChatHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        // TODO: Implement logic xóa lịch sử chat
    }

}
