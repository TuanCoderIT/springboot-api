package com.example.springboot_api.modules.chat.repository;

import com.example.springboot_api.modules.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
}