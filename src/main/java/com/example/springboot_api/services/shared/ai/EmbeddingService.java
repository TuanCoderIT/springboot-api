package com.example.springboot_api.services.shared.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    // 🟢 Dùng TÊN LỚP ĐẦY ĐỦ (FQCN) để đảm bảo tiêm đúng Bean
    private final com.google.genai.Client geminiClient;

    // Cấu hình mô hình và kích thước vector mục tiêu
    private static final String EMBED_MODEL = "gemini-embedding-001";
    private static final int TARGET_DIMENSION = 1536;

    /**
     * Sinh vector embedding 3072 chiều, sau đó chuẩn hóa L2 và cắt/trích về 1536
     * chiều.
     */
    public List<Double> embedGoogleNormalized(String text) {
        if (text == null || text.isEmpty() || text.trim().isEmpty()) {
            return createZeroVector(TARGET_DIMENSION);
        }

        try {
            EmbedContentConfig config = EmbedContentConfig.builder()
                    .taskType("retrieval_document")
                    .build();

            // 1. GỌI API: SỬA LỖI CÚ PHÁP -> gọi qua FIELD 'models'
            EmbedContentResponse response = geminiClient.models.embedContent(
                    EMBED_MODEL,
                    text.trim(),
                    config);

            // 2. Unwrap Optional<List<ContentEmbedding>>
            List<ContentEmbedding> embeddingList = response.embeddings()
                    .orElseThrow(() -> new NoSuchElementException("API response did not contain embedding list."));

            if (embeddingList.isEmpty()) {
                throw new RuntimeException("Danh sách embedding rỗng.");
            }

            // 3. Unwrap Optional<List<Float>>
            List<Float> originalVector = embeddingList.get(0).values()
                    .orElseThrow(() -> new NoSuchElementException("Embedding vector values are missing."));

            // 4. Chuẩn hóa và Cắt/Trích
            return normalizeAndTruncate(originalVector);

        } catch (NoSuchElementException e) {
            System.err.println("❌ Lỗi cấu trúc phản hồi API: Dữ liệu embedding bị thiếu. Chi tiết: " + e.getMessage());
            e.printStackTrace();
            return createZeroVector(TARGET_DIMENSION);
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi embedding tại EmbeddingService: " + e.getMessage());
            System.err.println("❌ Exception type: " + e.getClass().getName());
            e.printStackTrace();
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    private List<Double> normalizeAndTruncate(List<Float> vector3072) {
        if (vector3072.isEmpty()) {
            return createZeroVector(TARGET_DIMENSION);
        }

        double norm = 0.0;
        for (float component : vector3072) {
            norm += (double) component * (double) component;
        }
        norm = Math.sqrt(norm);

        List<Double> resultVector = new ArrayList<>(TARGET_DIMENSION);

        if (norm == 0.0) {
            return createZeroVector(TARGET_DIMENSION);
        }

        int dimensionToUse = Math.min(vector3072.size(), TARGET_DIMENSION);
        for (int i = 0; i < dimensionToUse; i++) {
            double normalizedComponent = vector3072.get(i) / norm;
            resultVector.add(normalizedComponent);
        }

        while (resultVector.size() < TARGET_DIMENSION) {
            resultVector.add(0.0);
        }

        return resultVector;
    }

    private List<Double> createZeroVector(int dimension) {
        List<Double> zeroVector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            zeroVector.add(0.0);
        }
        return zeroVector;
    }
}