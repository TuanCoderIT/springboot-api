package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookQuizFile;

@Repository
public interface QuizFileRepository extends JpaRepository<NotebookQuizFile, UUID> {

    /**
     * Tìm tất cả files liên kết với quiz ID
     */
    @Query("""
            SELECT qf FROM Notebook_Quiz_File qf
            WHERE qf.quiz.id = :quizId
            """)
    List<NotebookQuizFile> findByQuizId(@Param("quizId") UUID quizId);
}
