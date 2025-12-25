package com.example.springboot_api.services.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.user.regulation.GetUserRegulationFilesRequest;
import com.example.springboot_api.dto.user.regulation.UserRegulationFileResponse;
import com.example.springboot_api.dto.user.regulation.UserRegulationNotebookResponse;
import com.example.springboot_api.mappers.UserRegulationMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý tài liệu quy chế cho User.
 */
@Service
@RequiredArgsConstructor
public class UserRegulationService {

        private final NotebookRepository notebookRepo;
        private final NotebookFileRepository fileRepo;
        private final UserRegulationMapper mapper;

        private static final String REGULATION_TYPE = "regulation";

        /**
         * Lấy regulation notebook với thống kê đơn giản.
         */
        @Transactional(readOnly = true)
        public UserRegulationNotebookResponse getRegulationNotebook() {
                Notebook notebook = notebookRepo.findByType(REGULATION_TYPE).stream()
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException("Regulation notebook not found"));

                // Đếm file done/approved
                long totalFiles = fileRepo.countByNotebookIdAndStatusIn(
                                notebook.getId(),
                                java.util.Arrays.asList("done", "approved"));

                return mapper.toNotebookResponse(notebook, totalFiles);
        }

        /**
         * Lấy UUID của regulation notebook (dùng cho chat service).
         */
        @Transactional(readOnly = true)
        public UUID getRegulationNotebookId() {
                return notebookRepo.findByType(REGULATION_TYPE).stream()
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException("Regulation notebook not found"))
                                .getId();
        }

        /**
         * Lấy danh sách file quy chế (chỉ done/approved).
         */
        @Transactional(readOnly = true)
        public PagedResponse<UserRegulationFileResponse> getRegulationFiles(GetUserRegulationFilesRequest request) {
                Notebook notebook = notebookRepo.findByType(REGULATION_TYPE).stream()
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException("Regulation notebook not found"));

                // Build pageable
                String sortField = mapToSQLColumnName(request.getSortBy());
                Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;

                Pageable pageable = PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(direction, sortField));

                // Fetch files - chỉ done/approved
                Page<NotebookFile> page = fileRepo.findByNotebookIdAndStatusInAndSearch(
                                notebook.getId(),
                                java.util.Arrays.asList("done", "approved"),
                                request.getSearch(),
                                pageable);

                // Map to DTO
                java.util.List<UserRegulationFileResponse> items = page.getContent().stream()
                                .map(mapper::toFileResponse)
                                .toList();

                // Build metadata
                PagedResponse.Meta meta = new PagedResponse.Meta(
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements(),
                                page.getTotalPages());

                return new PagedResponse<>(items, meta);
        }

        // Helper method
        private String mapToSQLColumnName(String jpaPropertyName) {
                return switch (jpaPropertyName) {
                        case "originalFilename" -> "original_filename";
                        case "fileSize" -> "file_size";
                        case "createdAt" -> "created_at";
                        case "updatedAt" -> "updated_at";
                        default -> "created_at"; // Fallback to createdAt for invalid fields
                };
        }
}
