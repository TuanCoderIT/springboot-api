package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.CodeExercise;

@Repository
public interface CodeExerciseRepository extends JpaRepository<CodeExercise, UUID> {

    @Query("SELECT e FROM CodeExercise e WHERE e.notebookAiSet.id = :aiSetId ORDER BY e.orderIndex")
    List<CodeExercise> findByAiSetId(@Param("aiSetId") UUID aiSetId);

    @Query("SELECT e FROM CodeExercise e WHERE e.notebook.id = :notebookId ORDER BY e.createdAt DESC")
    List<CodeExercise> findByNotebookId(@Param("notebookId") UUID notebookId);

    @Query("SELECT COUNT(e) FROM CodeExercise e WHERE e.notebookAiSet.id = :aiSetId")
    long countByAiSetId(@Param("aiSetId") UUID aiSetId);

    void deleteByNotebookAiSetId(UUID aiSetId);
}
