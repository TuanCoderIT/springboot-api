package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookAiSetSuggestion;

@Repository
public interface NotebookAiSetSuggestionRepository extends JpaRepository<NotebookAiSetSuggestion, UUID> {
}
