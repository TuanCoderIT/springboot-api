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

    @Transactional
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

        String initStatus = isCommunity ? "pending" : "approved";

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
}
