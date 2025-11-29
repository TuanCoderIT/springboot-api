package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Entity(name = "Notebook_Activity_Log")
@Table(name = "notebook_activity_logs", schema = "public", indexes = {
        @Index(name = "idx_notebook_activity_notebook", columnList = "notebook_id, created_at"),
        @Index(name = "idx_notebook_activity_user", columnList = "user_id, created_at")
})
public class NotebookActivityLog implements Serializable {
    private static final long serialVersionUID = 8570574930383324193L;
    private UUID id;

    private Notebook notebook;

    private User user;

    private String action;

    private UUID targetId;

    private String targetType;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @Size(max = 64)
    @NotNull
    @Column(name = "action", nullable = false, length = 64)
    public String getAction() {
        return action;
    }

    @Column(name = "target_id")
    public UUID getTargetId() {
        return targetId;
    }

    @Size(max = 64)
    @Column(name = "target_type", length = 64)
    public String getTargetType() {
        return targetType;
    }

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}