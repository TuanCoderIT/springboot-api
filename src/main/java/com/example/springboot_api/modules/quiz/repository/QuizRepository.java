package com.example.springboot_api.modules.quiz.repository;

import com.example.springboot_api.modules.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
}