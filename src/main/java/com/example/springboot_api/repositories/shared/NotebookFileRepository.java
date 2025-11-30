package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookFile;

@Repository
public interface NotebookFileRepository extends JpaRepository<NotebookFile, UUID> {
    
    @Query("""
        SELECT nf FROM Notebook_File nf
        WHERE nf.notebook.id = :notebookId
        ORDER BY nf.createdAt DESC
        """)
    List<NotebookFile> findByNotebookId(@Param("notebookId") UUID notebookId);
}

