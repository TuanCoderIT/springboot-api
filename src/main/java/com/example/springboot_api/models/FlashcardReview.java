package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = FlashcardReview.ENTITY_NAME)
@Table(name = FlashcardReview.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_flashcard_reviews_user", columnList = "user_id, review_at")
})
public class FlashcardReview implements Serializable {
    public static final String ENTITY_NAME = "Flashcard_Review";
    public static final String TABLE_NAME = "flashcard_reviews";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_EASEFACTOR_NAME = "ease_factor";
    public static final String COLUMN_INTERVALDAYS_NAME = "interval_days";
    public static final String COLUMN_QUALITY_NAME = "quality";
    public static final String COLUMN_REVIEWAT_NAME = "review_at";
    private static final long serialVersionUID = 8357047291179495627L;


    private UUID id;

    private Flashcard flashcard;

    private User user;

    private Double easeFactor;

    private Integer intervalDays;

    private Integer quality;

    private OffsetDateTime reviewAt;

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
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Column(name = COLUMN_EASEFACTOR_NAME)
    public Double getEaseFactor() {
        return easeFactor;
    }

    @Column(name = COLUMN_INTERVALDAYS_NAME)
    public Integer getIntervalDays() {
        return intervalDays;
    }

    @Column(name = COLUMN_QUALITY_NAME)
    public Integer getQuality() {
        return quality;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_REVIEWAT_NAME, nullable = false)
    public OffsetDateTime getReviewAt() {
        return reviewAt;
    }

}