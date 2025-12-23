package com.example.springboot_api.services.lecturer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.constants.ChapterItemType;
import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.lecturer.chapter.ChapterFileUploadRequest;
import com.example.springboot_api.dto.lecturer.chapter.ChapterItemResponse;
import com.example.springboot_api.dto.lecturer.chapter.ChapterYoutubeUploadRequest;
import com.example.springboot_api.models.ChapterItem;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookChapter;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.lecturer.ChapterItemRepository;
import com.example.springboot_api.repositories.lecturer.NotebookChapterRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.FileProcessingTaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChapterItemService {

    private final ChapterItemRepository itemRepo;
    private final NotebookChapterRepository chapterRepo;
    private final NotebookFileRepository fileRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;
    private final FileStorageService fileStorageService;
    private final FileProcessingTaskService fileProcessingService;
    private final CurrentUserProvider userProvider;

    // ============================
    // UPLOAD FILES TO CHAPTER
    // ============================
    // Không dùng @Transactional - mỗi save() auto-commit để async task tìm thấy
    // file
    public List<ChapterItemResponse> uploadFilesToChapter(
            UUID chapterId,
            ChapterFileUploadRequest req,
            List<MultipartFile> files) throws IOException {
        System.out.println("📤 Upload files to chapter: " + chapterId);
        NotebookChapter chapter = validateChapterAccess(chapterId);
        Notebook notebook = chapter.getNotebook();
        UUID lecturerId = userProvider.getCurrentUserId();
        User lecturer = userRepo.findById(lecturerId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        // Cố định chunk size và overlap - không cho giảng viên tùy chỉnh
        final int chunkSize = 2000;
        final int chunkOverlap = 200;

        Integer maxOrder = itemRepo.findMaxSortOrderByChapterId(chapterId);
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        List<ChapterItemResponse> results = new ArrayList<>();
        List<ChapterFileUploadRequest.FileMetadata> fileInfos = req.getFileInfos();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty())
                continue;

            // Lấy metadata riêng cho từng file (nếu có)
            ChapterFileUploadRequest.FileMetadata fileInfo = null;
            if (fileInfos != null && i < fileInfos.size()) {
                fileInfo = fileInfos.get(i);
            }

            String normalizedMimeType = getValidatedMimeType(file);
            String storageUrl = fileStorageService.storeFile(file);

            // 1. Create NotebookFile
            NotebookFile notebookFile = NotebookFile.builder()
                    .notebook(notebook)
                    .uploadedBy(lecturer)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(normalizedMimeType)
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .status("approved")
                    .ocrDone(false)
                    .embeddingDone(false)
                    .chunkSize(chunkSize)
                    .chunkOverlap(chunkOverlap)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            NotebookFile savedFile = fileRepo.save(notebookFile);

            // 2. Start AI Processing (OCR, Chunking, Embedding)
            System.out.println("📤 Calling startAIProcessing for file: " + savedFile.getId());
            fileProcessingService.startAIProcessing(savedFile);
            System.out.println("📤 Called startAIProcessing (async)");

            // 3. Create ChapterItem referencing the file
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("mimeType", normalizedMimeType);
            metadata.put("fileSize", file.getSize());
            metadata.put("storageUrl", storageUrl);
            metadata.put("originalFilename", file.getOriginalFilename());

            // Lưu description vào metadata nếu có
            if (fileInfo != null && fileInfo.getDescription() != null) {
                metadata.put("description", fileInfo.getDescription());
            }

            // Dùng title từ fileInfo nếu có, không thì dùng tên file gốc
            String displayTitle = (fileInfo != null && fileInfo.getTitle() != null && !fileInfo.getTitle().isBlank())
                    ? fileInfo.getTitle()
                    : file.getOriginalFilename();

            ChapterItem item = ChapterItem.builder()
                    .chapter(chapter)
                    .itemType(ChapterItemType.FILE)
                    .refId(savedFile.getId())
                    .title(displayTitle)
                    .sortOrder(nextOrder++)
                    .metadata(metadata)
                    .createdAt(OffsetDateTime.now())
                    .visibleInLesson(true)
                    .visibleInNotebook(true)
                    .build();
            ChapterItem savedItem = itemRepo.save(item);

            results.add(toResponse(savedItem));
        }

        return results;
    }

    // ============================
    // ADD YOUTUBE VIDEO TO CHAPTER
    // ============================
    /**
     * Thêm video YouTube vào chapter.
     * Tạo record ngay → async: trích xuất phụ đề + tạo chunks + embedding.
     * API trả response ngay lập tức, xử lý nặng chạy nền.
     */
    public ChapterItemResponse addYoutubeVideoToChapter(UUID chapterId, ChapterYoutubeUploadRequest req) {
        System.out.println("🎬 Adding YouTube video to chapter: " + chapterId);
        NotebookChapter chapter = validateChapterAccess(chapterId);
        Notebook notebook = chapter.getNotebook();
        UUID lecturerId = userProvider.getCurrentUserId();
        User lecturer = userRepo.findById(lecturerId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        String youtubeUrl = req.getYoutubeUrl();
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            throw new BadRequestException("URL video YouTube không được để trống");
        }

        // 1. Tạo NotebookFile để lưu thông tin video (ngay lập tức)
        NotebookFile videoFile = NotebookFile.builder()
                .notebook(notebook)
                .uploadedBy(lecturer)
                .originalFilename("youtube_" + extractVideoId(youtubeUrl) + ".txt")
                .mimeType("video/youtube")
                .fileSize(0L) // Sẽ update sau khi trích xuất subtitle
                .storageUrl(youtubeUrl)
                .status("approved")
                .ocrDone(true) // Không cần OCR với video
                .embeddingDone(false)
                .chunkSize(2000)
                .chunkOverlap(200)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        NotebookFile savedFile = fileRepo.save(videoFile);

        // 2. Gọi async để: trích xuất phụ đề + tạo chunks + embedding (không block
        // response)
        System.out.println("📤 Calling startYoutubeProcessing for video: " + savedFile.getId());
        fileProcessingService.startYoutubeProcessing(savedFile, youtubeUrl);
        System.out.println("📤 Called startYoutubeProcessing (async)");

        // 3. Tạo ChapterItem (ngay lập tức)
        Integer maxOrder = itemRepo.findMaxSortOrderByChapterId(chapterId);
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("youtubeUrl", youtubeUrl);
        metadata.put("videoId", extractVideoId(youtubeUrl));
        if (req.getDescription() != null) {
            metadata.put("description", req.getDescription());
        }

        String displayTitle = (req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle()
                : "Video YouTube: " + extractVideoId(youtubeUrl);

        ChapterItem item = ChapterItem.builder()
                .chapter(chapter)
                .itemType(ChapterItemType.VIDEO)
                .refId(savedFile.getId())
                .title(displayTitle)
                .sortOrder(nextOrder)
                .metadata(metadata)
                .createdAt(OffsetDateTime.now())
                .visibleInLesson(true)
                .visibleInNotebook(true)
                .build();
        ChapterItem savedItem = itemRepo.save(item);

        System.out.println("✅ YouTube video added successfully: " + savedItem.getId());
        return toResponse(savedItem);
    }

    /**
     * Trích xuất video ID từ YouTube URL.
     */
    private String extractVideoId(String url) {
        if (url.contains("youtu.be/")) {
            String id = url.substring(url.indexOf("youtu.be/") + 9);
            int queryIndex = id.indexOf("?");
            return queryIndex > 0 ? id.substring(0, queryIndex) : id;
        }
        if (url.contains("v=")) {
            String id = url.substring(url.indexOf("v=") + 2);
            int ampIndex = id.indexOf("&");
            return ampIndex > 0 ? id.substring(0, ampIndex) : id;
        }
        return url;
    }

    // ============================
    // HELPER METHODS
    // ============================
    private NotebookChapter validateChapterAccess(UUID chapterId) {
        NotebookChapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chương không tồn tại"));

        UUID lecturerId = userProvider.getCurrentUserId();
        UUID notebookId = chapter.getNotebook().getId();

        TeachingAssignment assignment = assignmentRepo.findByNotebookId(notebookId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công cho Notebook này"));

        if (!assignment.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập Notebook này");
        }

        return chapter;
    }

    private String getValidatedMimeType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new BadRequestException("Tên file không hợp lệ.");
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".pdf"))
            return "application/pdf";
        if (lower.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc"))
            return "application/msword";
        if (lower.endsWith(".pptx"))
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".ppt"))
            return "application/vnd.ms-powerpoint";

        throw new BadRequestException(
                "Chỉ hỗ trợ file PDF, Word (.doc, .docx), PowerPoint (.ppt, .pptx). File không hợp lệ: " + filename);
    }

    private ChapterItemResponse toResponse(ChapterItem item) {
        return ChapterItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .refId(item.getRefId())
                .title(item.getTitle())
                .sortOrder(item.getSortOrder())
                .metadata(item.getMetadata())
                .createdAt(item.getCreatedAt())
                .visibleInLesson(item.getVisibleInLesson())
                .visibleInNotebook(item.getVisibleInNotebook())
                .build();
    }

    // ============================
    // MOVE ITEM TO ANOTHER CHAPTER
    // ============================
    /**
     * Di chuyển item sang chapter khác (cùng notebook).
     * 
     * @param itemId          ID của item cần di chuyển
     * @param targetChapterId ID chapter đích
     * @param targetIndex     Vị trí trong chapter đích (0-based), null = cuối danh
     *                        sách
     */
    public ChapterItemResponse moveItem(UUID itemId, UUID targetChapterId, Integer targetIndex) {
        ChapterItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item không tồn tại"));

        NotebookChapter sourceChapter = item.getChapter();
        NotebookChapter targetChapter = validateChapterAccess(targetChapterId);

        // Kiểm tra cùng notebook
        if (!sourceChapter.getNotebook().getId().equals(targetChapter.getNotebook().getId())) {
            throw new BadRequestException("Không thể di chuyển item sang notebook khác");
        }

        // Kiểm tra quyền truy cập source chapter
        validateChapterAccess(sourceChapter.getId());

        // Nếu di chuyển trong cùng chapter (reorder)
        boolean sameChapter = sourceChapter.getId().equals(targetChapterId);

        if (sameChapter) {
            // Reorder trong cùng chapter
            reorderItemInSameChapter(item, targetIndex);
        } else {
            // Di chuyển sang chapter khác
            moveItemToOtherChapter(item, targetChapter, targetIndex);
        }

        ChapterItem updated = itemRepo.findById(itemId).orElseThrow();
        return toResponse(updated);
    }

    private void reorderItemInSameChapter(ChapterItem item, Integer targetIndex) {
        UUID chapterId = item.getChapter().getId();
        List<ChapterItem> items = itemRepo.findByChapterIdOrderBySortOrderAsc(chapterId);

        int oldIndex = item.getSortOrder();
        int newIndex = (targetIndex == null) ? items.size() - 1 : Math.min(targetIndex, items.size() - 1);

        if (oldIndex == newIndex)
            return;

        // Di chuyển item
        items.remove(item);
        items.add(newIndex, item);

        // Cập nhật sortOrder
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortOrder(i);
        }

        itemRepo.saveAll(items);
    }

    private void moveItemToOtherChapter(ChapterItem item, NotebookChapter targetChapter, Integer targetIndex) {
        UUID sourceChapterId = item.getChapter().getId();
        UUID targetChapterId = targetChapter.getId();

        // 1. Xóa khỏi source chapter và reorder
        List<ChapterItem> sourceItems = itemRepo.findByChapterIdOrderBySortOrderAsc(sourceChapterId);
        sourceItems.remove(item);
        for (int i = 0; i < sourceItems.size(); i++) {
            sourceItems.get(i).setSortOrder(i);
        }
        itemRepo.saveAll(sourceItems);

        // 2. Thêm vào target chapter
        List<ChapterItem> targetItems = itemRepo.findByChapterIdOrderBySortOrderAsc(targetChapterId);
        int newIndex = (targetIndex == null) ? targetItems.size() : Math.min(targetIndex, targetItems.size());

        item.setChapter(targetChapter);
        targetItems.add(newIndex, item);

        // Cập nhật sortOrder
        for (int i = 0; i < targetItems.size(); i++) {
            targetItems.get(i).setSortOrder(i);
        }

        itemRepo.saveAll(targetItems);
    }

    // ============================
    // REORDER ITEMS IN CHAPTER
    // ============================
    /**
     * Sắp xếp lại thứ tự items trong chapter.
     * 
     * @param chapterId      ID của chapter
     * @param orderedItemIds Danh sách ID items theo thứ tự mới
     */
    public List<ChapterItemResponse> reorderItems(UUID chapterId, List<UUID> orderedItemIds) {
        validateChapterAccess(chapterId);

        if (orderedItemIds == null || orderedItemIds.isEmpty()) {
            throw new BadRequestException("Danh sách item IDs không được rỗng");
        }

        List<ChapterItem> items = itemRepo.findByChapterIdOrderBySortOrderAsc(chapterId);

        // Validate tất cả IDs đều thuộc chapter này
        Map<UUID, ChapterItem> itemMap = new HashMap<>();
        for (ChapterItem item : items) {
            itemMap.put(item.getId(), item);
        }

        for (UUID id : orderedItemIds) {
            if (!itemMap.containsKey(id)) {
                throw new BadRequestException("Item " + id + " không thuộc chapter này");
            }
        }

        // Cập nhật sortOrder theo thứ tự mới
        for (int i = 0; i < orderedItemIds.size(); i++) {
            ChapterItem item = itemMap.get(orderedItemIds.get(i));
            item.setSortOrder(i);
        }

        itemRepo.saveAll(items);

        // Trả về danh sách đã sắp xếp
        List<ChapterItem> sorted = itemRepo.findByChapterIdOrderBySortOrderAsc(chapterId);
        List<ChapterItemResponse> results = new ArrayList<>();
        for (ChapterItem item : sorted) {
            results.add(toResponse(item));
        }

        return results;
    }

    // ============================
    // DELETE ITEM
    // ============================
    public void deleteItem(UUID itemId) {
        ChapterItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item không tồn tại"));

        validateChapterAccess(item.getChapter().getId());
        UUID chapterId = item.getChapter().getId();

        // Nếu item là FILE -> xóa NotebookFile (chunks tự xóa theo CASCADE)
        if (ChapterItemType.FILE.equals(item.getItemType()) && item.getRefId() != null) {
            NotebookFile notebookFile = fileRepo.findById(item.getRefId()).orElse(null);
            if (notebookFile != null) {
                fileStorageService.deleteFile(notebookFile.getStorageUrl());
                fileRepo.delete(notebookFile); // Chunks auto-delete via CASCADE
            }
        }

        // Xóa chapter item
        itemRepo.delete(item);

        // Reorder remaining items
        List<ChapterItem> remaining = itemRepo.findByChapterIdOrderBySortOrderAsc(chapterId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i);
        }
        itemRepo.saveAll(remaining);
    }
}
