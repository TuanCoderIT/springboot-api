package com.example.springboot_api.services.lecturer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.lecturer.notebook.LecturerFileDetailResponse;
import com.example.springboot_api.dto.lecturer.notebook.LecturerNotebookFileResponse;
import com.example.springboot_api.dto.lecturer.notebook.LecturerNotebookSummary;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LecturerNotebookFileService {

    private final NotebookFileRepository notebookFileRepository;
    private final FileChunkRepository fileChunkRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileProcessingTaskService fileProcessingTaskService;

    /**
     * Upload files vào notebook
     */
    // @Transactional <-- Bỏ Transactional để file được lưu ngay lập tức
    public List<LecturerNotebookFileResponse> uploadFiles(UUID lecturerId, UUID notebookId,
            FileUploadRequest request, List<MultipartFile> files) throws IOException {
        // Kiểm tra notebook tồn tại
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        // Kiểm tra lecturer tồn tại
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new NotFoundException("Lecturer không tồn tại"));

        // TODO: Kiểm tra lecturer có quyền upload vào notebook này không
        // Hiện tại giả định lecturer có quyền upload vào tất cả notebooks

        // Sử dụng cấu hình từ request
        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 3000;
        int chunkOverlap = request.getChunkOverlap() != null ? request.getChunkOverlap() : 250;

        List<NotebookFile> uploadedFiles = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            // Validate và normalize MIME type
            String normalizedMimeType = getValidatedAndNormalizedMimeType(file);

            // Lưu file vào storage (IO operation - tốt nhất là không nằm trong Transaction)
            String storageUrl = fileStorageService.storeFile(file);

            // Tạo NotebookFile entity
            NotebookFile newFile = NotebookFile.builder()
                    .notebook(notebook)
                    .uploadedBy(lecturer)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(normalizedMimeType)
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .status("approved") // Lecturer upload thì auto approve
                    .ocrDone(false)
                    .embeddingDone(false)
                    .chunkSize(chunkSize)
                    .chunkOverlap(chunkOverlap)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

            // Lưu vào DB (commit ngay lập tức vì không có @Transactional)
            NotebookFile savedFile = notebookFileRepository.save(newFile);
            uploadedFiles.add(savedFile);

            // Bắt đầu AI processing ngay lập tức (Async thread sẽ thấy record ngay)
            fileProcessingTaskService.startAIProcessing(savedFile);
        }

        return uploadedFiles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xóa file khỏi notebook
     */
    @Transactional
    public void deleteFile(UUID lecturerId, UUID notebookId, UUID fileId) {
        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        fileChunkRepository.deleteByFileId(fileId);
        fileStorageService.deleteFile(file.getStorageUrl());
        notebookFileRepository.delete(file);
    }

    /**
     * Validate và normalize MIME type
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

    /**
     * Lấy files từ một notebook cụ thể
     * Chỉ lấy files có status = 'done' để đảm bảo có thể dùng cho tạo câu hỏi
     */
    @Transactional(readOnly = true)
    public List<LecturerNotebookFileResponse> getFilesByNotebook(UUID lecturerId, UUID notebookId, String search) {
        // TODO: Kiểm tra lecturer có quyền truy cập notebook này không
        // Hiện tại giả định lecturer có quyền truy cập tất cả notebooks

        List<NotebookFile> files = notebookFileRepository.findByNotebookIdAndStatusInAndSearch(
                notebookId,
                List.of("done", "approved"),
                search,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();

        return files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả files mà lecturer có thể truy cập
     */
    @Transactional(readOnly = true)
    public List<LecturerNotebookFileResponse> getAllAccessibleFiles(UUID lecturerId, String search,
            UUID notebookId, int limit) {
        // TODO: Implement logic để lấy files từ tất cả notebooks mà lecturer có quyền
        // Hiện tại sử dụng logic đơn giản

        if (notebookId != null) {
            return getFilesByNotebook(lecturerId, notebookId, search);
        }

        // Tạm thời return empty list - cần implement logic phức tạp hơn
        return List.of();
    }

    /**
     * Lấy danh sách notebooks mà lecturer có thể truy cập
     */
    @Transactional(readOnly = true)
    public List<LecturerNotebookSummary> getAccessibleNotebooks(UUID lecturerId) {
        List<LecturerNotebookSummary> result = new java.util.ArrayList<>();

        log.info("Getting accessible notebooks for lecturer: {}", lecturerId);

        // 1. Lấy class notebooks mà lecturer dạy
        List<Notebook> classNotebooks = notebookRepository.findClassNotebooksByLecturerId(lecturerId);
        log.info("Found {} class notebooks for lecturer", classNotebooks.size());
        for (Notebook notebook : classNotebooks) {
            result.add(toNotebookSummary(notebook));
        }

        // 2. Lấy personal notebooks của lecturer (nếu có)
        org.springframework.data.domain.Page<Notebook> personalNotebooks = notebookRepository
                .findPersonalNotebooksByUserId(lecturerId, null,
                        org.springframework.data.domain.PageRequest.of(0, 100));
        log.info("Found {} personal notebooks for lecturer", personalNotebooks.getTotalElements());
        for (Notebook notebook : personalNotebooks.getContent()) {
            result.add(toNotebookSummary(notebook));
        }

        log.info("Returning {} notebooks for lecturer", result.size());
        return result;
    }

    /**
     * Lấy chi tiết file
     */
    @Transactional(readOnly = true)
    public LecturerFileDetailResponse getFileDetail(UUID lecturerId, UUID notebookId, UUID fileId) {
        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        // TODO: Kiểm tra quyền truy cập

        return toDetailResponse(file);
    }

    /**
     * Convert NotebookFile to LecturerNotebookFileResponse
     */
    private LecturerNotebookFileResponse toResponse(NotebookFile file) {
        // Lấy content preview
        String contentPreview = getContentPreview(file.getId());

        // Lấy chunks count
        Long chunksCount = fileChunkRepository.countByFileId(file.getId());

        LecturerNotebookFileResponse.UploaderInfo uploaderInfo = null;
        if (file.getUploadedBy() != null) {
            uploaderInfo = LecturerNotebookFileResponse.UploaderInfo.builder()
                    .id(file.getUploadedBy().getId())
                    .fullName(file.getUploadedBy().getFullName())
                    .email(file.getUploadedBy().getEmail())
                    .build();
        }

        return LecturerNotebookFileResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .ocrDone(file.getOcrDone())
                .embeddingDone(file.getEmbeddingDone())
                .createdAt(file.getCreatedAt())
                .notebookId(file.getNotebook().getId())
                .notebookTitle(file.getNotebook().getTitle())
                .notebookType(file.getNotebook().getType())
                .uploadedBy(uploaderInfo)
                .chunksCount(chunksCount)
                .contentPreview(contentPreview)
                .build();
    }

    /**
     * Convert NotebookFile to LecturerFileDetailResponse
     */
    private LecturerFileDetailResponse toDetailResponse(NotebookFile file) {
        // Lấy content summary và first chunk
        String contentSummary = getContentSummary(file.getId());
        String firstChunkContent = getFirstChunkContent(file.getId());
        Long totalChunks = fileChunkRepository.countByFileId(file.getId());

        LecturerNotebookFileResponse.UploaderInfo uploaderInfo = null;
        if (file.getUploadedBy() != null) {
            uploaderInfo = LecturerNotebookFileResponse.UploaderInfo.builder()
                    .id(file.getUploadedBy().getId())
                    .fullName(file.getUploadedBy().getFullName())
                    .email(file.getUploadedBy().getEmail())
                    .build();
        }

        return LecturerFileDetailResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .ocrDone(file.getOcrDone())
                .embeddingDone(file.getEmbeddingDone())
                .createdAt(file.getCreatedAt())
                .notebookId(file.getNotebook().getId())
                .notebookTitle(file.getNotebook().getTitle())
                .uploadedBy(uploaderInfo)
                .contentSummary(contentSummary)
                .totalChunks(totalChunks)
                .firstChunkContent(firstChunkContent)
                .chunkSize(file.getChunkSize())
                .chunkOverlap(file.getChunkOverlap())
                .build();
    }

    /**
     * Lấy preview nội dung file (200 ký tự đầu)
     */
    private String getContentPreview(UUID fileId) {
        try {
            List<Object[]> chunks = fileChunkRepository.findChunkDataByFileId(fileId);
            if (!chunks.isEmpty()) {
                String content = (String) chunks.get(0)[2]; // content field
                if (content != null && content.length() > 200) {
                    return content.substring(0, 200) + "...";
                }
                return content;
            }
        } catch (Exception e) {
            log.warn("Error getting content preview for file {}: {}", fileId, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy tóm tắt nội dung file
     */
    private String getContentSummary(UUID fileId) {
        try {
            List<Object[]> chunks = fileChunkRepository.findChunkDataByFileId(fileId);
            if (!chunks.isEmpty()) {
                // Lấy 3 chunks đầu tiên để tạo summary
                StringBuilder summary = new StringBuilder();
                int count = Math.min(3, chunks.size());
                for (int i = 0; i < count; i++) {
                    String content = (String) chunks.get(i)[2];
                    if (content != null) {
                        summary.append(content);
                        if (i < count - 1)
                            summary.append("\n\n");
                    }
                }
                String fullSummary = summary.toString();
                if (fullSummary.length() > 1000) {
                    return fullSummary.substring(0, 1000) + "...";
                }
                return fullSummary;
            }
        } catch (Exception e) {
            log.warn("Error getting content summary for file {}: {}", fileId, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy nội dung chunk đầu tiên
     */
    private String getFirstChunkContent(UUID fileId) {
        try {
            List<Object[]> chunks = fileChunkRepository.findChunkDataByFileId(fileId);
            if (!chunks.isEmpty()) {
                return (String) chunks.get(0)[2]; // content field
            }
        } catch (Exception e) {
            log.warn("Error getting first chunk content for file {}: {}", fileId, e.getMessage());
        }
        return null;
    }

    /**
     * Convert Notebook to LecturerNotebookSummary
     */
    private LecturerNotebookSummary toNotebookSummary(Notebook notebook) {
        // Đếm tổng số files và files ready
        Long totalFiles = notebookFileRepository.countByNotebookId(notebook.getId());
        Long readyFiles = notebookFileRepository.countByNotebookIdAndStatusIn(
                notebook.getId(),
                List.of("done", "approved"));

        LecturerNotebookSummary.LecturerNotebookSummaryBuilder builder = LecturerNotebookSummary.builder()
                .id(notebook.getId())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .type(notebook.getType())
                .totalFiles(totalFiles)
                .readyFiles(readyFiles);

        // Nếu là class notebook, thêm thông tin class
        if ("class".equals(notebook.getType())) {
            // TODO: Lấy thông tin class từ notebook metadata hoặc relationship
            // Hiện tại để null, có thể implement sau
            builder.classId(null)
                    .className(null)
                    .subjectCode(null)
                    .subjectName(null);
        }

        return builder.build();
    }
}