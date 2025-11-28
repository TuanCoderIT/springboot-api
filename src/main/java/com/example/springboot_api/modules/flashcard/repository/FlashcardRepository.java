package com.example.springboot_api.modules.flashcard.repository;

import com.example.springboot_api.modules.flashcard.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
}