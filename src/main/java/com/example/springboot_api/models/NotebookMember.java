package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Notebook_Member")
@Table(name = "notebook_members", schema = "public", indexes = {
        @Index(name = "uq_notebook_members_notebook_user", columnList = "notebook_id, user_id", unique = true),
        @Index(name = "idx_notebook_members_user", columnList = "user_id"),
        @Index(name = "idx_notebook_members_status", columnList = "status")
})
public class NotebookMember implements Serializable {
    private static final long serialVersionUID = -8293171859453161881L;
    private UUID id;

    private Notebook notebook;

    private User user;

    private String role;

    private String status;

    private OffsetDateTime joinedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "role", nullable = false, length = 50)
    public String getRole() {
        return role;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    public String getStatus() {
        return status;
    }

    @Column(name = "joined_at")
    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}