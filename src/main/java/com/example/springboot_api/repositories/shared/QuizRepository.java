package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Quiz;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    long countByNotebookId(UUID notebookId);
}

