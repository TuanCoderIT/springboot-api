package com.example.springboot_api.modules.chunk.repository;

import com.example.springboot_api.modules.chunk.entity.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {
}