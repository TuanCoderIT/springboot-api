package com.example.springboot_api.dto.user.video;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho thông tin video.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

    private UUID id;
    private UUID aiSetId;
    private String videoUrl;
    private String title;
    private String style;
    private Integer durationSeconds;
    private OffsetDateTime createdAt;
}
