package com.example.springboot_api.repositories.admin;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.MajorSubject;

/**
 * Repository quản lý MajorSubject cho Admin.
 */
@Repository
public interface MajorSubjectRepository extends JpaRepository<MajorSubject, UUID> {

    /**
     * Xóa tất cả MajorSubject của một Subject
     */
    @Modifying
    @Query("DELETE FROM Major_Subject ms WHERE ms.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") UUID subjectId);

    /**
     * Xóa tất cả MajorSubject của một Major
     */
    @Modifying
    @Query("DELETE FROM Major_Subject ms WHERE ms.major.id = :majorId")
    void deleteByMajorId(@Param("majorId") UUID majorId);

    /**
     * Kiểm tra liên kết đã tồn tại chưa
     */
    @Query("SELECT COUNT(ms) > 0 FROM Major_Subject ms WHERE ms.major.id = :majorId AND ms.subject.id = :subjectId")
    boolean existsByMajorIdAndSubjectId(@Param("majorId") UUID majorId, @Param("subjectId") UUID subjectId);
}
