package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.FileChunk;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {

        @Query("SELECT fc.id, fc.chunkIndex, fc.content FROM File_Chunk fc WHERE fc.file.id = :fileId ORDER BY fc.chunkIndex ASC")
        List<Object[]> findChunkDataByFileId(@Param("fileId") UUID fileId);

        @Query("SELECT fc.chunkIndex, fc.content FROM File_Chunk fc WHERE fc.file.id = :fileId ORDER BY fc.chunkIndex ASC")
        List<Object[]> findByFileId(@Param("fileId") UUID fileId);

        /**
         * Lấy chunks với giới hạn số lượng để tránh OutOfMemoryError (dùng native
         * query)
         */
        @Query(value = "SELECT fc.chunk_index, fc.content FROM file_chunks fc WHERE fc.file_id = :fileId ORDER BY fc.chunk_index ASC LIMIT :limit", nativeQuery = true)
        List<Object[]> findByFileIdWithLimit(@Param("fileId") UUID fileId, @Param("limit") int limit);

        @Modifying
        @Query("DELETE FROM File_Chunk fc WHERE fc.file.id = :fileId")
        void deleteByFileId(@Param("fileId") UUID fileId);

        @Query("SELECT COUNT(fc) FROM File_Chunk fc WHERE fc.file.id = :fileId")
        long countByFileId(@Param("fileId") UUID fileId);

        @Query("SELECT fc.content FROM File_Chunk fc WHERE fc.file.id IN :fileIds AND fc.content IS NOT NULL ORDER BY fc.file.id, fc.chunkIndex")
        List<String> findContentByFileIds(@Param("fileIds") List<UUID> fileIds);
}