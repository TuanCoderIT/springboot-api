package com.example.springboot_api.repositories.shared;

import com.example.springboot_api.models.TtsVoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TtsVoiceRepository extends JpaRepository<TtsVoice, UUID> {
    List<TtsVoice> findByIsActiveTrueOrderBySortOrderAsc();
}
