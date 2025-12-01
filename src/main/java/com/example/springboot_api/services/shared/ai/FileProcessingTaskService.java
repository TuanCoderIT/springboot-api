package com.example.springboot_api.services.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.springboot_api.models.FileChunk;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileProcessingTaskService {

    private final OcrService ocrService;
    private final EmbeddingService embeddingService;
    private final NotebookFileRepository fileRepository;
    private final FileChunkRepository fileChunkRepository;

    @Async
    public void startAIProcessing(NotebookFile file) {
        System.out.println("=== START AI PROCESSING: " + file.getId());

        file.setStatus("processing");
        fileRepository.save(file);

        try {
            // 1. OCR
            String text = ocrService.extractTextFromDocument(file.getStorageUrl(), file.getMimeType());
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("OCR không đọc được nội dung.");
            }

            int chunkSize = file.getChunkSize() != null ? file.getChunkSize() : 800;
            int chunkOverlap = file.getChunkOverlap() != null ? file.getChunkOverlap() : 120;

            // 2. Chunking thật sự
            List<String> chunks = splitTextIntoChunks(text, chunkSize, chunkOverlap);

            // Xóa chunk cũ nếu user re-upload
            fileChunkRepository.deleteByFileId(file.getId());

            int index = 0;

            // 3. Embedding từng chunk → lưu DB
            for (String chunk : chunks) {
                List<Double> vector = embeddingService.embedGoogleNormalized(chunk);

                if (vector == null || vector.isEmpty() || vector.size() != 1536) {
                    String errorMsg = vector == null ? "null" : String.valueOf(vector.size());
                    throw new RuntimeException("Embedding invalid: size=" + errorMsg);
                }

                FileChunk fc = FileChunk.builder()
                        .notebook(file.getNotebook())
                        .file(file)
                        .chunkIndex(index++)
                        .content(chunk)
                        .embedding(vector)
                        .createdAt(OffsetDateTime.now())
                        .build();

                fileChunkRepository.save(fc);
            }

            // 4. Update status
            file.setOcrDone(true);
            file.setEmbeddingDone(true);
            file.setStatus("done");

        } catch (Exception e) {
            file.setStatus("failed");
            System.err.println("LỖI AI PROCESS: " + e.getMessage());
        } finally {
            file.setUpdatedAt(OffsetDateTime.now());
            fileRepository.save(file);
            System.out.println("=== END AI PROCESSING: " + file.getId() + " | status=" + file.getStatus());
        }
    }

    // Hàm chunk text
    private List<String> splitTextIntoChunks(String text, int size, int overlap) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            list.add(text.substring(start, end));
            start += size - overlap;
        }

        return list;
    }
}
