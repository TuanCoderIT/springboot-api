package com.example.springboot_api.services.lecturer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.lecturer.chapter.ChapterResponse;
import com.example.springboot_api.dto.lecturer.chapter.CreateChapterRequest;
import com.example.springboot_api.dto.lecturer.chapter.ReorderChapterRequest;
import com.example.springboot_api.dto.lecturer.chapter.UpdateChapterRequest;
import com.example.springboot_api.mappers.ChapterMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookChapter;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.lecturer.NotebookChapterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotebookChapterService {

    private final NotebookChapterRepository chapterRepo;
    private final NotebookRepository notebookRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final ChapterMapper chapterMapper;
    private final CurrentUserProvider userProvider;

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByNotebookId(UUID notebookId) {
        validateNotebookAccess(notebookId);
        List<NotebookChapter> chapters = chapterRepo.findByNotebookIdOrderBySortOrderAsc(notebookId);
        return chapters.stream()
                .map(chapterMapper::toChapterResponseWithItems)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChapterResponse createChapter(UUID notebookId, CreateChapterRequest req) {
        validateNotebookAccess(notebookId);

        Notebook notebook = notebookRepo.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        Integer maxOrder = chapterRepo.findMaxSortOrderByNotebookId(notebookId);
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        NotebookChapter chapter = NotebookChapter.builder()
                .notebook(notebook)
                .title(req.getTitle())
                .title(req.getTitle())
                // .description(req.getDescription()) // Description will be updated separately
                .sortOrder(nextOrder)
                .sortOrder(nextOrder)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        NotebookChapter saved = chapterRepo.save(chapter);
        return chapterMapper.toChapterResponse(saved);
    }

    @Transactional
    public ChapterResponse updateChapter(UUID chapterId, UpdateChapterRequest req) {
        NotebookChapter chapter = validateChapterAccess(chapterId);

        if (req.getTitle() != null) {
            chapter.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            chapter.setDescription(req.getDescription());
        }
        chapter.setUpdatedAt(OffsetDateTime.now());

        NotebookChapter saved = chapterRepo.save(chapter);
        return chapterMapper.toChapterResponse(saved);
    }

    @Transactional
    public void deleteChapter(UUID chapterId) {
        NotebookChapter chapter = validateChapterAccess(chapterId);
        chapterRepo.delete(chapter);
    }

    @Transactional
    public void reorderChapters(UUID notebookId, ReorderChapterRequest req) {
        validateNotebookAccess(notebookId);

        List<UUID> orderedIds = req.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }

        // Fetch all chapters to minimize queries and verify existence
        List<NotebookChapter> chapters = chapterRepo.findAllById(orderedIds);

        // Sort/Update based on the request list order
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int finalI = i;
            chapters.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .ifPresent(chapter -> {
                        // Ensure chapter belongs to the notebook (security check)
                        if (chapter.getNotebook().getId().equals(notebookId)) {
                            chapter.setSortOrder(finalI);
                            chapter.setUpdatedAt(OffsetDateTime.now());
                        }
                    });
        }

        chapterRepo.saveAll(chapters);
    }

    // --- Helper Methods using TeachingAssignment for validation ---

    private void validateNotebookAccess(UUID notebookId) {
        UUID lecturerId = userProvider.getCurrentUserId();

        // Find assignment by notebookId
        TeachingAssignment assignment = assignmentRepo.findByNotebookId(notebookId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công cho Notebook này"));

        if (!assignment.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập Notebook này");
        }
    }

    private NotebookChapter validateChapterAccess(UUID chapterId) {
        NotebookChapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chương không tồn tại"));

        validateNotebookAccess(chapter.getNotebook().getId());
        return chapter;
    }
}
