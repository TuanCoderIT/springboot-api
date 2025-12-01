package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Flashcard;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    long countByNotebookId(UUID notebookId);

    @Query("""
            SELECT COUNT(f) FROM Flashcard f
            WHERE f.notebook.id = :notebookId
            AND f.createdBy.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}

