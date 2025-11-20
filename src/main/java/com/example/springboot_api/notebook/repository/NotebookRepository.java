package com.example.springboot_api.notebook.repository;

import com.example.springboot_api.notebook.entity.Notebook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookRepository extends JpaRepository<Notebook, UUID> {

    // Lấy list notebook của 1 user + search theo title
    Page<Notebook> findByUserIdAndTitleContainingIgnoreCase(UUID userId, String title, Pageable pageable);

    // Nếu không search
    Page<Notebook> findByUserId(UUID userId, Pageable pageable);
}