package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.SupportedLanguage;

@Repository
public interface SupportedLanguageRepository extends JpaRepository<SupportedLanguage, UUID> {

    @Query("SELECT l FROM SupportedLanguage l WHERE l.isActive = true ORDER BY l.name")
    List<SupportedLanguage> findAllActive();

    Optional<SupportedLanguage> findByNameAndVersion(String name, String version);

    @Query("SELECT l FROM SupportedLanguage l WHERE l.name = :name AND l.isActive = true ORDER BY l.version DESC LIMIT 1")
    Optional<SupportedLanguage> findLatestByName(@Param("name") String name);
}
