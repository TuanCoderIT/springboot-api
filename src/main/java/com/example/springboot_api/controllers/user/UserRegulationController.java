package com.example.springboot_api.controllers.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.common.dto.ApiResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.user.regulation.GetUserRegulationFilesRequest;
import com.example.springboot_api.dto.user.regulation.UserRegulationFileResponse;
import com.example.springboot_api.dto.user.regulation.UserRegulationNotebookResponse;
import com.example.springboot_api.services.user.UserRegulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * User API để xem tài liệu quy chế.
 */
@RestController
@RequestMapping("/user/regulation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Regulation", description = "API xem tài liệu quy chế (user)")
public class UserRegulationController {

    private final UserRegulationService regulationService;

    @GetMapping("/notebook")
    @Operation(summary = "Lấy thông tin regulation notebook")
    public ResponseEntity<ApiResponse<UserRegulationNotebookResponse>> getRegulationNotebook() {
        UserRegulationNotebookResponse notebook = regulationService.getRegulationNotebook();
        return ResponseEntity.ok(ApiResponse.success(notebook, "Regulation notebook"));
    }

    @GetMapping("/files")
    @Operation(summary = "Lấy danh sách file quy chế (chỉ done/approved)")
    public ResponseEntity<ApiResponse<PagedResponse<UserRegulationFileResponse>>> getRegulationFiles(
            @Valid @ModelAttribute GetUserRegulationFilesRequest request) {

        PagedResponse<UserRegulationFileResponse> files = regulationService.getRegulationFiles(request);
        return ResponseEntity.ok(ApiResponse.success(files, "Regulation files"));
    }
}
