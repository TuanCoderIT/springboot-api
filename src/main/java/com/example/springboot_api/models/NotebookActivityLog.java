package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = NotebookActivityLog.ENTITY_NAME)
@Table(name = NotebookActivityLog.TABLE_NAME)
public class NotebookActivityLog implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Activity_Log";
    public static final String TABLE_NAME = "notebook_activity_logs";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ACTION_NAME = "action";
    public static final String COLUMN_TARGETID_NAME = "target_id";
    public static final String COLUMN_TARGETTYPE_NAME = "target_type";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -4857596769998217200L;

    private UUID id;

    private Notebook notebook;

    private User user;

    private String action;

    private UUID targetId;

    private String targetType;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    @Id
    @ColumnDefault("uuid_generate_v4()")
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
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @Size(max = 64)
    @NotNull
    @Column(name = COLUMN_ACTION_NAME, nullable = false, length = 64)
    public String getAction() {
        return action;
    }

    @Column(name = COLUMN_TARGETID_NAME)
    public UUID getTargetId() {
        return targetId;
    }

    @Size(max = 64)
    @Column(name = COLUMN_TARGETTYPE_NAME, length = 64)
    public String getTargetType() {
        return targetType;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}