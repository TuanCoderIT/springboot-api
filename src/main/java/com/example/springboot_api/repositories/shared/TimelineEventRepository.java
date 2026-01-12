package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.TimelineEvent;

/**
 * Repository cho TimelineEvent entity.
 */
@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    /**
     * Lấy tất cả events theo AI Set ID, sắp xếp theo thứ tự.
     */
    @Query("SELECT e FROM Timeline_Event e WHERE e.notebookAiSets.id = :aiSetId ORDER BY e.eventOrder ASC")
    List<TimelineEvent> findByAiSetIdOrderByEventOrder(@Param("aiSetId") UUID aiSetId);

    /**
     * Lấy events theo notebook ID.
     */
    @Query("SELECT e FROM Timeline_Event e WHERE e.notebook.id = :notebookId ORDER BY e.eventOrder ASC")
    List<TimelineEvent> findByNotebookId(@Param("notebookId") UUID notebookId);

    /**
     * Đếm số events theo AI Set ID.
     */
    @Query("SELECT COUNT(e) FROM Timeline_Event e WHERE e.notebookAiSets.id = :aiSetId")
    long countByAiSetId(@Param("aiSetId") UUID aiSetId);

    /**
     * Xóa tất cả events theo AI Set ID.
     */
    void deleteByNotebookAiSetsId(UUID aiSetId);
}
