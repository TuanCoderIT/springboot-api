package com.example.springboot_api.controller;

import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.modules.notebook.dto.NotebookCreateRequest;
import com.example.springboot_api.modules.notebook.dto.NotebookResponse;
import com.example.springboot_api.modules.notebook.service.NotebookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public NotebookResponse createNotebook(@Valid @RequestBody NotebookCreateRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return notebookService.createNotebook(request, userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public NotebookResponse getNotebook(@PathVariable("id") UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return notebookService.getNotebook(id, userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Page<NotebookResponse> listMyNotebooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return notebookService.listMyNotebooks(userId, pageable);
    }
}
