package com.example.springboot_api.notebook.controller;

import com.example.springboot_api.auth.entity.User;
import com.example.springboot_api.notebook.dto.NotebookRequest;
import com.example.springboot_api.notebook.dto.NotebookResponse;
import com.example.springboot_api.notebook.service.NotebookService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    // Tạo notebook mới
    @PostMapping
    public NotebookResponse createNotebook(@AuthenticationPrincipal User user,
                                           @RequestBody NotebookRequest req) {
        return notebookService.createNotebook(user, req);
    }

    // List notebook của user, có phân trang + search
    @GetMapping
    public Page<NotebookResponse> listNotebooks(@AuthenticationPrincipal User user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String search) {
        return notebookService.listNotebooks(user, page, size, search);
    }

    // Lấy chi tiết 1 notebook
    @GetMapping("/{id}")
    public NotebookResponse getNotebook(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        return notebookService.getNotebook(user, id);
    }

    // Cập nhật notebook
    @PutMapping("/{id}")
    public NotebookResponse updateNotebook(@AuthenticationPrincipal User user,
                                           @PathVariable UUID id,
                                           @RequestBody NotebookRequest req) {
        return notebookService.updateNotebook(user, id, req);
    }

    // Xóa notebook
    @DeleteMapping("/{id}")
    public void deleteNotebook(@AuthenticationPrincipal User user,
                               @PathVariable UUID id) {
        notebookService.deleteNotebook(user, id);
    }
}

