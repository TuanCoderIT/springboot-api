package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "User")
@Table(name = "users", schema = "public", indexes = {
        @Index(name = "idx_users_email", columnList = "email")
}, uniqueConstraints = {
        @UniqueConstraint(name = "users_email_key", columnNames = { "email" })
})
public class User implements Serializable {
    private static final long serialVersionUID = -483687064076707407L;
    private UUID id;

    private String email;

    private String passwordHash;

    private String fullName;

    private String role;

    private String avatarUrl;

    private Instant createdAt;

    private Instant updatedAt;

    private String avatar;

    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    private Set<FlashcardReview> flashcardReviews = new LinkedHashSet<>();

    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    private Set<MessageReaction> messageReactions = new LinkedHashSet<>();

    private Set<NotebookActivityLog> notebookActivityLogs = new LinkedHashSet<>();

    private Set<NotebookFile> notebookFiles = new LinkedHashSet<>();

    private Set<NotebookMember> notebookMembers = new LinkedHashSet<>();

    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    private Set<Notebook> notebooks = new LinkedHashSet<>();

    private Set<QuizSubmission> quizSubmissions = new LinkedHashSet<>();

    private Set<Quiz> quizzes = new LinkedHashSet<>();

    private Set<RagQuery> ragQueries = new LinkedHashSet<>();

    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();

    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    public String getEmail() {
        return email;
    }

    @NotNull
    @Column(name = "password_hash", nullable = false, length = Integer.MAX_VALUE)
    public String getPasswordHash() {
        return passwordHash;
    }

    @Size(max = 255)
    @Column(name = "full_name")
    public String getFullName() {
        return fullName;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "role", nullable = false, length = 50)
    public String getRole() {
        return role;
    }

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @ColumnDefault("now()")
    @Column(name = "created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Column(name = "avatar", length = Integer.MAX_VALUE)
    public String getAvatar() {
        return avatar;
    }

    @OneToMany(mappedBy = "user")
    public Set<AiTask> getAiTasks() {
        return aiTasks;
    }

    @OneToMany(mappedBy = "user")
    public Set<FlashcardReview> getFlashcardReviews() {
        return flashcardReviews;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<Flashcard> getFlashcards() {
        return flashcards;
    }

    @OneToMany(mappedBy = "user")
    public Set<MessageReaction> getMessageReactions() {
        return messageReactions;
    }

    @OneToMany(mappedBy = "user")
    public Set<NotebookActivityLog> getNotebookActivityLogs() {
        return notebookActivityLogs;
    }

    @OneToMany(mappedBy = "uploadedBy")
    public Set<NotebookFile> getNotebookFiles() {
        return notebookFiles;
    }

    @OneToMany(mappedBy = "user")
    public Set<NotebookMember> getNotebookMembers() {
        return notebookMembers;
    }

    @OneToMany(mappedBy = "user")
    public Set<NotebookMessage> getNotebookMessages() {
        return notebookMessages;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<Notebook> getNotebooks() {
        return notebooks;
    }

    @OneToMany(mappedBy = "user")
    public Set<QuizSubmission> getQuizSubmissions() {
        return quizSubmissions;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<Quiz> getQuizzes() {
        return quizzes;
    }

    @OneToMany(mappedBy = "user")
    public Set<RagQuery> getRagQueries() {
        return ragQueries;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<TtsAsset> getTtsAssets() {
        return ttsAssets;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<VideoAsset> getVideoAssets() {
        return videoAssets;
    }

}