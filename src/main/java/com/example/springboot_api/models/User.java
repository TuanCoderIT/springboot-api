package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = User.ENTITY_NAME)
@Table(name = User.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "ux_users_student_code", columnList = "student_code", unique = true),
        @Index(name = "ux_users_lecturer_code", columnList = "lecturer_code", unique = true),
        @Index(name = "idx_users_email", columnList = "email")
}, uniqueConstraints = {
        @UniqueConstraint(name = "users_email_key", columnNames = {"email"})
})
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
    public static final String COLUMN_STUDENTCODE_NAME = "student_code";
    public static final String COLUMN_COHORTYEAR_NAME = "cohort_year";
    public static final String COLUMN_PROGRAM_NAME = "program";
    public static final String COLUMN_CLASSCODE_NAME = "class_code";
    public static final String COLUMN_LECTURERCODE_NAME = "lecturer_code";
    public static final String COLUMN_ACADEMICDEGREE_NAME = "academic_degree";
    public static final String COLUMN_ACADEMICRANK_NAME = "academic_rank";
    public static final String COLUMN_SPECIALIZATION_NAME = "specialization";
    private static final long serialVersionUID = -668003510068088095L;


    private UUID id;

    private String email;

    private String passwordHash;

    private String fullName;

    private String role;

    private String avatarUrl;

    private Instant createdAt;

    private Instant updatedAt;

    private String avatar;

    private String studentCode;

    private Integer cohortYear;

    private String program;

    private String classCode;

    private Major major;

    private String lecturerCode;

    private OrgUnit primaryOrgUnit;

    private String academicDegree;

    private String academicRank;

    private String specialization;

    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    private Set<MessageReaction> messageReactions = new LinkedHashSet<>();

    private Set<NotebookActivityLog> notebookActivityLogs = new LinkedHashSet<>();

    private Set<NotebookAiSetSuggestion> notebookAiSetSuggestions = new LinkedHashSet<>();

    private Set<NotebookAiSet> notebookAiSets = new LinkedHashSet<>();

    private Set<NotebookAiSummary> notebookAiSummaries = new LinkedHashSet<>();

    private Set<NotebookBotConversationState> notebookBotConversationStates = new LinkedHashSet<>();

    private Set<NotebookBotConversation> notebookBotConversations = new LinkedHashSet<>();

    private Set<NotebookBotMessage> notebookBotMessages = new LinkedHashSet<>();

    private Set<NotebookFile> notebookFiles = new LinkedHashSet<>();

    private Set<NotebookMember> notebookMembers = new LinkedHashSet<>();

    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    private Set<NotebookMindmap> notebookMindmaps = new LinkedHashSet<>();

    private Set<NotebookQuizz> notebookQuizzes = new LinkedHashSet<>();

    private Set<Notebook> notebooks = new LinkedHashSet<>();

    private Set<Notification> notifications = new LinkedHashSet<>();

    private Set<RegulationChatAnalytic> regulationChatAnalytics = new LinkedHashSet<>();

    private Set<TeachingAssignment> teachingAssignments = new LinkedHashSet<>();

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

    @Size(max = 50)
    @Column(name = COLUMN_STUDENTCODE_NAME, length = 50)
    public String getStudentCode() {
        return studentCode;
    }

    @Column(name = COLUMN_COHORTYEAR_NAME)
    public Integer getCohortYear() {
        return cohortYear;
    }

    @Size(max = 255)
    @Column(name = COLUMN_PROGRAM_NAME)
    public String getProgram() {
        return program;
    }

    @Size(max = 50)
    @Column(name = COLUMN_CLASSCODE_NAME, length = 50)
    public String getClassCode() {
        return classCode;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    public Major getMajor() {
        return major;
    }

    @Size(max = 50)
    @Column(name = COLUMN_LECTURERCODE_NAME, length = 50)
    public String getLecturerCode() {
        return lecturerCode;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "primary_org_unit_id")
    public OrgUnit getPrimaryOrgUnit() {
        return primaryOrgUnit;
    }

    @Size(max = 255)
    @Column(name = COLUMN_ACADEMICDEGREE_NAME)
    public String getAcademicDegree() {
        return academicDegree;
    }

    @Size(max = 255)
    @Column(name = COLUMN_ACADEMICRANK_NAME)
    public String getAcademicRank() {
        return academicRank;
    }

    @Size(max = 255)
    @Column(name = COLUMN_SPECIALIZATION_NAME)
    public String getSpecialization() {
        return specialization;
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

    @OneToMany(mappedBy = "createdBy")
    public Set<NotebookAiSetSuggestion> getNotebookAiSetSuggestions() {
        return notebookAiSetSuggestions;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<NotebookAiSet> getNotebookAiSets() {
        return notebookAiSets;
    }

    @OneToMany
    @JoinColumn(name = "create_by")
    public Set<NotebookAiSummary> getNotebookAiSummaries() {
        return notebookAiSummaries;
    }

    @OneToMany(mappedBy = "user")
    public Set<NotebookBotConversationState> getNotebookBotConversationStates() {
        return notebookBotConversationStates;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<NotebookBotConversation> getNotebookBotConversations() {
        return notebookBotConversations;
    }

    @OneToMany(mappedBy = "user")
    public Set<NotebookBotMessage> getNotebookBotMessages() {
        return notebookBotMessages;
    }

    @OneToMany
    @JoinColumn(name = "uploaded_by")
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
    public Set<NotebookMindmap> getNotebookMindmaps() {
        return notebookMindmaps;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<NotebookQuizz> getNotebookQuizzes() {
        return notebookQuizzes;
    }

    @OneToMany(mappedBy = "createdBy")
    public Set<Notebook> getNotebooks() {
        return notebooks;
    }

    @OneToMany(mappedBy = "user")
    public Set<Notification> getNotifications() {
        return notifications;
    }

    @OneToMany(mappedBy = "user")
    public Set<RegulationChatAnalytic> getRegulationChatAnalytics() {
        return regulationChatAnalytics;
    }

    @OneToMany
    @JoinColumn(name = "lecturer_id")
    public Set<TeachingAssignment> getTeachingAssignments() {
        return teachingAssignments;
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