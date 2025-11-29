package com.example.springboot_api.repositories.shared;

import java.util.Optional;
import java.util.UUID;

import com.example.springboot_api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
