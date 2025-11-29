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
@Entity(name = "Ai_Task")
@Table(name = "ai_tasks", schema = "public", indexes = {
        @Index(name = "idx_ai_tasks_notebook", columnList = "notebook_id, created_at"),
        @Index(name = "idx_ai_tasks_type_status", columnList = "task_type, status"),
        @Index(name = "idx_ai_tasks_status", columnList = "status")
})
public class AiTask implements Serializable {
    private static final long serialVersionUID = -2698470328924756312L;
    private UUID id;

    private Notebook notebook;

    private NotebookFile file;

    private User user;

    private String taskType;

    private String status;

    private Map<String, Object> inputConfig;

    private Map<String, Object> outputData;

    private String errorMessage;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "file_id")
    public NotebookFile getFile() {
        return file;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "task_type", nullable = false, length = 50)
    public String getTaskType() {
        return taskType;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    public String getStatus() {
        return status;
    }

    @Column(name = "input_config")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getInputConfig() {
        return inputConfig;
    }

    @Column(name = "output_data")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getOutputData() {
        return outputData;
    }

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    public String getErrorMessage() {
        return errorMessage;
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