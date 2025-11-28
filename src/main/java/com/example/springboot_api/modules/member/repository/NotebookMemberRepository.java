package com.example.springboot_api.modules.member.repository;

import com.example.springboot_api.modules.member.entity.NotebookMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotebookMemberRepository extends JpaRepository<NotebookMember, UUID> {
}