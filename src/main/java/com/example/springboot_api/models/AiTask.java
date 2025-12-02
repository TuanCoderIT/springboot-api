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
@ToString
@Entity(name = AiTask.ENTITY_NAME)
@Table(name = AiTask.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_ai_tasks_notebook", columnList = "notebook_id, created_at"),
        @Index(name = "idx_ai_tasks_type_status", columnList = "task_type, status"),
        @Index(name = "idx_ai_tasks_status", columnList = "status")
})
public class AiTask implements Serializable {
    public static final String ENTITY_NAME = "Ai_Task";
    public static final String TABLE_NAME = "ai_tasks";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TASKTYPE_NAME = "task_type";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_INPUTCONFIG_NAME = "input_config";
    public static final String COLUMN_OUTPUTDATA_NAME = "output_data";
    public static final String COLUMN_ERRORMESSAGE_NAME = "error_message";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 7454011392209553171L;


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
    @Column(name = COLUMN_TASKTYPE_NAME, nullable = false, length = 50)
    public String getTaskType() {
        return taskType;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_STATUS_NAME, nullable = false, length = 50)
    public String getStatus() {
        return status;
    }

    @Column(name = COLUMN_INPUTCONFIG_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getInputConfig() {
        return inputConfig;
    }

    @Column(name = COLUMN_OUTPUTDATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getOutputData() {
        return outputData;
    }

    @Column(name = COLUMN_ERRORMESSAGE_NAME, length = Integer.MAX_VALUE)
    public String getErrorMessage() {
        return errorMessage;
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

}