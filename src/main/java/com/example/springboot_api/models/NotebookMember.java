package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
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
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = NotebookMember.ENTITY_NAME)
@Table(name = NotebookMember.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "uq_notebook_members_notebook_user", columnList = "notebook_id, user_id", unique = true),
        @Index(name = "idx_notebook_members_user", columnList = "user_id"),
        @Index(name = "idx_notebook_members_status", columnList = "status")
})
public class NotebookMember implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Member";
    public static final String TABLE_NAME = "notebook_members";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ROLE_NAME = "role";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_JOINEDAT_NAME = "joined_at";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 5370032590502638703L;


    private UUID id;

    private Notebook notebook;

    private User user;

    private OffsetDateTime createdAt;

    private OffsetDateTime joinedAt;
    private OffsetDateTime updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
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

    @Column(name = COLUMN_JOINEDAT_NAME)
    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME, nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

/*
 TODO [Reverse Engineering] create field to map the 'role' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object role;
*/
/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object status;
*/
}