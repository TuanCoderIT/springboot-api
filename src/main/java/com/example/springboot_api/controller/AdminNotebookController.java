package com.example.springboot_api.controller;

import com.example.springboot_api.modules.notebook.dto.NotebookResponse;
import com.example.springboot_api.modules.notebook.service.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notebooks")
@RequiredArgsConstructor
public class AdminNotebookController {

    private final NotebookService notebookService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<NotebookResponse> listAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        // tạm reuse listCommunityNotebooks, hoặc bạn có thể thêm hàm listAllNotebooks()
        return notebookService.listCommunityNotebooks(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteNotebook(@PathVariable("id") UUID id) {
        // ADMIN xoá không cần currentUserId, bạn có thể truyền null/UUID.fromString("...") tùy logic
        notebookService.deleteNotebook(id, null);
    }
}

