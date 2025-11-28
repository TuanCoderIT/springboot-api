package com.example.springboot_api.modules.quiz.repository;

import com.example.springboot_api.modules.quiz.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {
}