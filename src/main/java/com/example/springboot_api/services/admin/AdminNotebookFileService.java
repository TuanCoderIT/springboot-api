package com.example.springboot_api.services.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.notebook.ContributorInfo;
import com.example.springboot_api.dto.admin.notebook.PageResponse;
import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotebookFileService {

    private final FileStorageService fileStorageService;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileProcessingTaskService fileProcessingTaskService;
    private final UrlNormalizer urlNormalizer;

    // ============================
    // ADMIN UPLOAD FILE
    // ============================
    // @Transactional
    public List<NotebookFile> uploadFiles(
            UUID adminId,
            UUID notebookId,
            FileUploadRequest req,
            List<MultipartFile> files) throws IOException {

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin không tồn tại"));

        if (!admin.getRole().equals("ADMIN"))
            throw new BadRequestException("Bạn không có quyền upload file với tư cách admin");

        // Admin upload: trạng thái ban đầu là 'approved'
        String initStatus = "approved";

        List<NotebookFile> saved = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            // 🟢 Tối ưu hóa: Gọi hàm gộp để kiểm tra và lấy MIME Type chỉ MỘT lần
            String normalizedMimeType = getValidatedAndNormalizedMimeType(file);
            validateChunkParams(req);

            String storageUrl = fileStorageService.storeFile(file);

            NotebookFile newFile = NotebookFile.builder()
                    .notebook(notebook)
                    .uploadedBy(admin)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(normalizedMimeType) // Sử dụng MIME type đã chuẩn hóa
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .status(initStatus)
                    .ocrDone(false)
                    .embeddingDone(false)
                    .chunkSize(req.getChunkSize())
                    .chunkOverlap(req.getChunkOverlap())
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

            NotebookFile savedFile = notebookFileRepository.save(newFile);
            saved.add(savedFile);

            fileProcessingTaskService.startAIProcessing(savedFile);
        }

        return saved;
    }

    /**
     * 🟢 PHƯƠNG THỨC TỐI ƯU: Kiểm tra tính hợp lệ và trả về MIME Type chuẩn.
     * (Thay thế cho validateFile và normalizeMimeType)
     */
    private String getValidatedAndNormalizedMimeType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new BadRequestException("Tên file không hợp lệ.");
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        }

        throw new BadRequestException("Chỉ hỗ trợ file PDF và Word (.doc, .docx). File không hợp lệ: " + filename);
    }

    private void validateChunkParams(FileUploadRequest req) {
        if (req.getChunkSize() == null || req.getChunkSize() < 3000 || req.getChunkSize() > 5000)
            throw new BadRequestException("ChunkSize không hợp lệ.");

        if (req.getChunkOverlap() == null || req.getChunkOverlap() < 200
                || req.getChunkOverlap() > 500)
            throw new BadRequestException("ChunkOverlap không hợp lệ.");
    }

    public NotebookFileResponse toResponse(NotebookFile file, Long chunksCount) {
        User uploader = file.getUploadedBy();
        Notebook notebook = file.getNotebook();

        String normalizedStorageUrl = urlNormalizer.normalizeToFull(file.getStorageUrl());
        String normalizedThumbnailUrl = notebook != null
                ? urlNormalizer.normalizeToFull(notebook.getThumbnailUrl())
                : null;

        NotebookFileResponse.NotebookInfo notebookInfo = notebook != null
                ? new NotebookFileResponse.NotebookInfo(
                        notebook.getId(),
                        notebook.getTitle(),
                        notebook.getDescription(),
                        notebook.getType(),
                        notebook.getVisibility(),
                        normalizedThumbnailUrl)
                : null;

        NotebookFileResponse.UploaderInfo uploaderInfo = uploader != null
                ? new NotebookFileResponse.UploaderInfo(
                        uploader.getId(),
                        uploader.getFullName(),
                        uploader.getEmail(),
                        urlNormalizer.normalizeToFull(uploader.getAvatarUrl()))
                : null;

        return new NotebookFileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getMimeType(),
                file.getFileSize(),
                normalizedStorageUrl,
                file.getStatus(),
                file.getPagesCount(),
                file.getOcrDone(),
                file.getEmbeddingDone(),
                file.getChunkSize(),
                file.getChunkOverlap(),
                chunksCount,
                uploaderInfo,
                notebookInfo,
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

    // ============================
    // ADMIN DUYỆT FILE
    // ============================

    @Transactional
    public NotebookFile approveFile(UUID adminId, UUID notebookId, UUID fileId) {

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId))
            throw new BadRequestException("File không thuộc notebook này");

        if (!"pending".equals(file.getStatus()))
            throw new BadRequestException("Chỉ có thể duyệt file có trạng thái pending");

        file.setStatus("approved");
        file.setUpdatedAt(java.time.OffsetDateTime.now());
        NotebookFile saved = notebookFileRepository.save(file);

        fileProcessingTaskService.startAIProcessing(saved);

        return saved;
    }

    @Transactional
    public int approveAllFiles(UUID adminId) {
        List<NotebookFile> pendingFiles = notebookFileRepository.findAllPendingFiles();

        int count = 0;
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        for (NotebookFile file : pendingFiles) {
            file.setStatus("approved");
            file.setUpdatedAt(now);
            NotebookFile saved = notebookFileRepository.save(file);
            fileProcessingTaskService.startAIProcessing(saved);
            count++;
        }

        return count;
    }

    @Transactional
    public int approveAllFilesByNotebook(UUID adminId, UUID notebookId) {
        List<NotebookFile> pendingFiles = notebookFileRepository.findByNotebookIdAndStatus(notebookId, "pending");

        int count = 0;
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        for (NotebookFile file : pendingFiles) {
            file.setStatus("approved");
            file.setUpdatedAt(now);
            NotebookFile saved = notebookFileRepository.save(file);
            fileProcessingTaskService.startAIProcessing(saved);
            count++;
        }

        return count;
    }

    @Transactional
    public NotebookFile rejectFile(UUID adminId, UUID notebookId, UUID fileId) {

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId))
            throw new BadRequestException("File không thuộc notebook này");

        file.setStatus("rejected");
        file.setUpdatedAt(java.time.OffsetDateTime.now());

        return notebookFileRepository.save(file);
    }

    @Transactional
    public void deleteFile(UUID adminId, UUID notebookId, UUID fileId) {
        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId))
            throw new BadRequestException("File không thuộc notebook này");

        fileChunkRepository.deleteByFileId(fileId);

        fileStorageService.deleteFile(file.getStorageUrl());

        notebookFileRepository.delete(file);
    }

    // ============================
    // RETRY FILE PROCESSING
    // ============================

    @Transactional
    public NotebookFile retryFile(UUID adminId, UUID notebookId, UUID fileId) {
        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId))
            throw new BadRequestException("File không thuộc notebook này");

        // Chỉ retry file có status = "failed"
        if (!"failed".equals(file.getStatus())) {
            throw new BadRequestException("Chỉ có thể retry file có status = 'failed'");
        }

        // Xóa các chunks cũ
        fileChunkRepository.deleteByFileId(fileId);

        // Reset status và flags
        file.setStatus("approved");
        file.setOcrDone(false);
        file.setEmbeddingDone(false);
        file.setUpdatedAt(java.time.OffsetDateTime.now());
        NotebookFile savedFile = notebookFileRepository.save(file);

        // Bắt đầu xử lý lại
        fileProcessingTaskService.startAIProcessing(savedFile);

        return savedFile;
    }

    @Transactional
    public int retryAllFailedFiles(UUID adminId, UUID notebookId) {
        List<NotebookFile> failedFiles = notebookFileRepository.findFailedFilesByNotebookId(notebookId);

        int count = 0;
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        for (NotebookFile file : failedFiles) {
            // Xóa các chunks cũ
            fileChunkRepository.deleteByFileId(file.getId());

            // Reset status và flags
            file.setStatus("approved");
            file.setOcrDone(false);
            file.setEmbeddingDone(false);
            file.setUpdatedAt(now);
            NotebookFile savedFile = notebookFileRepository.save(file);

            // Bắt đầu xử lý lại
            fileProcessingTaskService.startAIProcessing(savedFile);
            count++;
        }

        return count;
    }

    // ============================
    // LẤY FILE CHỜ DUYỆT
    // ============================

    @Transactional(readOnly = true)
    public List<NotebookFile> getPendingFiles(UUID notebookId) {
        return notebookFileRepository.findByNotebookIdAndStatus(notebookId, "pending");
    }

    @Transactional(readOnly = true)
    public PageResponse<NotebookFileResponse> getPendingFiles(
            UUID notebookId,
            String mimeType,
            UUID uploadedBy,
            String search,
            String sortBy,
            int page,
            int size) {

        if (page < 0) {
            throw new BadRequestException("Page phải >= 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size phải từ 1 đến 100");
        }

        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        var result = notebookFileRepository.findPendingFilesWithFilters(
                notebookId, mimeType, uploadedBy, search, pageable);

        List<NotebookFile> files = result.getContent();

        List<UUID> userIds = new ArrayList<>();
        List<UUID> notebookIds = new ArrayList<>();
        for (NotebookFile file : files) {
            User uploader = file.getUploadedBy();
            if (uploader != null && uploader.getId() != null) {
                userIds.add(uploader.getId());
            }
            Notebook notebook = file.getNotebook();
            if (notebook != null && notebook.getId() != null) {
                notebookIds.add(notebook.getId());
            }
        }

        List<User> users = userRepository.findAllById(userIds);
        java.util.Map<UUID, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Notebook> notebooks = notebookRepository.findAllById(notebookIds);
        java.util.Map<UUID, Notebook> notebookMap = notebooks.stream()
                .collect(Collectors.toMap(Notebook::getId, n -> n));

        List<NotebookFileResponse> content = files.stream()
                .map(file -> {
                    long chunksCount = fileChunkRepository.countByFileId(file.getId());
                    User uploaderRef = file.getUploadedBy();
                    User uploader = uploaderRef != null && uploaderRef.getId() != null
                            ? userMap.get(uploaderRef.getId())
                            : null;
                    Notebook notebookRef = file.getNotebook();
                    Notebook notebook = notebookRef != null && notebookRef.getId() != null
                            ? notebookMap.get(notebookRef.getId())
                            : null;

                    String normalizedStorageUrl = urlNormalizer.normalizeToFull(file.getStorageUrl());
                    String normalizedThumbnailUrl = notebook != null
                            ? urlNormalizer.normalizeToFull(notebook.getThumbnailUrl())
                            : null;

                    NotebookFileResponse.NotebookInfo notebookInfo = notebook != null
                            ? new NotebookFileResponse.NotebookInfo(
                                    notebook.getId(),
                                    notebook.getTitle(),
                                    notebook.getDescription(),
                                    notebook.getType(),
                                    notebook.getVisibility(),
                                    normalizedThumbnailUrl)
                            : null;

                    NotebookFileResponse.UploaderInfo uploaderInfo = uploader != null
                            ? new NotebookFileResponse.UploaderInfo(
                                    uploader.getId(),
                                    uploader.getFullName(),
                                    uploader.getEmail(),
                                    urlNormalizer.normalizeToFull(uploader.getAvatarUrl()))
                            : null;

                    return new NotebookFileResponse(
                            file.getId(),
                            file.getOriginalFilename(),
                            file.getMimeType(),
                            file.getFileSize(),
                            normalizedStorageUrl,
                            file.getStatus(),
                            file.getPagesCount(),
                            file.getOcrDone(),
                            file.getEmbeddingDone(),
                            file.getChunkSize(),
                            file.getChunkOverlap(),
                            chunksCount,
                            uploaderInfo,
                            notebookInfo,
                            file.getCreatedAt(),
                            file.getUpdatedAt());
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotebookFileResponse> getFiles(
            UUID notebookId,
            String status,
            String mimeType,
            Boolean ocrDone,
            Boolean embeddingDone,
            UUID uploadedBy,
            String search,
            String sortBy,
            int page,
            int size) {

        if (page < 0) {
            throw new BadRequestException("Page phải >= 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size phải từ 1 đến 100");
        }

        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        var result = notebookFileRepository.findByNotebookIdWithFilters(
                notebookId, status, mimeType, ocrDone, embeddingDone, uploadedBy, search, pageable);

        List<NotebookFile> files = result.getContent();

        List<UUID> userIds = new ArrayList<>();
        for (NotebookFile file : files) {
            User uploader = file.getUploadedBy();
            if (uploader != null && uploader.getId() != null) {
                userIds.add(uploader.getId());
            }
        }

        List<User> users = userRepository.findAllById(userIds);
        java.util.Map<UUID, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<NotebookFileResponse> content = files.stream()
                .map(file -> {
                    long chunksCount = fileChunkRepository.countByFileId(file.getId());
                    User uploaderRef = file.getUploadedBy();
                    User uploader = uploaderRef != null && uploaderRef.getId() != null
                            ? userMap.get(uploaderRef.getId())
                            : null;

                    Notebook notebookRef = file.getNotebook();
                    String normalizedStorageUrl = urlNormalizer.normalizeToFull(file.getStorageUrl());
                    String normalizedThumbnailUrl = notebookRef != null
                            ? urlNormalizer.normalizeToFull(notebookRef.getThumbnailUrl())
                            : null;

                    NotebookFileResponse.NotebookInfo notebookInfo = notebookRef != null
                            ? new NotebookFileResponse.NotebookInfo(
                                    notebookRef.getId(),
                                    notebookRef.getTitle(),
                                    notebookRef.getDescription(),
                                    notebookRef.getType(),
                                    notebookRef.getVisibility(),
                                    normalizedThumbnailUrl)
                            : null;

                    NotebookFileResponse.UploaderInfo uploaderInfo = uploader != null
                            ? new NotebookFileResponse.UploaderInfo(
                                    uploader.getId(),
                                    uploader.getFullName(),
                                    uploader.getEmail(),
                                    urlNormalizer.normalizeToFull(uploader.getAvatarUrl()))
                            : null;

                    return new NotebookFileResponse(
                            file.getId(),
                            file.getOriginalFilename(),
                            file.getMimeType(),
                            file.getFileSize(),
                            normalizedStorageUrl,
                            file.getStatus(),
                            file.getPagesCount(),
                            file.getOcrDone(),
                            file.getEmbeddingDone(),
                            file.getChunkSize(),
                            file.getChunkOverlap(),
                            chunksCount,
                            uploaderInfo,
                            notebookInfo,
                            file.getCreatedAt(),
                            file.getUpdatedAt());
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private Sort getSort(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "created_at");
        }

        String lower = sortBy.toLowerCase().trim();

        if (lower.startsWith("createdat")) {
            return lower.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "created_at")
                    : Sort.by(Sort.Direction.DESC, "created_at");
        } else if (lower.startsWith("updatedat")) {
            return lower.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "updated_at")
                    : Sort.by(Sort.Direction.DESC, "updated_at");
        } else if (lower.startsWith("originalfilename") || lower.startsWith("filename")) {
            return lower.endsWith("desc")
                    ? Sort.by(Sort.Direction.DESC, "original_filename")
                    : Sort.by(Sort.Direction.ASC, "original_filename");
        }

        return Sort.by(Sort.Direction.DESC, "created_at");
    }

    @Transactional(readOnly = true)
    public List<ContributorInfo> getContributors(UUID notebookId, String search) {
        var results = notebookFileRepository.findContributorsByNotebookId(notebookId, search, 10);

        return results.stream()
                .map(row -> {
                    UUID userId = (UUID) row[0];
                    String fullName = (String) row[1];
                    String email = (String) row[2];
                    String avatarUrl = (String) row[3];
                    Long filesCount = ((Number) row[4]).longValue();

                    return new ContributorInfo(userId, fullName, email, avatarUrl, filesCount);
                })
                .collect(Collectors.toList());
    }


    public long getChunksCount(UUID fileId) {
        return fileChunkRepository.countByFileId(fileId);
    }

    @Transactional(readOnly = true)
    public com.example.springboot_api.dto.admin.notebook.NotebookFileDetailResponse getFileDetail(UUID notebookId,
            UUID fileId) {

        // Validate input parameters
        if (notebookId == null) {
            throw new BadRequestException("notebookId không được để trống");
        }
        if (fileId == null) {
            throw new BadRequestException("fileId không được để trống");
        }

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại với ID: " + fileId));

        // Check if notebook exists
        if (file.getNotebook() == null) {
            throw new NotFoundException("Notebook của file không tồn tại");
        }

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        long totalTextChunks = fileChunkRepository.countByFileId(fileId);

        // Count generated content using collection sizes (with null safety)
        int videoCount = file.getVideoAssetFiles() != null ? file.getVideoAssetFiles().size() : 0;
        int podcastCount = file.getTtsFiles() != null ? file.getTtsFiles().size() : 0;
        int flashcardCount = file.getFlashcardFiles() != null ? file.getFlashcardFiles().size() : 0;
        int quizCount = file.getQuizFiles() != null ? file.getQuizFiles().size() : 0;

        java.util.Map<String, Integer> generatedContentCounts = java.util.Map.of(
                "video", videoCount,
                "podcast", podcastCount,
                "flashcard", flashcardCount,
                "quiz", quizCount);

        NotebookFileResponse fileInfo = toResponse(file, totalTextChunks);

        return new com.example.springboot_api.dto.admin.notebook.NotebookFileDetailResponse(
                fileInfo,
                totalTextChunks,
                generatedContentCounts);
    }

}