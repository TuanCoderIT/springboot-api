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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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


}