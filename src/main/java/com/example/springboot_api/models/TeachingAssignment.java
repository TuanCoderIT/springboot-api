package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
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
@Entity(name = TeachingAssignment.ENTITY_NAME)
@Table(name = TeachingAssignment.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_teaching_assignments_term", columnList = "term_id"),
        @Index(name = "idx_teaching_assignments_subject", columnList = "subject_id")
})
public class TeachingAssignment implements Serializable {
    public static final String ENTITY_NAME = "Teaching_Assignment";
    public static final String TABLE_NAME = "teaching_assignments";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_CREATEDBY_NAME = "created_by";
    public static final String COLUMN_APPROVALSTATUS_NAME = "approval_status";
    public static final String COLUMN_APPROVEDBY_NAME = "approved_by";
    public static final String COLUMN_APPROVEDAT_NAME = "approved_at";
    public static final String COLUMN_NOTE_NAME = "note";
    private static final long serialVersionUID = 937029900511364439L;


    private UUID id;

    private Term term;

    private Subject subject;

    private String status;

    private OffsetDateTime createdAt;

    private String createdBy;

    private String approvalStatus;

    private UUID approvedBy;

    private OffsetDateTime approvedAt;

    private String note;

    private User lecturer;

    private Notebook notebook;

    private Set<Class> classes = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "term_id", nullable = false)
    public Term getTerm() {
        return term;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "subject_id", nullable = false)
    public Subject getSubject() {
        return subject;
    }

    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = COLUMN_STATUS_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getStatus() {
        return status;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("'ADMIN'")
    @Column(name = COLUMN_CREATEDBY_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @ColumnDefault("'APPROVED'")
    @Column(name = COLUMN_APPROVALSTATUS_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getApprovalStatus() {
        return approvalStatus;
    }

    @Column(name = COLUMN_APPROVEDBY_NAME)
    public UUID getApprovedBy() {
        return approvedBy;
    }

    @Column(name = COLUMN_APPROVEDAT_NAME)
    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    @Column(name = COLUMN_NOTE_NAME, length = Integer.MAX_VALUE)
    public String getNote() {
        return note;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "lecturer_id")
    public User getLecturer() {
        return lecturer;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id")
    public Notebook getNotebook() {
        return notebook;
    }

    @OneToMany(mappedBy = "teachingAssignment")
    public Set<Class> getClasses() {
        return classes;
    }

}