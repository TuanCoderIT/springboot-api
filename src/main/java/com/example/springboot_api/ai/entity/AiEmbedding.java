package com.example.springboot_api.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "ai_embeddings",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_chunk_embedding", columnNames = "chunk_id")
        },
        indexes = {
            @Index(name = "idx_embeddings_chunk_id", columnList = "chunk_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_embeddings_chunk"))
    private AiChunk chunk;

    // Cần custom type/AttributeConverter để làm việc 100% ngon với pgvector
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private float[] embedding;
}
