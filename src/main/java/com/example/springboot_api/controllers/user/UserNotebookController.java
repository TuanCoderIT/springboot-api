package com.example.springboot_api.controllers.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.user.notebook.CreatePersonalNotebookRequest;
import com.example.springboot_api.dto.user.notebook.PersonalNotebookResponse;
import com.example.springboot_api.services.user.UserNotebookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/personal-notebooks")
@RequiredArgsConstructor
public class UserNotebookController {

    private final UserNotebookService service;

    /**
     * Tạo notebook cá nhân mới - Hỗ trợ 2 mode:
     * 
     * MODE 1 (Manual): autoGenerate = false hoặc không truyền
     * - Yêu cầu title (bắt buộc) + thumbnail (bắt buộc)
     * 
     * MODE 2 (Auto): autoGenerate = true
     * - Yêu cầu description (bắt buộc, ≥10 từ)
     * - Hệ thống tự tạo title và thumbnail
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalNotebookResponse createPersonalNotebook(
            @Valid @RequestPart("data") CreatePersonalNotebookRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.createPersonalNotebook(request, thumbnail, user.getId());
    }

    /**
     * Xóa notebook cá nhân
     */
    @DeleteMapping("/{notebookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePersonalNotebook(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal UserPrincipal user) {
        service.deletePersonalNotebook(notebookId, user.getId());
    }

    /**
     * Lấy danh sách notebook cá nhân của user
     */
    @GetMapping
    public PagedResponse<PersonalNotebookResponse> getMyPersonalNotebooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.getMyPersonalNotebooks(user.getId(), q, sortBy, sortDir, page, size);
    }
}
