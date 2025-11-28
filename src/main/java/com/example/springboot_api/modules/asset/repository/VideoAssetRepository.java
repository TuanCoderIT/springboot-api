package com.example.springboot_api.modules.asset.repository;

import com.example.springboot_api.modules.asset.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {
}