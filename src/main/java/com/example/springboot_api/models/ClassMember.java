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
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = ClassMember.ENTITY_NAME)
@Table(name = ClassMember.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_class_members_class", columnList = "class_id"),
        @Index(name = "idx_class_members_student_code", columnList = "student_code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_class_student", columnNames = {"class_id", "student_code"})
})
public class ClassMember implements Serializable {
    public static final String ENTITY_NAME = "Class_Member";
    public static final String TABLE_NAME = "class_members";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_STUDENTCODE_NAME = "student_code";
    public static final String COLUMN_FIRSTNAME_NAME = "first_name";
    public static final String COLUMN_LASTNAME_NAME = "last_name";
    public static final String COLUMN_FULLNAME_NAME = "full_name";
    public static final String COLUMN_DOB_NAME = "dob";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 1800741546805298054L;


    private UUID id;

    private Class classField;

    private String studentCode;

    private String firstName;

    private String lastName;

    private String fullName;

    private LocalDate dob;

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
    @JoinColumn(name = "class_id", nullable = false)
    public Class getClassField() {
        return classField;
    }

    @NotNull
    @Column(name = COLUMN_STUDENTCODE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getStudentCode() {
        return studentCode;
    }

    @Column(name = COLUMN_FIRSTNAME_NAME, length = Integer.MAX_VALUE)
    public String getFirstName() {
        return firstName;
    }

    @Column(name = COLUMN_LASTNAME_NAME, length = Integer.MAX_VALUE)
    public String getLastName() {
        return lastName;
    }

    @NotNull
    @Column(name = COLUMN_FULLNAME_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getFullName() {
        return fullName;
    }

    @Column(name = COLUMN_DOB_NAME)
    public LocalDate getDob() {
        return dob;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}