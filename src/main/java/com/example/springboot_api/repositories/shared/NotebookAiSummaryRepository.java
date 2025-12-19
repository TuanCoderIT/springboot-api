package com.example.springboot_api.repositories.shared;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookAiSummary;

@Repository
public interface NotebookAiSummaryRepository extends JpaRepository<NotebookAiSummary, UUID> {

    @Query("""
            SELECT s FROM Notebook_Ai_Summary s
            WHERE s.notebookAiSets.id = :aiSetId
            """)
    Optional<NotebookAiSummary> findByAiSetId(@Param("aiSetId") UUID aiSetId);

    @Query("""
            SELECT COUNT(s) FROM Notebook_Ai_Summary s
            WHERE s.notebookAiSets.notebook.id = :notebookId
            """)
    long countByNotebookId(@Param("notebookId") UUID notebookId);
}
