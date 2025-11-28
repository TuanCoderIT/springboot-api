package com.example.springboot_api.modules.member.entity;

import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notebook_members")
public class NotebookMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;
    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

/*
 TODO [Reverse Engineering] create field to map the 'role' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @ColumnDefault("'member'")
    @Column(name = "role", columnDefinition = "notebook_member_role not null")
    private Object role;
*/
/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @ColumnDefault("'pending'")
    @Column(name = "status", columnDefinition = "notebook_member_status not null")
    private Object status;
*/
}