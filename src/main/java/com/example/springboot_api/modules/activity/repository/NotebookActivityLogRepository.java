package com.example.springboot_api.modules.activity.repository;

import com.example.springboot_api.modules.activity.entity.NotebookActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookActivityLogRepository extends JpaRepository<NotebookActivityLog, UUID> {
}