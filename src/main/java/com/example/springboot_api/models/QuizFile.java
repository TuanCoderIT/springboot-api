package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.Instant;
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
@Entity(name = QuizFile.ENTITY_NAME)
@Table(name = QuizFile.TABLE_NAME)
public class QuizFile implements Serializable {
    public static final String ENTITY_NAME = "Quiz_File";
    public static final String TABLE_NAME = "quiz_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -107357420881514906L;

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