package com.example.springboot_api.modules.quiz.repository;

import com.example.springboot_api.modules.quiz.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, UUID> {
}