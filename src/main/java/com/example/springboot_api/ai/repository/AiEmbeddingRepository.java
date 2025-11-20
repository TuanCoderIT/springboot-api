package com.example.springboot_api.ai.repository;

import com.example.springboot_api.ai.entity.AiEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiEmbeddingRepository extends JpaRepository<AiEmbedding, UUID> {
    Optional<AiEmbedding> findByChunkId(UUID chunkId);
}
