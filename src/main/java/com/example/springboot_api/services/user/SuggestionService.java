package com.example.springboot_api.services.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.suggestion.SuggestionResponse;
import com.example.springboot_api.models.NotebookAiSetSuggestion;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetSuggestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final NotebookAiSetSuggestionRepository suggestionRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;

    public SuggestionResponse getSuggestionsByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        // Validation check (can be expanded)
        if (!notebookRepository.existsById(notebookId)) {
            throw new NotFoundException("Notebook không tồn tại");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User không tồn tại");
        }

        NotebookAiSetSuggestion suggestion = suggestionRepository.findAll().stream()
                .filter(s -> s.getNotebookAiSet().getId().equals(aiSetId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy suggestions cho AI Set ID: " + aiSetId));

        // Assuming map structure: { "suggestions": ["q1", "q2"] }
        Map<String, Object> data = suggestion.getSuggestions();
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) data.get("suggestions");

        return SuggestionResponse.builder()
                .aiSetId(aiSetId)
                .suggestions(list)
                .build();
    }
}
