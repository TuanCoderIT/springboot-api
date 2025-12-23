package com.example.springboot_api.repositories.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookChapter;

@Repository
public interface NotebookChapterRepository extends JpaRepository<NotebookChapter, UUID> {

    @Query("SELECT c FROM Notebook_Chapter c WHERE c.notebook.id = :notebookId ORDER BY c.sortOrder ASC")
    List<NotebookChapter> findByNotebookIdOrderBySortOrderAsc(@Param("notebookId") UUID notebookId);

    @Query("SELECT MAX(c.sortOrder) FROM Notebook_Chapter c WHERE c.notebook.id = :notebookId")
    Integer findMaxSortOrderByNotebookId(@Param("notebookId") UUID notebookId);
}
