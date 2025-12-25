package com.example.springboot_api.services.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.models.FileChunk;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileProcessingTaskService {

    private final OcrService ocrService;
    private final EmbeddingService embeddingService;
    private final YoutubeSubtitleService youtubeSubtitleService;
    private final NotebookFileRepository fileRepository;
    private final FileChunkRepository fileChunkRepository;

    /**
     * Xử lý file (PDF, Word, PPT): OCR → chunk → embedding.
     */
    @Async
    @Transactional
    public void startAIProcessing(NotebookFile file) {
        System.out.println("🔥 RUNNING AI THREAD: " + Thread.currentThread().getName());

        UUID fileId = file.getId();
        System.out.println("=== START AI PROCESSING: " + fileId);

        NotebookFile loadedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File không tồn tại: " + fileId));

        loadedFile.setStatus("processing");
        fileRepository.save(loadedFile);

        try {
            System.out.println("📄 Bắt đầu OCR...");
            String text = ocrService.extract(loadedFile.getStorageUrl());
            System.out.println("✅ OCR hoàn thành, độ dài text: " + (text != null ? text.length() : 0));
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("OCR không đọc được nội dung.");
            }

            processChunksAndEmbeddings(loadedFile, text);

            loadedFile.setOcrDone(true);
            loadedFile.setEmbeddingDone(true);
            loadedFile.setStatus("done");

        } catch (Exception e) {
            loadedFile.setStatus("failed");
            System.err.println("LỖI AI PROCESS: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loadedFile.setUpdatedAt(OffsetDateTime.now());
            fileRepository.save(loadedFile);
            System.out.println("=== END AI PROCESSING: " + fileId + " | status=" + loadedFile.getStatus());
        }
    }

    /**
     * Xử lý video YouTube: trích xuất phụ đề → chunk → embedding (tất cả async).
     * 
     * @param file       NotebookFile đã được lưu (với mimeType = video/youtube)
     * @param youtubeUrl URL video YouTube để trích xuất phụ đề
     */
    @Async
    @Transactional
    public void startYoutubeProcessing(NotebookFile file, String youtubeUrl) {
        System.out.println("🎬 RUNNING YOUTUBE THREAD: " + Thread.currentThread().getName());

        UUID fileId = file.getId();
        System.out.println("=== START YOUTUBE PROCESSING: " + fileId);

        NotebookFile loadedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File không tồn tại: " + fileId));

        loadedFile.setStatus("processing");
        fileRepository.save(loadedFile);

        try {
            // 1. Trích xuất phụ đề (async - không block API)
            System.out.println("📥 Đang trích xuất phụ đề từ: " + youtubeUrl);
            YoutubeSubtitleService.SubtitleResult subtitleResult = youtubeSubtitleService
                    .extractSubtitleWithTimestamps(youtubeUrl);
            String subtitleText = subtitleResult.fullText();

            if (subtitleText == null || subtitleText.isBlank()) {
                System.out.println("⚠️ Subtitle trống, bỏ qua embedding.");
                loadedFile.setEmbeddingDone(true);
                loadedFile.setStatus("done");
            } else {
                System.out.println("📝 Subtitle extracted, length: " + subtitleText.length());
                // 2. Chunk + Embedding
                processChunksAndEmbeddings(loadedFile, subtitleText);
                loadedFile.setEmbeddingDone(true);
                loadedFile.setStatus("done");
            }

        } catch (Exception e) {
            loadedFile.setStatus("failed");
            System.err.println("LỖI YOUTUBE PROCESS: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loadedFile.setUpdatedAt(OffsetDateTime.now());
            fileRepository.save(loadedFile);
            System.out.println("=== END YOUTUBE PROCESSING: " + fileId + " | status=" + loadedFile.getStatus());
        }
    }

    /**
     * Logic chung: chunk text → embedding → lưu FileChunk.
     */
    private void processChunksAndEmbeddings(NotebookFile loadedFile, String text) {
        UUID fileId = loadedFile.getId();
        int chunkSize = loadedFile.getChunkSize() != null ? loadedFile.getChunkSize() : 2000;
        int chunkOverlap = loadedFile.getChunkOverlap() != null ? loadedFile.getChunkOverlap() : 200;

        List<String> chunks = splitTextIntoChunks(text, chunkSize, chunkOverlap);
        System.out.println("📦 Số lượng chunks: " + chunks.size());

        fileChunkRepository.deleteByFileId(fileId);

        Notebook notebook = loadedFile.getNotebook();
        int index = 0;

        for (String chunk : chunks) {
            System.out.println("🔄 Embedding chunk " + (index + 1) + "/" + chunks.size() + "...");
            try {
                List<Double> vector = embeddingService.embedGoogleNormalized(chunk);

                if (vector == null || vector.isEmpty() || vector.size() != 1536) {
                    String errorMsg = vector == null ? "null" : String.valueOf(vector.size());
                    throw new RuntimeException("Embedding invalid: size=" + errorMsg);
                }

                FileChunk fc = FileChunk.builder()
                        .notebook(notebook)
                        .file(loadedFile)
                        .chunkIndex(index++)
                        .content(chunk)
                        .embedding(vector)
                        .createdAt(OffsetDateTime.now())
                        .build();

                fileChunkRepository.save(fc);
                System.out.println("✅ Đã lưu chunk " + index);
            } catch (Exception e) {
                System.err.println("❌ LỖI Ở CHUNK " + (index + 1) + ": " + e.getMessage());
                throw e;
            }
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