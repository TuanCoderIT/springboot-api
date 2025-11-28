package com.example.springboot_api.modules.asset.repository;

import com.example.springboot_api.modules.asset.entity.TtsAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TtsAssetRepository extends JpaRepository<TtsAsset, UUID> {
}