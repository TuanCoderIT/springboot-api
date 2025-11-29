package com.example.springboot_api.modules.notebook.service.impl;

import com.example.springboot_api.modules.notebook.dto.NotebookCreateRequest;
import com.example.springboot_api.modules.notebook.dto.NotebookResponse;
import com.example.springboot_api.modules.notebook.dto.NotebookUpdateRequest;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;
import com.example.springboot_api.modules.notebook.mapper.NotebookMapper;
import com.example.springboot_api.modules.notebook.repository.NotebookRepository;
import com.example.springboot_api.modules.notebook.service.NotebookService;
import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotebookServiceImpl implements NotebookService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookMapper notebookMapper;

    @Override
    public NotebookResponse createNotebook(NotebookCreateRequest request, UUID currentUserId) {
        User creator = userRepository.findById(currentUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Notebook notebook = notebookMapper.toEntity(request);
        notebook.setCreatedBy(creator);
        notebook.setCreatedAt(OffsetDateTime.now());
        notebook.setUpdatedAt(OffsetDateTime.now());

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toResponse(saved);
    }

    @Override
    public NotebookResponse getNotebook(UUID notebookId, UUID currentUserId) {
        Notebook notebook = findByIdOrThrow(notebookId);

        // TODO: kiểm tra quyền truy cập notebook (public, member, owner, admin, ...)
        // tạm thời bỏ qua

        return notebookMapper.toResponse(notebook);
    }

    @Override
    public Page<NotebookResponse> listMyNotebooks(UUID currentUserId, Pageable pageable) {
        return notebookRepository
            .findByCreatedBy_Id(currentUserId, pageable)
            .map(notebookMapper::toResponse);
    }

    @Override
    public Page<NotebookResponse> listCommunityNotebooks(Pageable pageable) {
        return notebookRepository
            .findByTypeAndVisibility(
                NotebookType.community,
                NotebookVisibility.PUBLIC,
                pageable
            )
            .map(notebookMapper::toResponse);
    }

    @Override
    public NotebookResponse updateNotebook(UUID notebookId, NotebookUpdateRequest request, UUID currentUserId) {
        Notebook notebook = findByIdOrThrow(notebookId);

        // TODO: kiểm tra currentUserId có phải owner/admin không

        notebookMapper.updateEntityFromRequest(request, notebook);
        notebook.setUpdatedAt(OffsetDateTime.now());

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toResponse(saved);
    }

    @Override
    public void deleteNotebook(UUID notebookId, UUID currentUserId) {
        Notebook notebook = findByIdOrThrow(notebookId);

        // TODO: kiểm tra quyền xóa

        notebookRepository.delete(notebook);
    }

    private Notebook findByIdOrThrow(UUID notebookId) {
        return notebookRepository.findById(notebookId)
            .orElseThrow(() -> new EntityNotFoundException("Notebook not found"));
    }
}

