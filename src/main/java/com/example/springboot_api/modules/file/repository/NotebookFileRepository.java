package com.example.springboot_api.modules.file.repository;

import com.example.springboot_api.modules.file.entity.NotebookFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookFileRepository extends JpaRepository<NotebookFile, UUID> {
}