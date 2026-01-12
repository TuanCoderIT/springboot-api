package com.example.springboot_api.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Ngôn ngữ lập trình được Piston hỗ trợ.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = "SupportedLanguage")
@Table(name = "supported_languages", schema = "public")
public class SupportedLanguage {

    public static final String ENTITY_NAME = "SupportedLanguage";
    public static final String TABLE_NAME = "supported_languages";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    @ToString.Include
    private String name;

    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "aliases", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> aliases;

    @Column(name = "runtime", length = 50)
    private String runtime;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;
}
