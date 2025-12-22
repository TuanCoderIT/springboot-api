package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.LocalDate;
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
@Entity(name = Class.ENTITY_NAME)
@Table(name = Class.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_classes_assignment", columnList = "teaching_assignment_id"),
        @Index(name = "idx_classes_subject_code", columnList = "subject_code"),
        @Index(name = "idx_classes_time", columnList = "start_date, end_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_classes_assignment_classcode", columnNames = {"teaching_assignment_id", "class_code"})
})
public class Class implements Serializable {
    public static final String ENTITY_NAME = "Class";
    public static final String TABLE_NAME = "classes";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CLASSCODE_NAME = "class_code";
    public static final String COLUMN_SUBJECTCODE_NAME = "subject_code";
    public static final String COLUMN_SUBJECTNAME_NAME = "subject_name";
    public static final String COLUMN_ROOM_NAME = "room";
    public static final String COLUMN_DAYOFWEEK_NAME = "day_of_week";
    public static final String COLUMN_PERIODS_NAME = "periods";
    public static final String COLUMN_STARTDATE_NAME = "start_date";
    public static final String COLUMN_ENDDATE_NAME = "end_date";
    public static final String COLUMN_NOTE_NAME = "note";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = -4379268833791657814L;


    private UUID id;

    private TeachingAssignment teachingAssignment;

    private String classCode;

    private String subjectCode;

    private String subjectName;

    private String room;

    private Integer dayOfWeek;

    private String periods;

    private LocalDate startDate;

    private LocalDate endDate;

    private String note;

    private Boolean isActive = false;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Set<ClassMember> classMembers = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "teaching_assignment_id", nullable = false)
    public TeachingAssignment getTeachingAssignment() {
        return teachingAssignment;
    }

    @NotNull
    @Column(name = COLUMN_CLASSCODE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getClassCode() {
        return classCode;
    }

    @NotNull
    @Column(name = COLUMN_SUBJECTCODE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getSubjectCode() {
        return subjectCode;
    }

    @NotNull
    @Column(name = COLUMN_SUBJECTNAME_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getSubjectName() {
        return subjectName;
    }

    @Column(name = COLUMN_ROOM_NAME, length = Integer.MAX_VALUE)
    public String getRoom() {
        return room;
    }

    @Column(name = COLUMN_DAYOFWEEK_NAME)
    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    @Column(name = COLUMN_PERIODS_NAME, length = Integer.MAX_VALUE)
    public String getPeriods() {
        return periods;
    }

    @Column(name = COLUMN_STARTDATE_NAME)
    public LocalDate getStartDate() {
        return startDate;
    }

    @Column(name = COLUMN_ENDDATE_NAME)
    public LocalDate getEndDate() {
        return endDate;
    }

    @Column(name = COLUMN_NOTE_NAME, length = Integer.MAX_VALUE)
    public String getNote() {
        return note;
    }

    @NotNull
    @ColumnDefault("true")
    @Column(name = COLUMN_ISACTIVE_NAME, nullable = false)
    public Boolean getIsActive() {
        return isActive;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME, nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @OneToMany
    @JoinColumn(name = "class_id")
    public Set<ClassMember> getClassMembers() {
        return classMembers;
    }

}