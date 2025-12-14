package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.notebook.NotebookMemberItem;
import com.example.springboot_api.dto.user.notebook.PersonalNotebookResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Notebook entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class NotebookMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert Notebook entity sang PersonalNotebookResponse DTO.
     * 
     * @param notebook  Notebook entity
     * @param fileCount Số lượng files trong notebook
     */
    public PersonalNotebookResponse toPersonalNotebookResponse(Notebook notebook, Long fileCount) {
        if (notebook == null) {
            return null;
        }

        String thumbnailUrl = notebook.getThumbnailUrl();
        // Chỉ normalize nếu là file local
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            thumbnailUrl = urlNormalizer.normalizeToFull(thumbnailUrl);
        }

        return new PersonalNotebookResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getType(),
                notebook.getVisibility(),
                thumbnailUrl,
                fileCount != null ? fileCount : 0L,
                notebook.getCreatedAt(),
                notebook.getUpdatedAt());
    }

    /**
     * Convert NotebookMember entity sang NotebookMemberItem DTO.
     */
    public NotebookMemberItem toNotebookMemberItem(NotebookMember member) {
        if (member == null) {
            return null;
        }

        User user = member.getUser();
        String avatarUrl = user.getAvatarUrl();

        // Normalize avatar URL nếu là file local
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = urlNormalizer.normalizeToFull(avatarUrl);
        }

        return new NotebookMemberItem(
                member.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                avatarUrl,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt());
    }
}
