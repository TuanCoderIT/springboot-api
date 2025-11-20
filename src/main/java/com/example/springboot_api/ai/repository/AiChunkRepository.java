package com.example.springboot_api.ai.repository;

import com.example.springboot_api.ai.entity.AiChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiChunkRepository extends JpaRepository<AiChunk, UUID> {
    List<AiChunk> findByFileId(UUID fileId);
}
