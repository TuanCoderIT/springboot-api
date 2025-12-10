package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookAiSetFile;

@Repository
public interface NotebookAiSetFileRepository extends JpaRepository<NotebookAiSetFile, UUID> {

        @Query("""
                        SELECT nasf FROM Notebook_Ai_Set_File nasf
                        WHERE nasf.aiSet.id = :aiSetId
                        ORDER BY nasf.createdAt ASC
                        """)
        List<NotebookAiSetFile> findByAiSetId(@Param("aiSetId") UUID aiSetId);

        @Query("""
                        SELECT COUNT(nasf) FROM Notebook_Ai_Set_File nasf
                        WHERE nasf.aiSet.id = :aiSetId
                        """)
        long countByAiSetId(@Param("aiSetId") UUID aiSetId);

        /**
         * Xóa tất cả file liên kết theo AI Set ID
         */
        @Modifying
        @Query("""
                        DELETE FROM Notebook_Ai_Set_File nasf
                        WHERE nasf.aiSet.id = :aiSetId
                        """)
        void deleteByAiSetId(@Param("aiSetId") UUID aiSetId);
}
