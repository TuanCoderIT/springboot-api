package com.example.springboot_api.modules.quiz.repository;

import com.example.springboot_api.modules.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
}