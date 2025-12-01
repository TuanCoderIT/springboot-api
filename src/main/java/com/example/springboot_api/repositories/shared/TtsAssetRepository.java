package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.TtsAsset;

@Repository
public interface TtsAssetRepository extends JpaRepository<TtsAsset, UUID> {
    
    @Query("""
            SELECT COUNT(t) FROM Tts_Asset t
            WHERE t.notebook.id = :notebookId
            AND t.createdBy.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}

