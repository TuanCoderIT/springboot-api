package com.example.springboot_api.controllers.lecturer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.dto.lecturer.chapter.ChapterFileUploadRequest;
import com.example.springboot_api.dto.lecturer.chapter.ChapterItemResponse;
import com.example.springboot_api.dto.lecturer.chapter.ChapterYoutubeUploadRequest;
import com.example.springboot_api.dto.lecturer.chapter.MoveItemRequest;
import com.example.springboot_api.dto.lecturer.chapter.ReorderItemsRequest;
import com.example.springboot_api.services.lecturer.ChapterItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lecturer/chapters/{chapterId}/items")
@RequiredArgsConstructor
@Tag(name = "Lecturer Chapter Items", description = "Quản lý nội dung trong chương")
public class LecturerChapterItemController {

    private final ChapterItemService itemService;

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload files vào chương (itemType=FILE)")
    public ResponseEntity<List<ChapterItemResponse>> uploadFiles(
            @PathVariable UUID chapterId,
            @Valid @ModelAttribute ChapterFileUploadRequest req,
            @RequestPart("files") List<MultipartFile> files) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.uploadFilesToChapter(chapterId, req, files));
    }

    @PostMapping("/youtube")
    @Operation(summary = "Thêm video YouTube vào chương (itemType=VIDEO)", description = "Trích xuất phụ đề từ video YouTube và tạo RAG chunks cho AI")
    public ResponseEntity<ChapterItemResponse> addYoutubeVideo(
            @PathVariable UUID chapterId,
            @Valid @RequestBody ChapterYoutubeUploadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.addYoutubeVideoToChapter(chapterId, req));
    }

    @PatchMapping("/{itemId}/move")
    @Operation(summary = "Di chuyển item sang chapter khác hoặc đổi vị trí trong chapter")
    public ResponseEntity<ChapterItemResponse> moveItem(
            @PathVariable UUID chapterId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MoveItemRequest req) {
        return ResponseEntity.ok(itemService.moveItem(itemId, req.getTargetChapterId(), req.getTargetIndex()));
    }

    @PatchMapping("/reorder")
    @Operation(summary = "Sắp xếp lại thứ tự items trong chapter")
    public ResponseEntity<List<ChapterItemResponse>> reorderItems(
            @PathVariable UUID chapterId,
            @Valid @RequestBody ReorderItemsRequest req) {
        return ResponseEntity.ok(itemService.reorderItems(chapterId, req.getOrderedIds()));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Xóa item khỏi chapter")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID chapterId,
            @PathVariable UUID itemId) {
        itemService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
