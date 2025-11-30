package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = User.ENTITY_NAME)
@Table(name = User.TABLE_NAME)
public class User implements Serializable {
    public static final String ENTITY_NAME = "User";
    public static final String TABLE_NAME = "users";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_EMAIL_NAME = "email";
    public static final String COLUMN_PASSWORDHASH_NAME = "password_hash";
    public static final String COLUMN_FULLNAME_NAME = "full_name";
    public static final String COLUMN_ROLE_NAME = "role";
    public static final String COLUMN_AVATARURL_NAME = "avatar_url";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    public static final String COLUMN_AVATAR_NAME = "avatar";
    private static final long serialVersionUID = 4410069988865527919L;


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
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @Size(max = 255)
    @NotNull
    @Column(name = COLUMN_EMAIL_NAME, nullable = false)
    public String getEmail() {
        return email;
    }

    @NotNull
    @Column(name = COLUMN_PASSWORDHASH_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getPasswordHash() {
        return passwordHash;
    }

    @Size(max = 255)
    @Column(name = COLUMN_FULLNAME_NAME)
    public String getFullName() {
        return fullName;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_ROLE_NAME, nullable = false, length = 50)
    public String getRole() {
        return role;
    }

    @Column(name = COLUMN_AVATARURL_NAME, length = Integer.MAX_VALUE)
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME)
    public Instant getCreatedAt() {
        return createdAt;
    }

    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME)
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Column(name = COLUMN_AVATAR_NAME, length = Integer.MAX_VALUE)
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