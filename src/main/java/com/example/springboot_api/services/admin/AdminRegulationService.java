package com.example.springboot_api.services.admin;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.regulation.GetRegulationFilesRequest;
import com.example.springboot_api.dto.admin.regulation.RegulationFileResponse;
import com.example.springboot_api.dto.admin.regulation.RegulationFileUploadRequest;
import com.example.springboot_api.dto.admin.regulation.RegulationNotebookResponse;
import com.example.springboot_api.dto.admin.regulation.RenameRegulationFileRequest;
import com.example.springboot_api.dto.admin.regulation.UpdateRegulationNotebookRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.RegulationMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý tài liệu quy chế cho Admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegulationService {

    private final NotebookRepository notebookRepo;
    private final NotebookFileRepository fileRepo;
    private final UserRepository userRepo;
    private final FileStorageService fileStorageService;
    private final FileProcessingTaskService processingTaskService;
    private final RegulationMapper regulationMapper;

    private static final String REGULATION_TYPE = "regulation";

    /**
     * Lấy regulation notebook với statistics.
     */
    @Transactional(readOnly = true)
    public RegulationNotebookResponse getRegulationNotebook() {
        Notebook notebook = notebookRepo.findByType(REGULATION_TYPE).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Regulation notebook chưa được tạo. " +
                                "Vui lòng tạo notebook với type='regulation' trước trong database."));

        // Calculate statistics
        long totalFiles = fileRepo.countByNotebookId(notebook.getId());
        long pendingFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "pending");
        long approvedFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "approved");
        long processingFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "processing");
        long failedFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "failed");
        long ocrDoneFiles = fileRepo.countByNotebookIdAndOcrDone(notebook.getId(), true);
        long embeddingDoneFiles = fileRepo.countByNotebookIdAndEmbeddingDone(notebook.getId(), true);

        return regulationMapper.toNotebookResponse(
                notebook,
                totalFiles,
                pendingFiles,
                approvedFiles,
                processingFiles,
                failedFiles,
                ocrDoneFiles,
                embeddingDoneFiles);
    }

    /**
     * Lấy danh sách tài liệu quy chế (có phân trang, lọc, sắp xếp).
     */
    @Transactional(readOnly = true)
    public PagedResponse<RegulationFileResponse> getRegulationFiles(GetRegulationFilesRequest request) {
        Notebook notebook = getRegulationNotebookEntity();

        // Build Pageable - convert JPA property names to SQL column names
        String sortField = mapToSQLColumnName(request.getSortBy());
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                request.getPage(),
                request.getSize(),
                org.springframework.data.domain.Sort.by(direction, sortField));

        // Fetch from repository với filters
        org.springframework.data.domain.Page<NotebookFile> page = fileRepo.findByNotebookIdWithFilters(
                notebook.getId(),
                request.getStatus(),
                null, // mimeType
                null, // ocrDone
                null, // embeddingDone
                null, // uploadedBy
                request.getSearch(),
                pageable);

        // Map to DTO
        java.util.List<RegulationFileResponse> items = page.getContent().stream()
                .map(regulationMapper::toFileResponse)
                .toList();

        // Build metadata
        PagedResponse.Meta meta = new PagedResponse.Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());

        return new PagedResponse<>(items, meta);
    }

    /**
     * Cập nhật thông tin regulation notebook.
     */
    @Transactional
    public RegulationNotebookResponse updateRegulationNotebook(UpdateRegulationNotebookRequest request) {
        Notebook notebook = getRegulationNotebookEntity();

        if (request.getTitle() != null) {
            notebook.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            notebook.setDescription(request.getDescription());
        }

        notebook = notebookRepo.save(notebook);

        // Calculate statistics for response
        long totalFiles = fileRepo.countByNotebookId(notebook.getId());
        long pendingFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "pending");
        long approvedFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "approved");
        long processingFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "processing");
        long failedFiles = fileRepo.countByNotebookIdAndStatus(notebook.getId(), "failed");
        long ocrDoneFiles = fileRepo.countByNotebookIdAndOcrDone(notebook.getId(), true);
        long embeddingDoneFiles = fileRepo.countByNotebookIdAndEmbeddingDone(notebook.getId(), true);

        return regulationMapper.toNotebookResponse(
                notebook,
                totalFiles,
                pendingFiles,
                approvedFiles,
                processingFiles,
                failedFiles,
                ocrDoneFiles,
                embeddingDoneFiles);
    }

    /**
     * Upload tài liệu quy chế (PDF/DOCX).
     */
    // @Transactional
    public List<RegulationFileResponse> uploadRegulationFiles(
            UUID adminId,
            List<MultipartFile> files,
            RegulationFileUploadRequest request) throws IOException {

        Notebook notebook = getRegulationNotebookEntity();
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        List<RegulationFileResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String mimeType = getMimeType(file);
                String storageUrl = fileStorageService.storeFile(file);

                OffsetDateTime now = OffsetDateTime.now();

                NotebookFile notebookFile = NotebookFile.builder()
                        .notebook(notebook)
                        .uploadedBy(admin)
                        .originalFilename(file.getOriginalFilename())
                        .mimeType(mimeType)
                        .fileSize(file.getSize())
                        .storageUrl(storageUrl)
                        .status("pending")
                        .ocrDone(false)
                        .embeddingDone(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .chunkSize(request.getChunkSize())
                        .chunkOverlap(request.getChunkOverlap())
                        .build();

                notebookFile = fileRepo.save(notebookFile);
                processingTaskService.startAIProcessing(notebookFile);
                responses.add(regulationMapper.toFileResponse(notebookFile));

            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Upload failed: " + file.getOriginalFilename(), e);
            }
        }

        return responses;
    }

    /**
     * Đổi tên file (giữ nguyên đuôi mở rộng).
     * Chỉ cập nhật tên hiển thị (originalFilename) trong DB.
     * File vật lý trong storage giữ nguyên tên UUID.
     */
    @Transactional
    public RegulationFileResponse renameFile(UUID fileId, RenameRegulationFileRequest request) {
        NotebookFile file = fileRepo.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        String oldFilename = file.getOriginalFilename();
        String newFilename = request.getNewFilename();

        // Validate extension
        String oldExt = getExtension(oldFilename);
        String newExt = getExtension(newFilename);

        if (!oldExt.equalsIgnoreCase(newExt)) {
            throw new IllegalArgumentException(
                    String.format("Không được đổi đuôi file (cũ: %s, mới: %s)", oldExt, newExt));
        }

        // Update DB - chỉ đổi tên hiển thị
        file.setOriginalFilename(newFilename);
        file.setUpdatedAt(OffsetDateTime.now());

        file = fileRepo.save(file);
        return regulationMapper.toFileResponse(file);
    }

    /**
     * Retry OCR cho file bị lỗi hoặc cần xử lý lại.
     */
    @Transactional
    public RegulationFileResponse retryOcr(UUID fileId) {
        NotebookFile file = fileRepo.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        // Reset trạng thái
        file.setStatus("pending");
        file.setOcrDone(false);
        file.setEmbeddingDone(false);
        file.setUpdatedAt(OffsetDateTime.now());
        file = fileRepo.save(file);

        // Trigger lại AI processing
        processingTaskService.startAIProcessing(file);

        return regulationMapper.toFileResponse(file);
    }

    /**
     * Xóa tài liệu quy chế.
     * Xóa cả trong DB và file vật lý.
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        NotebookFile file = fileRepo.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        // Xóa file vật lý
        fileStorageService.deleteFile(file.getStorageUrl());

        // Xóa trong DB (cascade sẽ xóa chunks và relations)
        fileRepo.delete(file);
    }

    // ==================== Private Methods ====================

    private Notebook getRegulationNotebookEntity() {
        return notebookRepo.findByType(REGULATION_TYPE).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Regulation notebook chưa được tạo. " +
                                "Vui lòng tạo notebook với type='regulation' trước trong database."));
    }

    private String getMimeType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf"))
                return "application/pdf";
            if (lower.endsWith(".docx"))
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (lower.endsWith(".doc"))
                return "application/msword";
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Could not determine file type");
        }
        return contentType;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * Map JPA property names to SQL column names for native queries.
     */
    private String mapToSQLColumnName(String jpaPropertyName) {
        return switch (jpaPropertyName) {
            case "originalFilename" -> "original_filename";
            case "fileSize" -> "file_size";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            default -> jpaPropertyName;
        };
    }
}
