package com.example.springboot_api.modules.flashcard.entity;

import com.example.springboot_api.modules.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ease_factor")
    private Double easeFactor;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "quality")
    private Integer quality;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "review_at", nullable = false)
    private OffsetDateTime reviewAt;

}