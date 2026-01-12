package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.CodeExerciseTestcase;

@Repository
public interface CodeExerciseTestcaseRepository extends JpaRepository<CodeExerciseTestcase, UUID> {

    @Query("SELECT t FROM CodeExerciseTestcase t WHERE t.exercise.id = :exerciseId ORDER BY t.orderIndex")
    List<CodeExerciseTestcase> findByExerciseId(@Param("exerciseId") UUID exerciseId);

    @Query("SELECT t FROM CodeExerciseTestcase t WHERE t.exercise.id = :exerciseId AND t.isSample = false ORDER BY t.orderIndex")
    List<CodeExerciseTestcase> findSampleTestcases(@Param("exerciseId") UUID exerciseId);

    void deleteByExerciseId(UUID exerciseId);
}
