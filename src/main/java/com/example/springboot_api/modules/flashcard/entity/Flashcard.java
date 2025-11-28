package com.example.springboot_api.modules.flashcard.entity;

import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import com.example.springboot_api.modules.file.entity.NotebookFile;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "flashcards")
public class Flashcard {
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
    @JoinColumn(name = "created_by")
    private User createdBy;

    @NotNull
    @Column(name = "front_text", nullable = false, length = Integer.MAX_VALUE)
    private String frontText;

    @NotNull
    @Column(name = "back_text", nullable = false, length = Integer.MAX_VALUE)
    private String backText;

    @Column(name = "extra_metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extraMetadata;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "flashcard")
    private Set<FlashcardReview> flashcardReviews = new LinkedHashSet<>();

}