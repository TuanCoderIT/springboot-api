package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookFile;

@Repository
public interface NotebookFileRepository extends JpaRepository<NotebookFile, UUID> {

    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            ORDER BY nf.createdAt DESC
            """)
    List<NotebookFile> findByNotebookId(@Param("notebookId") UUID notebookId);

    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            AND nf.status = 'approved'
            ORDER BY nf.createdAt DESC
            """)
    List<NotebookFile> findApprovedByNotebookId(@Param("notebookId") UUID notebookId);

    long countByNotebookId(UUID notebookId);

    long countByNotebookIdAndStatus(UUID notebookId, String status);

    @Query("""
            SELECT COUNT(nf) FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            AND nf.uploadedBy.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);

    long countByNotebookIdAndOcrDone(UUID notebookId, Boolean ocrDone);

    long countByNotebookIdAndEmbeddingDone(UUID notebookId, Boolean embeddingDone);

    List<NotebookFile> findByNotebookIdAndStatus(UUID notebookId, String status);

    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.status = 'pending'
            """)
    List<NotebookFile> findAllPendingFiles();

    @Query(value = """
            SELECT nf.* FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND (:status IS NULL OR nf.status = :status)
            AND (:mimeType IS NULL OR nf.mime_type = :mimeType)
            AND (:ocrDone IS NULL OR nf.ocr_done = :ocrDone)
            AND (:embeddingDone IS NULL OR nf.embedding_done = :embeddingDone)
            AND (:uploadedBy IS NULL OR nf.uploaded_by = CAST(:uploadedBy AS uuid))
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, countQuery = """
            SELECT COUNT(nf.*) FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND (:status IS NULL OR nf.status = :status)
            AND (:mimeType IS NULL OR nf.mime_type = :mimeType)
            AND (:ocrDone IS NULL OR nf.ocr_done = :ocrDone)
            AND (:embeddingDone IS NULL OR nf.embedding_done = :embeddingDone)
            AND (:uploadedBy IS NULL OR nf.uploaded_by = CAST(:uploadedBy AS uuid))
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, nativeQuery = true)
    org.springframework.data.domain.Page<NotebookFile> findByNotebookIdWithFilters(
            @Param("notebookId") UUID notebookId,
            @Param("status") String status,
            @Param("mimeType") String mimeType,
            @Param("ocrDone") Boolean ocrDone,
            @Param("embeddingDone") Boolean embeddingDone,
            @Param("uploadedBy") UUID uploadedBy,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT COUNT(nf.*) FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND (:status IS NULL OR nf.status = :status)
            AND (:mimeType IS NULL OR nf.mime_type = :mimeType)
            AND (:ocrDone IS NULL OR nf.ocr_done = :ocrDone)
            AND (:embeddingDone IS NULL OR nf.embedding_done = :embeddingDone)
            AND (:uploadedBy IS NULL OR nf.uploaded_by = CAST(:uploadedBy AS uuid))
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, nativeQuery = true)
    long countByNotebookIdWithFilters(
            @Param("notebookId") UUID notebookId,
            @Param("status") String status,
            @Param("mimeType") String mimeType,
            @Param("ocrDone") Boolean ocrDone,
            @Param("embeddingDone") Boolean embeddingDone,
            @Param("uploadedBy") UUID uploadedBy,
            @Param("search") String search);

    @Query(value = """
            SELECT
                u.id,
                u.full_name,
                u.email,
                u.avatar_url,
                COUNT(nf.id) as files_count
            FROM notebook_files nf
            JOIN users u ON nf.uploaded_by = u.id
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND (:search IS NULL OR LOWER(u.full_name) LIKE LOWER('%' || :search || '%')
                 OR LOWER(u.email) LIKE LOWER('%' || :search || '%'))
            GROUP BY u.id, u.full_name, u.email, u.avatar_url
            ORDER BY files_count DESC, u.full_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    java.util.List<Object[]> findContributorsByNotebookId(
            @Param("notebookId") UUID notebookId,
            @Param("search") String search,
            @Param("limit") int limit);

    @Query(value = """
            SELECT nf.* FROM notebook_files nf
            WHERE nf.status = 'pending'
            AND (:notebookId IS NULL OR nf.notebook_id = CAST(:notebookId AS uuid))
            AND (:mimeType IS NULL OR nf.mime_type = :mimeType)
            AND (:uploadedBy IS NULL OR nf.uploaded_by = CAST(:uploadedBy AS uuid))
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, countQuery = """
            SELECT COUNT(nf.*) FROM notebook_files nf
            WHERE nf.status = 'pending'
            AND (:notebookId IS NULL OR nf.notebook_id = CAST(:notebookId AS uuid))
            AND (:mimeType IS NULL OR nf.mime_type = :mimeType)
            AND (:uploadedBy IS NULL OR nf.uploaded_by = CAST(:uploadedBy AS uuid))
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, nativeQuery = true)
    org.springframework.data.domain.Page<NotebookFile> findPendingFilesWithFilters(
            @Param("notebookId") UUID notebookId,
            @Param("mimeType") String mimeType,
            @Param("uploadedBy") UUID uploadedBy,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            AND nf.status = 'failed'
            ORDER BY nf.createdAt DESC
            """)
    List<NotebookFile> findFailedFilesByNotebookId(@Param("notebookId") UUID notebookId);

    /**
     * Lấy danh sách file theo notebookId:
     * - File có status = 'approved' (đã duyệt và xử lý xong)
     * - File của user hiện tại với các status khác (pending, failed, rejected,
     * processing)
     * - Có tìm kiếm theo tên file
     */
    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            AND (
                nf.status = 'done'
                OR (nf.uploadedBy.id = :userId AND nf.status != 'done')
            )
            AND (:search IS NULL OR :search = '' OR LOWER(nf.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY nf.createdAt DESC
            """)
    List<NotebookFile> findFilesForUserByNotebookId(
            @Param("notebookId") UUID notebookId,
            @Param("userId") UUID userId,
            @Param("search") String search);

    // Methods for Lecturer Workspace
    @Query("""
            SELECT nf FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            ORDER BY nf.createdAt DESC
            """)
    List<NotebookFile> findByNotebookIdOrderByCreatedAtDesc(@Param("notebookId") UUID notebookId);

    @Query(value = """
            SELECT nf.* FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND nf.extra_metadata->>'chapter' = :chapter
            ORDER BY nf.created_at DESC
            """, nativeQuery = true)
    List<NotebookFile> findByNotebookIdAndMetadataChapter(
            @Param("notebookId") UUID notebookId,
            @Param("chapter") String chapter);

    /**
     * Đếm file theo notebook và danh sách status (done/approved).
     */
    @Query("""
            SELECT COUNT(nf) FROM Notebook_File nf
            WHERE nf.notebook.id = :notebookId
            AND nf.status IN :statuses
            """)
    long countByNotebookIdAndStatusIn(
            @Param("notebookId") UUID notebookId,
            @Param("statuses") java.util.List<String> statuses);

    /**
     * Lấy danh sách file theo notebook, filter theo status (done/approved) và
     * search.
     */
    @Query(value = """
            SELECT nf.* FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND nf.status IN (:statuses)
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, countQuery = """
            SELECT COUNT(nf.*) FROM notebook_files nf
            WHERE nf.notebook_id = CAST(:notebookId AS uuid)
            AND nf.status IN (:statuses)
            AND (:search IS NULL OR nf.original_filename::text ILIKE '%' || :search || '%')
            """, nativeQuery = true)
    org.springframework.data.domain.Page<NotebookFile> findByNotebookIdAndStatusInAndSearch(
            @Param("notebookId") UUID notebookId,
            @Param("statuses") java.util.List<String> statuses,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}
