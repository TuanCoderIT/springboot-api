package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.CodeExerciseFile;

@Repository
public interface CodeExerciseFileRepository extends JpaRepository<CodeExerciseFile, UUID> {

    @Query("SELECT f FROM CodeExerciseFile f WHERE f.exercise.id = :exerciseId AND f.role = :role ORDER BY f.isMain DESC")
    List<CodeExerciseFile> findByExerciseIdAndRole(@Param("exerciseId") UUID exerciseId, @Param("role") String role);

    @Query("SELECT f FROM CodeExerciseFile f WHERE f.exercise.id = :exerciseId AND f.user.id = :userId AND f.role = 'user'")
    List<CodeExerciseFile> findUserFiles(@Param("exerciseId") UUID exerciseId, @Param("userId") UUID userId);

    @Query("SELECT f FROM CodeExerciseFile f WHERE f.exercise.id = :exerciseId AND f.role = 'solution' ORDER BY f.isMain DESC")
    List<CodeExerciseFile> findSolutionFiles(@Param("exerciseId") UUID exerciseId);

    void deleteByExerciseId(UUID exerciseId);
}
