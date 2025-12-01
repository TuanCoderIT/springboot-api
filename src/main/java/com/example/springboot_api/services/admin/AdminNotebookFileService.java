package com.example.springboot_api.services.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotebookFileService {

    private final FileStorageService fileStorageService;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final FileProcessingTaskService fileProcessingTaskService;

    // ============================
    // ADMIN UPLOAD FILE
    // ============================
    @Transactional
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

            // ADMIN upload → xử lý AI ngay
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
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        }

        // Nếu không khớp với bất kỳ extension hợp lệ nào
        throw new BadRequestException("Chỉ hỗ trợ PDF / Ảnh (JPG, PNG, GIF) / DOCX.");
    }

    private void validateChunkParams(FileUploadRequest req) {
        if (req.getChunkSize() == null || req.getChunkSize() < 200 || req.getChunkSize() > 2000)
            throw new BadRequestException("ChunkSize không hợp lệ.");

        if (req.getChunkOverlap() == null || req.getChunkOverlap() < 0
                || req.getChunkOverlap() > req.getChunkSize() - 10)
            throw new BadRequestException("ChunkOverlap không hợp lệ.");
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

        file.setStatus("approved");
        file.setUpdatedAt(java.time.OffsetDateTime.now());
        NotebookFile saved = notebookFileRepository.save(file);

        // Kích hoạt AI khi duyệt
        fileProcessingTaskService.startAIProcessing(saved);

        return saved;
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

    // ============================
    // LẤY FILE CHỜ DUYỆT
    // ============================

    @Transactional(readOnly = true)
    public List<NotebookFile> getPendingFiles(UUID notebookId) {
        return notebookFileRepository.findByNotebookIdAndStatus(notebookId, "pending");
    }
}