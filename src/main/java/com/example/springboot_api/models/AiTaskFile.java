package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@ToString
@Entity(name = AiTaskFile.ENTITY_NAME)
@Table(name = AiTaskFile.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_ai_task_files_task_file", columnList = "task_id, file_id", unique = true),
        @Index(name = "idx_ai_task_files_file_id", columnList = "file_id")
})
public class AiTaskFile implements Serializable {
    public static final String ENTITY_NAME = "Ai_Task_File";
    public static final String TABLE_NAME = "ai_task_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ROLE_NAME = "role";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -766509021523105296L;


    private UUID id;

    private AiTask task;

    private NotebookFile file;

    private String role;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id", nullable = false)
    public AiTask getTask() {
        return task;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @Column(name = COLUMN_ROLE_NAME, length = Integer.MAX_VALUE)
    public String getRole() {
        return role;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}