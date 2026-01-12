package com.example.springboot_api.dto.user.timeline;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho một timeline event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventResponse {

    private UUID id;

    /**
     * Thứ tự sự kiện trong timeline.
     */
    private Integer order;

    /**
     * Mốc thời gian: "1945" hoặc "1945-08-19".
     */
    private String date;

    /**
     * Độ chính xác của date: year, month, day, unknown.
     */
    private String datePrecision;

    /**
     * Tiêu đề sự kiện.
     */
    private String title;

    /**
     * Mô tả ngắn (max 180 ký tự).
     */
    private String description;

    /**
     * Mức độ quan trọng: minor, normal, major, critical.
     */
    private String importance;

    /**
     * Icon type: history, network, milestone, process, etc.
     */
    private String icon;
}
