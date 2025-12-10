package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookQuizOption;

@Repository
public interface QuizOptionRepository extends JpaRepository<NotebookQuizOption, UUID> {

    /**
     * Tìm tất cả options theo quiz ID
     */
    @Query("""
            SELECT o FROM Notebook_Quiz_Option o
            WHERE o.quiz.id = :quizId
            ORDER BY o.position ASC
            """)
    List<NotebookQuizOption> findByQuizId(@Param("quizId") UUID quizId);
}
