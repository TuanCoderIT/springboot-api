package com.example.springboot_api.services.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;
import com.example.springboot_api.mappers.NotebookFileMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserNotebookFileService {

    private final FileStorageService fileStorageService;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final FileProcessingTaskService fileProcessingTaskService;
    private final NotebookFileMapper notebookFileMapper;
    private final com.example.springboot_api.repositories.shared.FileChunkRepository fileChunkRepository;

    public List<NotebookFile> uploadFiles(
            UUID userId,
            UUID notebookId,
            List<MultipartFile> files) throws IOException {

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        // Xác định status ban đầu cho file
        // - Notebook thường: approved
        // - Community + owner/admin: approved (không cần chờ duyệt)
        // - Community + member thường: pending (chờ admin duyệt)
        String initStatus;
        if (isCommunity) {
            String memberRole = memberOpt.get().getRole();
            boolean isAdminOrOwner = "owner".equals(memberRole) || "admin".equals(memberRole);
            initStatus = isAdminOrOwner ? "approved" : "pending";
        } else {
            initStatus = "approved";
        }

        int defaultChunkSize = 3000;
        int defaultChunkOverlap = 250;

        List<NotebookFile> saved = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            String normalizedMimeType = getValidatedAndNormalizedMimeType(file);

            String storageUrl = fileStorageService.storeFile(file);

            NotebookFile newFile = NotebookFile.builder()
                    .notebook(notebook)
                    .uploadedBy(user)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(normalizedMimeType)
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .status(initStatus)
                    .ocrDone(false)
                    .embeddingDone(false)
                    .chunkSize(defaultChunkSize)
                    .chunkOverlap(defaultChunkOverlap)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

            NotebookFile savedFile = notebookFileRepository.save(newFile);
            saved.add(savedFile);

            if ("approved".equals(initStatus)) {
                fileProcessingTaskService.startAIProcessing(savedFile);
            }
        }

        return saved;
    }

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
     * Lấy danh sách file theo notebookId:
     * - File có status = 'done' (đã duyệt và xử lý xong)
     * - File của user hiện tại với các status khác (pending, failed, rejected,
     * processing)
     * - Có tìm kiếm theo tên file (optional)
     */
    public List<NotebookFile> getFilesForUser(UUID userId, UUID notebookId, String search) {
        // Kiểm tra user có quyền truy cập notebook không
        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        // Normalize search string
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        return notebookFileRepository.findFilesForUserByNotebookId(notebookId, userId, searchTerm);
    }

    /**
     * Lấy thông tin notebook file theo fileId
     */
    @Transactional(readOnly = true)
    public NotebookFileResponse getFileById(UUID userId, UUID fileId) {
        // Lấy file theo fileId
        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        return toResponse(file);
    }

    @Transactional(readOnly = true)
    public com.example.springboot_api.dto.user.notebook.UserNotebookFileDetailResponse getFileDetail(UUID userId,
            UUID notebookId, UUID fileId) {
        // Kiểm tra user có quyền truy cập notebook không
        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        // Get full content
        // List<com.example.springboot_api.models.FileChunk> chunks =
        // fileChunkRepository.findByFileId(fileId);
        // String fullContent = chunks.stream()
        // .map(com.example.springboot_api.models.FileChunk::getContent)
        // .collect(java.util.stream.Collectors.joining("\n"));
        String fullContent = "";

        // Count generated content
        // TODO: Cập nhật sau khi cấu trúc model mới hoàn thiện
        // Hiện tại các generated content được quản lý qua NotebookAiSet
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("video", 0L);
        counts.put("podcast", 0L);
        counts.put("flashcard", 0L);
        counts.put("quiz", 0L);

        // Contributor
        NotebookFileResponse.UploaderInfo contributor = null;
        if (file.getUploadedBy() != null) {
            // Use mapper to get normalized info
            NotebookFileResponse tempResponse = notebookFileMapper.toNotebookFileResponse(file);
            contributor = tempResponse.uploadedBy();
        }

        return new com.example.springboot_api.dto.user.notebook.UserNotebookFileDetailResponse(
                toResponse(file),
                fullContent,
                counts,
                contributor);
    }

    @Transactional(readOnly = true)
    public List<com.example.springboot_api.dto.user.notebook.FileChunkResponse> getFileChunks(
            UUID userId,
            UUID notebookId,
            UUID fileId) {
        // Kiểm tra user có quyền truy cập notebook không
        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        // Lấy chỉ các thông tin cần thiết từ repository (id, chunkIndex, content)
        List<Object[]> chunkDataList = fileChunkRepository.findChunkDataByFileId(fileId);

        // Map sang DTO
        return chunkDataList.stream()
                .map(data -> new com.example.springboot_api.dto.user.notebook.FileChunkResponse(
                        (UUID) data[0], // id
                        (Integer) data[1], // chunkIndex
                        (String) data[2])) // content
                .toList();
    }

    @Transactional
    public void deleteFile(UUID userId, UUID notebookId, UUID fileId) {
        // Kiểm tra user có quyền truy cập notebook không
        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        NotebookFile file = notebookFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File không tồn tại"));

        if (!file.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("File không thuộc notebook này");
        }

        // Kiểm tra file có phải của chính user này không
        if (file.getUploadedBy() == null || !file.getUploadedBy().getId().equals(userId)) {
            throw new BadRequestException("Bạn chỉ có thể xóa file của chính mình");
        }

        // Xóa file chunks
        fileChunkRepository.deleteByFileId(fileId);

        // Xóa file từ storage
        fileStorageService.deleteFile(file.getStorageUrl());

        // Xóa file record
        notebookFileRepository.delete(file);
    }

    public NotebookFileResponse toResponse(NotebookFile file) {
        return notebookFileMapper.toNotebookFileResponse(file);
    }

}
