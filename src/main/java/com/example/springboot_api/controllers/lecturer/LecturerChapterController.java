package com.example.springboot_api.controllers.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.lecturer.chapter.ChapterResponse;
import com.example.springboot_api.dto.lecturer.chapter.CreateChapterRequest;
import com.example.springboot_api.dto.lecturer.chapter.ReorderChapterRequest;
import com.example.springboot_api.dto.lecturer.chapter.UpdateChapterRequest;
import com.example.springboot_api.services.lecturer.NotebookChapterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Tag(name = "Lecturer Chapters", description = "Quản lý chương trong Notebook")
public class LecturerChapterController {

    private final NotebookChapterService chapterService;

    @GetMapping("/notebooks/{notebookId}/chapters")
    @Operation(summary = "Lấy danh sách chương của notebook")
    public ResponseEntity<List<ChapterResponse>> getChapters(@PathVariable UUID notebookId) {
        return ResponseEntity.ok(chapterService.getChaptersByNotebookId(notebookId));
    }

    @PostMapping("/notebooks/{notebookId}/chapters")
    @Operation(summary = "Tạo chương mới")
    public ResponseEntity<ChapterResponse> createChapter(
            @PathVariable UUID notebookId,
            @Valid @RequestBody CreateChapterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chapterService.createChapter(notebookId, req));
    }

    @PutMapping("/chapters/{chapterId}")
    @Operation(summary = "Cập nhật chương")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable UUID chapterId,
            @RequestBody UpdateChapterRequest req) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, req));
    }

    @DeleteMapping("/chapters/{chapterId}")
    @Operation(summary = "Xóa chương")
    public ResponseEntity<Void> deleteChapter(@PathVariable UUID chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notebooks/{notebookId}/chapters/reorder")
    @Operation(summary = "Sắp xếp lại thứ tự chương (phục vụ dndkit)")
    public ResponseEntity<Void> reorderChapters(
            @PathVariable UUID notebookId,
            @Valid @RequestBody ReorderChapterRequest req) {
        chapterService.reorderChapters(notebookId, req);
        return ResponseEntity.ok().build();
    }
}
