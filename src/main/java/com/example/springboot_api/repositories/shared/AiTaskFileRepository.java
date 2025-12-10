package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.AiTaskFile;

@Repository
public interface AiTaskFileRepository extends JpaRepository<AiTaskFile, UUID> {

  @Query("""
      SELECT atf FROM Ai_Task_File atf
      WHERE atf.task.id = :taskId
      ORDER BY atf.createdAt ASC
      """)
  List<AiTaskFile> findByTaskId(@Param("taskId") UUID taskId);

  @Query("""
      SELECT atf FROM Ai_Task_File atf
      WHERE atf.file.id = :fileId
      ORDER BY atf.createdAt DESC
      """)
  List<AiTaskFile> findByFileId(@Param("fileId") UUID fileId);

  @Query("""
      SELECT atf FROM Ai_Task_File atf
      WHERE atf.task.id = :taskId
      AND atf.role = :role
      ORDER BY atf.createdAt ASC
      """)
  List<AiTaskFile> findByTaskIdAndRole(@Param("taskId") UUID taskId, @Param("role") String role);

  boolean existsByTaskIdAndFileId(UUID taskId, UUID fileId);

  long countByTaskId(UUID taskId);
}