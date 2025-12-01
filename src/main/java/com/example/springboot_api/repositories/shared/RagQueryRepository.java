package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.RagQuery;

@Repository
public interface RagQueryRepository extends JpaRepository<RagQuery, UUID> {
    
    @Query("""
            SELECT COUNT(r) FROM Rag_Query r
            WHERE r.notebook.id = :notebookId
            AND r.user.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}

