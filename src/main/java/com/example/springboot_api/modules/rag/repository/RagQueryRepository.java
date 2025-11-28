package com.example.springboot_api.modules.rag.repository;

import com.example.springboot_api.modules.rag.entity.RagQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RagQueryRepository extends JpaRepository<RagQuery, UUID> {
}