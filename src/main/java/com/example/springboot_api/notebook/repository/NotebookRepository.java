package com.example.springboot_api.notebook.repository;

import com.example.springboot_api.notebook.entity.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
    List<Notebook> findByUserId(UUID userId);
}