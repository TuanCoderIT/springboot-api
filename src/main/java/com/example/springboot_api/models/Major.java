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
@Entity(name = Major.ENTITY_NAME)
@Table(name = Major.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_majors_org_unit", columnList = "org_unit_id"),
        @Index(name = "idx_majors_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "majors_code_key", columnNames = {"code"})
})
public class Major implements Serializable {
    public static final String ENTITY_NAME = "Major";
    public static final String TABLE_NAME = "majors";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CODE_NAME = "code";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = -5522382482616774402L;


    private UUID id;

    private String code;

    private String name;

    private OrgUnit orgUnit;

    private Boolean isActive = false;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Set<MajorSubject> majorSubjects = new LinkedHashSet<>();

    private Set<User> users = new LinkedHashSet<>();

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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "org_unit_id")
    public OrgUnit getOrgUnit() {
        return orgUnit;
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

    @OneToMany(mappedBy = "major")
    public Set<MajorSubject> getMajorSubjects() {
        return majorSubjects;
    }

    @OneToMany(mappedBy = "major")
    public Set<User> getUsers() {
        return users;
    }

}