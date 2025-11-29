package com.example.springboot_api.modules.ai_task.entity;

import com.example.springboot_api.modules.ai_task.entity.enums.AiTaskStatus;
import com.example.springboot_api.modules.ai_task.entity.enums.AiTaskType;
import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.file.entity.NotebookFile;
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
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_tasks")
public class AiTask {
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
    @JoinColumn(name = "file_id")
    private NotebookFile file;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "input_config")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> inputConfig;

    @Column(name = "output_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> outputData;
    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;
    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private AiTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiTaskStatus status;

    /*
     * TODO [Reverse Engineering] create field to map the 'task_type' column
     * Available actions: Define target Java type | Uncomment as is | Remove column
     * mapping
     * 
     * @Column(name = "task_type", columnDefinition = "ai_task_type not null")
     * private Object taskType;
     */
    /*
     * TODO [Reverse Engineering] create field to map the 'status' column
     * Available actions: Define target Java type | Uncomment as is | Remove column
     * mapping
     * 
     * @ColumnDefault("'queued'")
     * 
     * @Column(name = "status", columnDefinition = "ai_task_status not null")
     * private Object status;
     */
}