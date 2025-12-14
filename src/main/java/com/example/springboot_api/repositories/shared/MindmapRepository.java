package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookMindmap;

@Repository
public interface MindmapRepository extends JpaRepository<NotebookMindmap, UUID> {

    /**
     * Tìm mindmap theo AI Set ID.
     */
    @Query("SELECT m FROM Notebook_Mindmap m WHERE m.sourceAiSet.id = :aiSetId")
    List<NotebookMindmap> findByAiSetId(@Param("aiSetId") UUID aiSetId);

    /**
     * Tìm mindmap đầu tiên theo AI Set ID.
     */
    @Query("SELECT m FROM Notebook_Mindmap m WHERE m.sourceAiSet.id = :aiSetId ORDER BY m.createdAt DESC")
    List<NotebookMindmap> findFirstByAiSetId(@Param("aiSetId") UUID aiSetId);

    /**
     * Tìm tất cả mindmaps theo notebook ID.
     */
    @Query("SELECT m FROM Notebook_Mindmap m WHERE m.notebook.id = :notebookId ORDER BY m.createdAt DESC")
    List<NotebookMindmap> findByNotebookId(@Param("notebookId") UUID notebookId);

    /**
     * Đếm số mindmap theo notebook ID.
     */
    @Query("SELECT COUNT(m) FROM Notebook_Mindmap m WHERE m.notebook.id = :notebookId")
    Long countByNotebookId(@Param("notebookId") UUID notebookId);
}
