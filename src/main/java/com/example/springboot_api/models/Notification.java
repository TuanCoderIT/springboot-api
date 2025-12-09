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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = Notification.ENTITY_NAME)
@Table(name = Notification.TABLE_NAME, schema = "public")
public class Notification implements Serializable {
    public static final String ENTITY_NAME = "Notification";
    public static final String TABLE_NAME = "notifications";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TYPE_NAME = "type";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_CONTENT_NAME = "content";
    public static final String COLUMN_URL_NAME = "url";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_ISREAD_NAME = "is_read";
    public static final String COLUMN_READAT_NAME = "read_at";
    public static final String COLUMN_ACTION_NAME = "action";
    public static final String COLUMN_ROLETARGET_NAME = "role_target";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = -7045505334767134308L;


    private UUID id;

    private User user;

    private String type;

    private String title;

    private String content;

    private String url;

    private Map<String, Object> metadata;

    private Boolean isRead = false;

    private OffsetDateTime readAt;

    private String action;

    private List<String> roleTarget;

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
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_TYPE_NAME, nullable = false, length = 50)
    public String getType() {
        return type;
    }

    @NotNull
    @Column(name = COLUMN_TITLE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getTitle() {
        return title;
    }

    @Column(name = COLUMN_CONTENT_NAME, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
    }

    @Column(name = COLUMN_URL_NAME, length = Integer.MAX_VALUE)
    public String getUrl() {
        return url;
    }

    @NotNull
    @ColumnDefault("'{}'")
    @Column(name = COLUMN_METADATA_NAME, nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_ISREAD_NAME, nullable = false)
    public Boolean getIsRead() {
        return isRead;
    }

    @Column(name = COLUMN_READAT_NAME)
    public OffsetDateTime getReadAt() {
        return readAt;
    }

    @Size(max = 50)
    @ColumnDefault("NULL")
    @Column(name = COLUMN_ACTION_NAME, length = 50)
    public String getAction() {
        return action;
    }

    @ColumnDefault("'{}'")
    @Column(name = COLUMN_ROLETARGET_NAME)
    public List<String> getRoleTarget() {
        return roleTarget;
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