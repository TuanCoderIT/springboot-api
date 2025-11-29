package com.example.springboot_api.repositories.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot_api.models.NotebookMember;

public interface NotebookMemberRepository extends JpaRepository<NotebookMember, UUID> {

    List<NotebookMember> findByNotebookIdAndStatus(UUID notebookId, String status);

    Optional<NotebookMember> findByNotebookIdAndUserId(UUID notebookId, UUID userId);
}
