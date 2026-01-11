package com.example.springboot_api.repositories.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Notebook;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {

    /**
     * Tìm notebooks theo type (ví dụ: "regulation", "community", "personal").
     */
    List<Notebook> findByType(String type);

    @Query("""
            SELECT n FROM Notebook n
            WHERE n.type = 'community'
            AND (:q IS NULL OR :q = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(n.description) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:visibility IS NULL OR :visibility = '' OR n.visibility = :visibility)
            """)
    Page<Notebook> findCommunities(
            @Param("q") String keyword,
            @Param("visibility") String visibility,
            Pageable pageable);

    @Query("""
            SELECT n FROM Notebook n
            WHERE n.type = 'community'
            AND n.id NOT IN (
                SELECT nm.notebook.id FROM Notebook_Member nm
                WHERE nm.user.id = :userId
                AND nm.status IN ('approved', 'pending')
            )
            AND (:q IS NULL OR :q = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(n.description) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:visibility IS NULL OR :visibility = '' OR n.visibility = :visibility)
            """)
    Page<Notebook> findAvailableCommunities(
            @Param("userId") UUID userId,
            @Param("q") String keyword,
            @Param("visibility") String visibility,
            Pageable pageable);

    @Query("""
            SELECT n FROM Notebook n
            WHERE n.type = 'personal'
            AND n.id IN (
                SELECT nm.notebook.id FROM Notebook_Member nm
                WHERE nm.user.id = :userId
                AND nm.role = 'owner'
                AND nm.status = 'approved'
            )
            AND (:q IS NULL OR :q = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(n.description) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Notebook> findPersonalNotebooksByUserId(
            @Param("userId") UUID userId,
            @Param("q") String keyword,
            Pageable pageable);

    // Method for Lecturer Workspace Management
    @Query("""
            SELECT n FROM Notebook n
            WHERE n.type IN ('assignment', 'community')
            AND n.id IN (
                SELECT nm.notebook.id FROM Notebook_Member nm
                WHERE nm.user.id = :lecturerId
                AND nm.role IN ('owner', 'lecturer')
                AND nm.status = 'approved'
            )
            ORDER BY n.createdAt DESC
            """)
    java.util.List<Notebook> findClassNotebooksByLecturerId(@Param("lecturerId") UUID lecturerId);
}
