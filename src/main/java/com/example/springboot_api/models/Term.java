package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;

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
@Entity(name = Term.ENTITY_NAME)
@Table(name = Term.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_terms_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "terms_code_key", columnNames = {"code"})
})
public class Term implements Serializable {
    public static final String ENTITY_NAME = "Term";
    public static final String TABLE_NAME = "terms";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CODE_NAME = "code";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_STARTDATE_NAME = "start_date";
    public static final String COLUMN_ENDDATE_NAME = "end_date";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 3769753216548055286L;


    private UUID id;

    private String code;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isActive = false;

    private OffsetDateTime createdAt;

    private Set<TeachingAssignment> teachingAssignments = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @Column(name = COLUMN_CODE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getCode() {
        return code;
    }

    @NotNull
    @Column(name = COLUMN_NAME_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getName() {
        return name;
    }

    @Column(name = COLUMN_STARTDATE_NAME)
    public LocalDate getStartDate() {
        return startDate;
    }

    @Column(name = COLUMN_ENDDATE_NAME)
    public LocalDate getEndDate() {
        return endDate;
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

    @OneToMany(mappedBy = "term")
    public Set<TeachingAssignment> getTeachingAssignments() {
        return teachingAssignments;
    }

}