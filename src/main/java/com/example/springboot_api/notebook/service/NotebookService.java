package com.example.springboot_api.notebook.service;

import com.example.springboot_api.auth.entity.User;
import com.example.springboot_api.notebook.dto.NotebookRequest;
import com.example.springboot_api.notebook.dto.NotebookResponse;
import com.example.springboot_api.notebook.entity.Notebook;
import com.example.springboot_api.notebook.repository.NotebookRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;
// import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookRepository notebookRepository;

    // Tạo notebook mới cho user đang đăng nhập
    public NotebookResponse createNotebook(User user, NotebookRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề không được để trống");
        }

        Instant now = Instant.now();

        Notebook notebook = Notebook.builder()
                .user(user)
                .title(req.getTitle())
                .description(req.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        notebookRepository.save(notebook);

        return mapToResponse(notebook);
    }

    // Lấy list notebook của user (có phân trang + search)
    public Page<NotebookResponse> listNotebooks(User user, int page, int size, String search) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notebook> notebooksPage;
        if (search != null && !search.isBlank()) {
            notebooksPage = notebookRepository.findByUserIdAndTitleContainingIgnoreCase(
                    user.getId(), search.trim(), pageable
            );
        } else {
            notebooksPage = notebookRepository.findByUserId(user.getId(), pageable);
        }

        return notebooksPage.map(this::mapToResponse);
    }

    // Lấy chi tiết 1 notebook, đảm bảo thuộc về user
    public NotebookResponse getNotebook(User user, UUID id) {
        Notebook notebook = getOwnedNotebookOrThrow(user, id);
        return mapToResponse(notebook);
    }

    // Cập nhật notebook
    public NotebookResponse updateNotebook(User user, UUID id, NotebookRequest req) {
        Notebook notebook = getOwnedNotebookOrThrow(user, id);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            notebook.setTitle(req.getTitle());
        }
        notebook.setDescription(req.getDescription());
        notebook.setUpdatedAt(Instant.now());

        notebookRepository.save(notebook);

        return mapToResponse(notebook);
    }

    // Xóa notebook
    public void deleteNotebook(User user, UUID id) {
        Notebook notebook = getOwnedNotebookOrThrow(user, id);
        notebookRepository.delete(notebook);
    }

    // ================== Helper ==================

    private Notebook getOwnedNotebookOrThrow(User user, UUID id) {
        Notebook notebook = notebookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy notebook"));

        if (!notebook.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập notebook này");
        }
        return notebook;
    }

    private NotebookResponse mapToResponse(Notebook notebook) {
        return NotebookResponse.builder()
                .id(notebook.getId())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .createdAt(notebook.getCreatedAt())
                .updatedAt(notebook.getUpdatedAt())
                .build();
    }
}