package com.example.springboot_api.file.repository;

import com.example.springboot_api.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByNotebookId(UUID notebookId);
}
