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

    @Query("""
            SELECT u FROM User u
            WHERE u.role = 'LECTURER'
            AND (
                :kw IS NULL
                OR :kw = ''
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%'))
            )
            """)
    Page<User> findAllLecturers(
            @Param("kw") String keyword,
            Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = 'LECTURER' AND u.id = :id
            """)
    Optional<User> findLecturerById(@Param("id") UUID id);

    Optional<User> findByEmail(String email);
}
