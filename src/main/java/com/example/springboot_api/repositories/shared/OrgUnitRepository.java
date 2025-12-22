package com.example.springboot_api.repositories.shared;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.OrgUnit;

@Repository
public interface OrgUnitRepository extends JpaRepository<OrgUnit, UUID> {

    /**
     * Tìm OrgUnit theo code
     */
    Optional<OrgUnit> findByCode(String code);

    /**
     * Kiểm tra code đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Lấy danh sách với filter: search (code/name), type, isActive
     */
    @Query("""
            SELECT o FROM Org_Unit o
            WHERE (:q IS NULL OR :q = '' OR LOWER(o.code) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(o.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:type IS NULL OR o.type = :type)
            AND (:isActive IS NULL OR o.isActive = :isActive)
            """)
    Page<OrgUnit> findAllWithFilters(
            @Param("q") String q,
            @Param("type") String type,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
