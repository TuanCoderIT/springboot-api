package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookQuizz;

@Repository
public interface QuizRepository extends JpaRepository<NotebookQuizz, UUID> {

        /**
         * Lấy danh sách quiz theo NotebookAiSet ID kèm options.
         * Sử dụng LEFT JOIN FETCH để load options trong 1 query (tránh N+1).
         */
        @Query("""
                        SELECT DISTINCT q FROM Notebook_Quizz q
                        LEFT JOIN FETCH q.notebookQuizOptions o
                        WHERE q.notebookAiSets.id = :aiSetId
                        ORDER BY q.createdAt ASC
                        """)
        List<NotebookQuizz> findByAiSetIdWithOptions(@Param("aiSetId") UUID aiSetId);

        /**
         * Count quizzes by NotebookAiSet ID
         */
        @Query("""
                        SELECT COUNT(q) FROM Notebook_Quizz q
                        WHERE q.notebookAiSets.id = :aiSetId
                        """)
        long countByAiSetId(@Param("aiSetId") UUID aiSetId);

        /**
         * Count quizzes by notebook ID
         */
        @Query("""
                        SELECT COUNT(q) FROM Notebook_Quizz q
                        WHERE q.notebook.id = :notebookId
                        """)
        long countByNotebookId(@Param("notebookId") UUID notebookId);

        /**
         * Count quizzes by notebook ID and user ID
         */
        @Query("""
                        SELECT COUNT(q) FROM Notebook_Quizz q
                        WHERE q.notebook.id = :notebookId
                        AND q.createdBy.id = :userId
                        """)
        long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}
