package com.example.springboot_api.modules.notebook.repository;

import com.example.springboot_api.modules.notebook.entity.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
}