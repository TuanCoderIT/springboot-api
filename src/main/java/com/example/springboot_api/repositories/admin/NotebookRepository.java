package com.example.springboot_api.repositories.admin;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Notebook;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
}
