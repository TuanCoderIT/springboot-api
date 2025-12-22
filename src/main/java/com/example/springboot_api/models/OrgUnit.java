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
@Entity(name = OrgUnit.ENTITY_NAME)
@Table(name = OrgUnit.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_org_units_parent_id", columnList = "parent_id"),
        @Index(name = "idx_org_units_is_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "org_units_code_key", columnNames = {"code"})
})
public class OrgUnit implements Serializable {
    public static final String ENTITY_NAME = "Org_Unit";
    public static final String TABLE_NAME = "org_units";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CODE_NAME = "code";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_TYPE_NAME = "type";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 6481221999784070342L;


    private UUID id;

    private String code;

    private String name;

    private String type;

    private OrgUnit parent;

    private Boolean isActive = false;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Set<Major> majors = new LinkedHashSet<>();

    private Set<OrgUnit> orgUnits = new LinkedHashSet<>();

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

    @Column(name = COLUMN_TYPE_NAME, length = Integer.MAX_VALUE)
    public String getType() {
        return type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_id")
    public OrgUnit getParent() {
        return parent;
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

    @OneToMany(mappedBy = "orgUnit")
    public Set<Major> getMajors() {
        return majors;
    }

    @OneToMany(mappedBy = "parent")
    public Set<OrgUnit> getOrgUnits() {
        return orgUnits;
    }

    @OneToMany(mappedBy = "primaryOrgUnit")
    public Set<User> getUsers() {
        return users;
    }

}