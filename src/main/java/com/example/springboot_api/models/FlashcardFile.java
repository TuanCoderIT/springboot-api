package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = FlashcardFile.ENTITY_NAME)
@Table(name = FlashcardFile.TABLE_NAME, schema = "public")
public class FlashcardFile implements Serializable {
    public static final String ENTITY_NAME = "Flashcard_File";
    public static final String TABLE_NAME = "flashcard_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 1734727847814167400L;


    private UUID id;

    private Flashcard flashcard;

    private NotebookFile file;

    private Instant createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "flashcard_id", nullable = false)
    public Flashcard getFlashcard() {
        return flashcard;
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
    public Instant getCreatedAt() {
        return createdAt;
    }

}