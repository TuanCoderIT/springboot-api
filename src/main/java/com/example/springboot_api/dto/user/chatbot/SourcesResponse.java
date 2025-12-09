package com.example.springboot_api.dto.user.chatbot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcesResponse {
    private List<RagSourceResponse> rag;
    private List<WebSourceResponse> web;
}

