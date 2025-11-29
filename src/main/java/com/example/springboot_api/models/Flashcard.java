package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Flashcard")
@Table(name = "flashcards", schema = "public", indexes = {
        @Index(name = "idx_flashcards_notebook", columnList = "notebook_id")
})
public class Flashcard implements Serializable {
    private static final long serialVersionUID = -7696285719380330236L;
    private UUID id;

    private Notebook notebook;

    private NotebookFile file;

    private User createdBy;

    private String frontText;

    private String backText;

    private Map<String, Object> extraMetadata;

    private OffsetDateTime createdAt;

    private Set<FlashcardReview> flashcardReviews = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
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
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @Column(name = "front_text", nullable = false, length = Integer.MAX_VALUE)
    public String getFrontText() {
        return frontText;
    }

    @NotNull
    @Column(name = "back_text", nullable = false, length = Integer.MAX_VALUE)
    public String getBackText() {
        return backText;
    }

    @Column(name = "extra_metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getExtraMetadata() {
        return extraMetadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @OneToMany(mappedBy = "flashcard")
    public Set<FlashcardReview> getFlashcardReviews() {
        return flashcardReviews;
    }

}