package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.VideoAsset;

@Repository
public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {
    
    @Query("""
            SELECT COUNT(v) FROM Video_Asset v
            WHERE v.notebook.id = :notebookId
            AND v.createdBy.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}

