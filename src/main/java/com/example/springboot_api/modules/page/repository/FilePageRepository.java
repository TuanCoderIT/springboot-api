package com.example.springboot_api.modules.page.repository;

import com.example.springboot_api.modules.page.entity.FilePage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FilePageRepository extends JpaRepository<FilePage, UUID> {
}