package com.example.springboot_api.repositories.admin;

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
        Pageable pageable
    );
}
