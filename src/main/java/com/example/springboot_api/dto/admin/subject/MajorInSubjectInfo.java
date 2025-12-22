package com.example.springboot_api.dto.admin.subject;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MajorInSubjectInfo {
    private UUID id;
    private String code;
    private String name;
    private Integer termNo;
    private Boolean isRequired;
    private String knowledgeBlock;
}
