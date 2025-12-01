package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Entity(name = TtsFile.ENTITY_NAME)
@Table(name = TtsFile.TABLE_NAME)
public class TtsFile implements Serializable {
    public static final String ENTITY_NAME = "Tts_File";
    public static final String TABLE_NAME = "tts_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 6771184132635978895L;

    private UUID id;

    private TtsAsset tts;

    private NotebookFile file;

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
    @JoinColumn(name = "tts_id", nullable = false)
    public TtsAsset getTts() {
        return tts;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}