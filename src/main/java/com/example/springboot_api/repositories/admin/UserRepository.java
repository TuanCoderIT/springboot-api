package com.example.springboot_api.repositories.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot_api.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
                SELECT u FROM User u
                WHERE (:kw IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')))
                   OR (:kw IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')))
                AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> allUserPage(
            @Param("kw") String keyword,
            @Param("role") String role,
            Pageable pageable);

    java.util.Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.studentCode = :studentCode")
    User findByStudentCode(@Param("studentCode") String studentCode);
    
    @Query("SELECT u FROM User u WHERE u.lecturerCode = :lecturerCode")
    User findByLecturerCode(@Param("lecturerCode") String lecturerCode);

}