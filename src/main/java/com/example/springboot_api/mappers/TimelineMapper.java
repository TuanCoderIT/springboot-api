package com.example.springboot_api.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.timeline.TimelineEventResponse;
import com.example.springboot_api.dto.user.timeline.TimelineResponse;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.TimelineEvent;
import com.example.springboot_api.models.User;

import lombok.RequiredArgsConstructor;

/**
 * Mapper cho Timeline entities -> DTOs.
 */
@Component
@RequiredArgsConstructor
public class TimelineMapper {

    /**
     * Convert TimelineEvent entity -> TimelineEventResponse DTO.
     */
    public TimelineEventResponse toEventResponse(TimelineEvent event) {
        if (event == null)
            return null;

        return TimelineEventResponse.builder()
                .id(event.getId())
                .order(event.getEventOrder())
                .date(event.getDate())
                .datePrecision(event.getDatePrecision())
                .title(event.getTitle())
                .description(event.getDescription())
                .importance(event.getImportance())
                .icon(event.getIcon())
                .build();
    }

    /**
     * Convert list of TimelineEvent entities -> list of TimelineEventResponse DTOs.
     */
    public List<TimelineEventResponse> toEventResponseList(List<TimelineEvent> events) {
        if (events == null)
            return List.of();

        return events.stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Build TimelineResponse from AI Set and events.
     */
    public TimelineResponse toTimelineResponse(NotebookAiSet aiSet, List<TimelineEvent> events) {
        if (aiSet == null)
            return null;

        return TimelineResponse.builder()
                .aiSetId(aiSet.getId())
                .title(aiSet.getTitle())
                .mode(extractMode(aiSet))
                .totalEvents(events != null ? events.size() : 0)
                .status(aiSet.getStatus())
                .createdAt(aiSet.getCreatedAt())
                .events(toEventResponseList(events))
                .createdBy(toCreatorInfo(aiSet.getCreatedBy()))
                .build();
    }

    private TimelineResponse.CreatorInfo toCreatorInfo(User user) {
        if (user == null)
            return null;

        return TimelineResponse.CreatorInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private String extractMode(NotebookAiSet aiSet) {
        if (aiSet.getInputConfig() != null && aiSet.getInputConfig().containsKey("mode")) {
            return (String) aiSet.getInputConfig().get("mode");
        }
        return "logic";
    }
}
