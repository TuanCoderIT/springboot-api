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
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = MajorSubject.ENTITY_NAME)
@Table(name = MajorSubject.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_major_subjects_term", columnList = "major_id, term_no"),
        @Index(name = "idx_major_subjects_major", columnList = "major_id"),
        @Index(name = "idx_major_subjects_subject", columnList = "subject_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_major_subject", columnNames = {"major_id", "subject_id"})
})
public class MajorSubject implements Serializable {
    public static final String ENTITY_NAME = "Major_Subject";
    public static final String TABLE_NAME = "major_subjects";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_KNOWLEDGEBLOCK_NAME = "knowledge_block";
    public static final String COLUMN_TERMNO_NAME = "term_no";
    public static final String COLUMN_ISREQUIRED_NAME = "is_required";
    public static final String COLUMN_PERIODSPLIT_NAME = "period_split";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -3030409997031513248L;


    private UUID id;

    private Major major;

    private Subject subject;

    private String knowledgeBlock;

    private Integer termNo;

    private Boolean isRequired = false;

    private String periodSplit;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "major_id", nullable = false)
    public Major getMajor() {
        return major;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "subject_id", nullable = false)
    public Subject getSubject() {
        return subject;
    }

    @Column(name = COLUMN_KNOWLEDGEBLOCK_NAME, length = Integer.MAX_VALUE)
    public String getKnowledgeBlock() {
        return knowledgeBlock;
    }

    @Column(name = COLUMN_TERMNO_NAME)
    public Integer getTermNo() {
        return termNo;
    }

    @NotNull
    @ColumnDefault("true")
    @Column(name = COLUMN_ISREQUIRED_NAME, nullable = false)
    public Boolean getIsRequired() {
        return isRequired;
    }

    @Column(name = COLUMN_PERIODSPLIT_NAME, length = Integer.MAX_VALUE)
    public String getPeriodSplit() {
        return periodSplit;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}