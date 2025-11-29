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
@Entity(name = "Flashcard_Review")
@Table(name = "flashcard_reviews", schema = "public", indexes = {
        @Index(name = "idx_flashcard_reviews_user", columnList = "user_id, review_at")
})
public class FlashcardReview implements Serializable {
    private static final long serialVersionUID = 1146723971813767511L;
    private UUID id;

    private Flashcard flashcard;

    private User user;

    private Double easeFactor;

    private Integer intervalDays;

    private Integer quality;

    private OffsetDateTime reviewAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
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

    @Column(name = "ease_factor")
    public Double getEaseFactor() {
        return easeFactor;
    }

    @Column(name = "interval_days")
    public Integer getIntervalDays() {
        return intervalDays;
    }

    @Column(name = "quality")
    public Integer getQuality() {
        return quality;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "review_at", nullable = false)
    public OffsetDateTime getReviewAt() {
        return reviewAt;
    }

}