package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.community.AvailableGroupResponse;
import com.example.springboot_api.dto.user.community.JoinedGroupResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Community/Group entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class CommunityMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert Notebook entity sang AvailableGroupResponse DTO.
     * 
     * @param notebook    Notebook entity
     * @param memberCount Số lượng thành viên đã approved
     */
    public AvailableGroupResponse toAvailableGroupResponse(Notebook notebook, Long memberCount) {
        if (notebook == null) {
            return null;
        }

        String thumbnailUrl = urlNormalizer.normalizeToFull(notebook.getThumbnailUrl());

        return new AvailableGroupResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getVisibility(),
                thumbnailUrl,
                memberCount != null ? memberCount : 0L,
                notebook.getCreatedAt());
    }

    /**
     * Convert NotebookMember entity sang JoinedGroupResponse DTO.
     * 
     * @param member      NotebookMember entity
     * @param memberCount Số lượng thành viên đã approved
     */
    public JoinedGroupResponse toJoinedGroupResponse(NotebookMember member, Long memberCount) {
        if (member == null) {
            return null;
        }

        Notebook notebook = member.getNotebook();
        String thumbnailUrl = urlNormalizer.normalizeToFull(notebook.getThumbnailUrl());

        return new JoinedGroupResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getVisibility(),
                thumbnailUrl,
                memberCount != null ? memberCount : 0L,
                member.getStatus(),
                member.getRole(),
                member.getJoinedAt(),
                notebook.getCreatedAt());
    }
}
