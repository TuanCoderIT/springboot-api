package com.example.springboot_api.services.shared.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.repositories.shared.FileChunkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý tóm tắt tài liệu.
 * Dùng chung cho tất cả AI generation (Quiz, Flashcard, Summary, Mindmap, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSummarizationService {

    private final FileChunkRepository fileChunkRepository;
    private final AIModelService aiModelService;

    /**
     * Tóm tắt nội dung từ nhiều files.
     * 
     * @param files    Danh sách NotebookFile cần tóm tắt
     * @param llmModel Optional: chọn model, có thể null để dùng default
     * @return String tóm tắt tổng hợp từ tất cả files
     */
    public String summarizeDocuments(List<NotebookFile> files, LlmModel llmModel) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        int maxFiles = 10;
        int maxCharsTotal = 50000;

        StringBuilder fullTextBuilder = new StringBuilder();
        int totalChars = 0;

        int limitFiles = Math.min(files.size(), maxFiles);

        for (int i = 0; i < limitFiles; i++) {
            NotebookFile file = files.get(i);
            String fileSummary = summarizeSingleFile(file, llmModel);

            if (fileSummary != null && !fileSummary.isEmpty()) {
                int remaining = maxCharsTotal - totalChars;
                if (remaining <= 0)
                    break;

                if (fileSummary.length() > remaining) {
                    fileSummary = fileSummary.substring(0, remaining);
                }

                fullTextBuilder.append("\n\n--- FILE: ")
                        .append(file.getOriginalFilename())
                        .append(" ---\n");

                fullTextBuilder.append(fileSummary);
                totalChars += fileSummary.length();
            }

            // Né rate limit Gemini khi xử lý file tiếp theo
            if (i < limitFiles - 1) {
                try {
                    log.info("⏳ Chờ 10s trước file tiếp theo...");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return fullTextBuilder.toString().trim();
    }

    /**
     * Tóm tắt nội dung của một file.
     */
    private String summarizeSingleFile(NotebookFile file, LlmModel llmModel) {
        int maxChunks = 8;
        int maxCharsPerFile = 12000;
        int summaryThreshold = 4000;

        List<Object[]> chunkData = fileChunkRepository.findByFileIdWithLimit(file.getId(), maxChunks);
        if (chunkData == null || chunkData.isEmpty()) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        int charCount = 0;

        for (Object[] row : chunkData) {
            if (charCount >= maxCharsPerFile)
                break;

            String content = (String) row[1];
            if (content != null && !content.isEmpty()) {
                int remaining = maxCharsPerFile - charCount;
                if (content.length() > remaining) {
                    content = content.substring(0, remaining);
                }
                textBuilder.append(content).append("\n");
                charCount += content.length();
            }
        }

        String fullText = textBuilder.toString().trim();
        if (fullText.isEmpty()) {
            return "";
        }

        // Nếu file quá dài → tóm tắt theo chunk
        if (fullText.length() > summaryThreshold) {
            return summarizeLongText(fullText, llmModel);
        }

        return fullText;
    }

    /**
     * Chia text dài thành chunks và tóm tắt từng phần bằng LLM.
     */
    private String summarizeLongText(String fullText, LlmModel llmModel) {
        int chunkSize = 3000;
        int overlap = 200;

        List<String> chunks = splitTextIntoChunks(fullText, chunkSize, overlap);
        StringBuilder summaryBuilder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkSummary = summarizeChunk(chunk, i, chunks.size(), fullText.length(), llmModel);

            if (chunkSummary != null && !chunkSummary.isEmpty()) {
                summaryBuilder.append(chunkSummary).append("\n");
            }

            // Né rate-limit Google GEMINI FREE
            if (i < chunks.size() - 1) {
                try {
                    log.info("⏳ Chờ 10s để né rate limit Gemini...");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return summaryBuilder.toString().trim();
    }

    /**
     * Chia text thành chunks với overlap.
     */
    private List<String> splitTextIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));

            start = end - overlap;
            if (start >= text.length())
                break;
            if (end == text.length())
                break;
        }

        return chunks;
    }

    /**
     * Tóm tắt một chunk bằng LLM.
     */
    private String summarizeChunk(String chunk, int chunkIndex, int totalChunks, int originalLength,
            LlmModel llmModel) {
        try {
            String prompt = String.format("""
                     Tóm tắt đoạn văn bản sau (phần %d/%d của văn bản gốc %d ký tự):

                     ---
                     %s
                     ---

                    Yêu cầu:
                     - Chỉ giữ các ý quan trọng nhất.
                     - Viết súc tích, rõ ràng, không lan man.
                     - Không nhắc lại "phần x/y", không thêm lời dẫn, không mở đầu hay kết thúc.
                     - Trả về đúng phần tóm tắt, không thêm bất kỳ câu nào ngoài nội dung.
                     """, chunkIndex + 1, totalChunks, originalLength, chunk);

            String response = aiModelService.callGeminiModel(prompt);
            return response != null ? response.trim() : "";
        } catch (Exception e) {
            log.error("❌ Lỗi tóm tắt chunk: {}", e.getMessage());
            // Fallback: trả về chunk gốc đã cắt ngắn
            return chunk.length() > 500 ? chunk.substring(0, 500) + "..." : chunk;
        }
    }
}
