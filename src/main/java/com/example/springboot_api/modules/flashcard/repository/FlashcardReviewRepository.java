package com.example.springboot_api.modules.flashcard.repository;

import com.example.springboot_api.modules.flashcard.entity.FlashcardReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, UUID> {
}