package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookAiSet;

@Repository
public interface NotebookAiSetRepository extends JpaRepository<NotebookAiSet, UUID> {

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookId(@Param("notebookId") UUID notebookId);

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.setType = :setType
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookIdAndSetType(@Param("notebookId") UUID notebookId,
            @Param("setType") String setType);

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.status = :status
            ORDER BY nas.createdAt ASC
            """)
    List<NotebookAiSet> findByStatus(@Param("status") String status);

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.status = :status
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookIdAndStatus(@Param("notebookId") UUID notebookId,
            @Param("status") String status);

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.createdBy.id = :userId
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByCreatedById(@Param("userId") UUID userId);

    /**
     * Lấy AI sets đã hoàn thành theo notebook (cho tất cả users)
     */
    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.status = 'done'
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findCompletedByNotebookId(@Param("notebookId") UUID notebookId);

    /**
     * Lấy tất cả AI sets của user trong notebook (bao gồm mọi status)
     */
    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.createdBy.id = :userId
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);

    /**
     * Lấy AI sets đã hoàn thành của người khác trong notebook
     */
    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.status = 'done'
            AND (nas.createdBy.id != :userId OR nas.createdBy IS NULL)
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findCompletedByNotebookIdExcludeUser(@Param("notebookId") UUID notebookId,
            @Param("userId") UUID userId);

    long countByNotebookIdAndStatus(UUID notebookId, String status);

    long countByNotebookIdAndSetType(UUID notebookId, String setType);

    // Methods for Lecturer Workspace
    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookIdOrderByCreatedAtDesc(@Param("notebookId") UUID notebookId);

    @Query("""
            SELECT nas FROM Notebook_Ai_Set nas
            WHERE nas.notebook.id = :notebookId
            AND nas.setType = :setType
            ORDER BY nas.createdAt DESC
            """)
    List<NotebookAiSet> findByNotebookIdAndSetTypeOrderByCreatedAtDesc(
            @Param("notebookId") UUID notebookId,
            @Param("setType") String setType);
}
