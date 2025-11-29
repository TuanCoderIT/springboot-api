package com.example.springboot_api.modules.notebook.service;

import com.example.springboot_api.modules.notebook.dto.NotebookCreateRequest;
import com.example.springboot_api.modules.notebook.dto.NotebookResponse;
import com.example.springboot_api.modules.notebook.dto.NotebookUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotebookService {

    NotebookResponse createNotebook(NotebookCreateRequest request, UUID currentUserId);

    NotebookResponse getNotebook(UUID notebookId, UUID currentUserId);

    Page<NotebookResponse> listMyNotebooks(UUID currentUserId, Pageable pageable);

    Page<NotebookResponse> listCommunityNotebooks(Pageable pageable);

    NotebookResponse updateNotebook(UUID notebookId, NotebookUpdateRequest request, UUID currentUserId);

    void deleteNotebook(UUID notebookId, UUID currentUserId);
}
