package com.example.springboot_api.repositories.admin;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot_api.models.User;

public interface LecturerRepository extends JpaRepository<User, UUID> {

        /**
         * Lấy danh sách giảng viên với filter: keyword, orgUnitId
         */
        @Query("""
                        SELECT u FROM User u
                        WHERE u.role = 'TEACHER'
                        AND (
                            :kw IS NULL
                            OR :kw = ''
                            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:kw AS string), '%'))
                            OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:kw AS string), '%'))
                            OR LOWER(u.lecturerCode) LIKE LOWER(CONCAT('%', CAST(:kw AS string), '%'))
                        )
                        AND (:orgUnitId IS NULL OR u.primaryOrgUnit.id = :orgUnitId)
                        """)
        Page<User> findAllLecturers(
                        @Param("kw") String keyword,
                        @Param("orgUnitId") UUID orgUnitId,
                        Pageable pageable);

        @Query("""
                        SELECT u FROM User u
                        WHERE u.role = 'TEACHER' AND u.id = :id
                        """)
        Optional<User> findLecturerById(@Param("id") UUID id);

        Optional<User> findByEmail(String email);

        /**
         * Kiểm tra mã giảng viên đã tồn tại chưa
         */
        boolean existsByLecturerCode(String lecturerCode);
}
