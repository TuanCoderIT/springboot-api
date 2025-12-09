package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = NotebookBotConversationState.ENTITY_NAME)
@Table(name = NotebookBotConversationState.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "ux_bot_conv_state_user_notebook", columnList = "user_id, notebook_id", unique = true)
})
public class NotebookBotConversationState implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Bot_Conversation_State";
    public static final String TABLE_NAME = "notebook_bot_conversation_states";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_LASTOPENEDAT_NAME = "last_opened_at";
    public static final String COLUMN_METADATA_NAME = "metadata";
    private static final long serialVersionUID = 4648652614646518802L;


    private UUID id;

    private User user;

    private Notebook notebook;

    private NotebookBotConversation conversation;

    private OffsetDateTime lastOpenedAt;

    private Map<String, Object> metadata;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "conversation_id", nullable = false)
    public NotebookBotConversation getConversation() {
        return conversation;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_LASTOPENEDAT_NAME, nullable = false)
    public OffsetDateTime getLastOpenedAt() {
        return lastOpenedAt;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}