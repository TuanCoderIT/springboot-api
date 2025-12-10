package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookQuizz;

@Repository
public interface QuizRepository extends JpaRepository<NotebookQuizz, UUID> {

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
