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
@Entity(name = QuizFile.ENTITY_NAME)
@Table(name = QuizFile.TABLE_NAME, schema = "public")
public class QuizFile implements Serializable {
    public static final String ENTITY_NAME = "Quiz_File";
    public static final String TABLE_NAME = "quiz_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -4489002159142380319L;


    private UUID id;

    private Quiz quiz;

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
    @JoinColumn(name = "quiz_id", nullable = false)
    public Quiz getQuiz() {
        return quiz;
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