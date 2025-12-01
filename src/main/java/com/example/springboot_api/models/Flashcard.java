package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Entity(name = Flashcard.ENTITY_NAME)
@Table(name = Flashcard.TABLE_NAME)
public class Flashcard implements Serializable {
    public static final String ENTITY_NAME = "Flashcard";
    public static final String TABLE_NAME = "flashcards";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_FRONTTEXT_NAME = "front_text";
    public static final String COLUMN_BACKTEXT_NAME = "back_text";
    public static final String COLUMN_EXTRAMETADATA_NAME = "extra_metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -5958316842989249609L;

    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String frontText;

    private String backText;

    private Map<String, Object> extraMetadata;

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
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @Column(name = COLUMN_FRONTTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getFrontText() {
        return frontText;
    }

    @NotNull
    @Column(name = COLUMN_BACKTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getBackText() {
        return backText;
    }

    @Column(name = COLUMN_EXTRAMETADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getExtraMetadata() {
        return extraMetadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


}