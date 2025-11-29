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
@Entity(name = FilePage.ENTITY_NAME)
@Table(name = FilePage.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "uq_file_pages_file_page", columnList = "file_id, page_number", unique = true),
        @Index(name = "idx_file_pages_file_id", columnList = "file_id")
})
public class FilePage implements Serializable {
    public static final String ENTITY_NAME = "File_Page";
    public static final String TABLE_NAME = "file_pages";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_PAGENUMBER_NAME = "page_number";
    public static final String COLUMN_TEXTCONTENT_NAME = "text_content";
    public static final String COLUMN_TOKENCOUNT_NAME = "token_count";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -6846664035903864271L;


    private UUID id;

    private NotebookFile file;

    private Integer pageNumber;

    private String textContent;

    private Integer tokenCount;

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
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @NotNull
    @Column(name = COLUMN_PAGENUMBER_NAME, nullable = false)
    public Integer getPageNumber() {
        return pageNumber;
    }

    @Column(name = COLUMN_TEXTCONTENT_NAME, length = Integer.MAX_VALUE)
    public String getTextContent() {
        return textContent;
    }

    @Column(name = COLUMN_TOKENCOUNT_NAME)
    public Integer getTokenCount() {
        return tokenCount;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}