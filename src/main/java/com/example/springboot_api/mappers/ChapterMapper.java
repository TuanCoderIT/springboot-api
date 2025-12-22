package com.example.springboot_api.mappers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.lecturer.chapter.ChapterItemResponse;
import com.example.springboot_api.dto.lecturer.chapter.ChapterResponse;
import com.example.springboot_api.models.ChapterItem;
import com.example.springboot_api.models.NotebookChapter;

@Component
public class ChapterMapper {

    public ChapterResponse toChapterResponse(NotebookChapter chapter) {
        if (chapter == null) {
            return null;
        }

        return ChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .sortOrder(chapter.getSortOrder())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .items(null) // Không load items mặc định
                .build();
    }

    /**
     * Chuyển đổi chapter kèm theo items (dùng cho getChaptersByNotebookId)
     */
    public ChapterResponse toChapterResponseWithItems(NotebookChapter chapter) {
        if (chapter == null) {
            return null;
        }

        List<ChapterItemResponse> items = null;
        if (chapter.getChapterItems() != null && !chapter.getChapterItems().isEmpty()) {
            items = chapter.getChapterItems().stream()
                    .sorted(Comparator.comparingInt(ChapterItem::getSortOrder))
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }

        return ChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .sortOrder(chapter.getSortOrder())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .items(items)
                .build();
    }

    private ChapterItemResponse toItemResponse(ChapterItem item) {
        return ChapterItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .refId(item.getRefId())
                .title(item.getTitle())
                .sortOrder(item.getSortOrder())
                .metadata(item.getMetadata())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
