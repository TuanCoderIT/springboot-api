package com.example.springboot_api.mappers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.ai.MindmapResponse;
import com.example.springboot_api.models.NotebookMindmap;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Mindmap entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class MindmapMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert NotebookMindmap entity sang MindmapResponse DTO.
     * Xử lý cả dữ liệu cũ có wrapper "root" và dữ liệu mới không có.
     */
    public MindmapResponse toMindmapResponse(NotebookMindmap mindmap) {
        if (mindmap == null) {
            return null;
        }

        String createdByName = null;
        String createdByAvatar = null;
        java.util.UUID createdById = null;

        if (mindmap.getCreatedBy() != null) {
            createdById = mindmap.getCreatedBy().getId();
            createdByName = mindmap.getCreatedBy().getFullName();
            createdByAvatar = urlNormalizer.normalizeToFull(mindmap.getCreatedBy().getAvatarUrl());
        }

        // Flatten mindmap: nếu có wrapper "root" thì lấy nội dung bên trong
        Map<String, Object> mindmapData = flattenMindmapRoot(mindmap.getMindmap());

        return MindmapResponse.builder()
                .id(mindmap.getId())
                .notebookId(mindmap.getNotebook() != null ? mindmap.getNotebook().getId() : null)
                .title(mindmap.getTitle())
                .mindmap(mindmapData)
                .layout(mindmap.getLayout())
                .aiSetId(mindmap.getSourceAiSet() != null ? mindmap.getSourceAiSet().getId() : null)
                .createdById(createdById)
                .createdByName(createdByName)
                .createdByAvatar(createdByAvatar)
                .createdAt(mindmap.getCreatedAt())
                .updatedAt(mindmap.getUpdatedAt())
                .build();
    }

    /**
     * Flatten mindmap data: nếu có wrapper "root" thì trả về node root trực tiếp.
     * Hỗ trợ backward compatibility với dữ liệu cũ.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenMindmapRoot(Map<String, Object> mindmapData) {
        if (mindmapData == null) {
            return null;
        }

        // Kiểm tra xem có wrapper "root" không
        if (mindmapData.containsKey("root") && mindmapData.get("root") instanceof Map) {
            // Dữ liệu cũ có wrapper "root" -> flatten
            return (Map<String, Object>) mindmapData.get("root");
        }

        // Dữ liệu mới đã đúng format (có id, title, summary, children trực tiếp)
        return mindmapData;
    }
}
