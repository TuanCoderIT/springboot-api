package com.example.springboot_api.modules.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookMessageRepository extends JpaRepository<NotebookMessage, UUID> {
}