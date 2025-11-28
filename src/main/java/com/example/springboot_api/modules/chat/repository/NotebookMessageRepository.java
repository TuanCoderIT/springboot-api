package com.example.springboot_api.modules.chat.repository;

import com.example.springboot_api.modules.chat.entity.NotebookMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookMessageRepository extends JpaRepository<NotebookMessage, UUID> {
}