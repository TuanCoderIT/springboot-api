package com.example.springboot_api.repositories.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.MessageReaction;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);

    List<MessageReaction> findByMessageId(UUID messageId);

    @Query("""
            SELECT mr FROM Message_Reaction mr
            JOIN FETCH mr.user u
            WHERE mr.message.id = :messageId
            ORDER BY mr.createdAt ASC
            """)
    List<MessageReaction> findByMessageIdWithUser(@Param("messageId") UUID messageId);
}
