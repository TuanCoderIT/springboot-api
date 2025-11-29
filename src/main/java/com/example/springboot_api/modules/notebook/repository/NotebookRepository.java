package com.example.springboot_api.modules.notebook.repository;

import com.example.springboot_api.modules.notebook.entity.Notebook;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookRepository extends JpaRepository<Notebook, UUID> {

    Page<Notebook> findByCreatedBy_Id(UUID userId, Pageable pageable);

    Page<Notebook> findByTypeAndVisibility(
        NotebookType type,
        NotebookVisibility visibility,
        Pageable pageable
    );
}
