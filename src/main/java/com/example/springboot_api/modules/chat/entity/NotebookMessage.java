package com.example.springboot_api.modules.chat.entity;

import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.chat.entity.enums.NotebookMessageType;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notebook_messages")
public class NotebookMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "reply_to_message_id")
    private NotebookMessage replyToMessage;

    @Column(name = "ai_context")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> aiContext;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotebookMessageType type;

    @OneToMany(mappedBy = "message")
    private Set<MessageReaction> messageReactions = new LinkedHashSet<>();
    @OneToMany(mappedBy = "replyToMessage")
    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    /*
     * TODO [Reverse Engineering] create field to map the 'type' column
     * Available actions: Define target Java type | Uncomment as is | Remove column
     * mapping
     * 
     * @ColumnDefault("'user'")
     * 
     * @Column(name = "type", columnDefinition = "notebook_message_type not null")
     * private Object type;
     */
}