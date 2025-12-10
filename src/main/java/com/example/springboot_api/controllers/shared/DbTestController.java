package com.example.springboot_api.controllers.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.shared.ai.AIModelTestResponse;
import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.repositories.shared.LlmModelRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiAsyncTaskService;
import com.example.springboot_api.services.shared.ai.EmbeddingService;
import com.example.springboot_api.services.shared.ai.OcrGoogleService;
import com.example.springboot_api.services.shared.ai.OcrService;
import com.example.springboot_api.services.shared.ai.WebSearchService;
import com.example.springboot_api.services.user.AiGenerationService;
import com.example.springboot_api.services.user.ChatBotService;

@RestController
@RequestMapping("/test")
public class DbTestController {

    private final EmbeddingService embeddingService;
    private final OcrGoogleService ocrService;
    private final OcrService localOcrService;
    private final WebSearchService webSearchService;
    private final AIModelService aiModelService;
    private final ChatBotService chatBotService;
    private final AiGenerationService aiGenerationService;
    private final AiAsyncTaskService aiAsyncTaskService;
    private final NotebookFileRepository notebookFileRepository;
    private final LlmModelRepository llmModelRepository;

    public DbTestController(
            EmbeddingService embeddingService,
            OcrGoogleService ocrService,
            OcrService localOcrService,
            WebSearchService webSearchService,
            AIModelService aiModelService,
            ChatBotService chatBotService,
            AiGenerationService aiGenerationService,
            AiAsyncTaskService aiAsyncTaskService,
            NotebookFileRepository notebookFileRepository,
            LlmModelRepository llmModelRepository) {
        this.embeddingService = embeddingService;
        this.ocrService = ocrService;
        this.localOcrService = localOcrService;
        this.webSearchService = webSearchService;
        this.aiModelService = aiModelService;
        this.chatBotService = chatBotService;
        this.aiGenerationService = aiGenerationService;
        this.aiAsyncTaskService = aiAsyncTaskService;
        this.notebookFileRepository = notebookFileRepository;
        this.llmModelRepository = llmModelRepository;
    }

    // ... (các router /test/db và /test/embedding không đổi)

    // 3. ROUTER TEST OCR: /test/ocr
    @GetMapping("/ocr")
    public String testOcrService() {
        try {
            String sampleFilePath = "uploads/BÀI THỰC HÀNH 6.docx";

            // 🟢 SỬA LỖI 1: MIME Type chuẩn cho DOCX
            String extractedText = ocrService.extractTextFromDocument(sampleFilePath,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            String cleanedText = extractedText.replaceAll("[\r\n]", " ");

            // 🟢 SỬA LỖI 2: Logic cắt chuỗi an toàn
            int maxLength = 1500;
            String preview;

            if (cleanedText.length() > maxLength) {
                preview = cleanedText.substring(0, maxLength) + "...";
            } else {
                preview = cleanedText;
            }

            return "OCR OK ✅. Trích xuất: '" + preview + "'";
        } catch (Exception e) {
            return "OCR LỖI RUNTIME ❌: " + e.getMessage();
        }
    }

    // 4. ROUTER TEST OCR VỚI DELAY: /test/ocr-delay
    @GetMapping("/ocr-delay")
    public String testOcrWithDelay() {
        try {
            // Delay trước khi gọi API
            System.out.println("⏳ Đợi 150ms trước khi gọi OCR...");
            Thread.sleep(150);

            // Test với file PDF (có thể thay đổi đường dẫn)
            String sampleFilePath = "uploads/020d1b1c-6348-461e-b75b-1be1404aa35e.pdf";

            System.out.println("📄 Bắt đầu OCR cho file: " + sampleFilePath);
            long startTime = System.currentTimeMillis();

            String extractedText = ocrService.extractTextFromDocument(sampleFilePath, "application/pdf");

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String cleanedText = extractedText.replaceAll("[\r\n]", " ");

            int maxLength = 500;
            String preview;
            if (cleanedText.length() > maxLength) {
                preview = cleanedText.substring(0, maxLength) + "...";
            } else {
                preview = cleanedText;
            }

            return String.format(
                    "OCR OK ✅\n" +
                            "Thời gian: %dms\n" +
                            "Độ dài text: %d ký tự\n" +
                            "Preview: '%s'",
                    duration, cleanedText.length(), preview);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "OCR LỖI: Thread bị interrupt ❌";
        } catch (Exception e) {
            return "OCR LỖI RUNTIME ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName();
        }
    }

    // 5. ROUTER TEST OCR VỚI FILE TỪ PARAM: /test/ocr-file?path=...
    @GetMapping("/ocr-file")
    public String testOcrWithFile(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/020d1b1c-6348-461e-b75b-1be1404aa35e.pdf") String path,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "application/pdf") String mimeType) {
        try {
            System.out.println("⏳ Đợi 150ms trước khi gọi OCR...");
            Thread.sleep(150);

            System.out.println("📄 Bắt đầu OCR cho file: " + path);
            System.out.println("📄 MIME Type: " + mimeType);
            long startTime = System.currentTimeMillis();

            String extractedText = ocrService.extractTextFromDocument(path, mimeType);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String cleanedText = extractedText.replaceAll("[\r\n]", " ");

            int maxLength = 500;
            String preview;
            if (cleanedText.length() > maxLength) {
                preview = cleanedText.substring(0, maxLength) + "...";
            } else {
                preview = cleanedText;
            }

            return String.format(
                    "OCR OK ✅\n" +
                            "File: %s\n" +
                            "MIME Type: %s\n" +
                            "Thời gian: %dms\n" +
                            "Độ dài text: %d ký tự\n" +
                            "Preview: '%s'",
                    path, mimeType, duration, cleanedText.length(), preview);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "OCR LỖI: Thread bị interrupt ❌";
        } catch (Exception e) {
            return "OCR LỖI RUNTIME ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    // 6. ROUTER TEST EMBEDDING: /test/embedding
    @GetMapping("/embedding")
    public String testEmbeddingService(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "Đây là một đoạn văn bản mẫu để test embedding service.") String text) {
        try {
            System.out.println("⏳ Đợi 150ms trước khi gọi Embedding...");
            Thread.sleep(150);

            System.out.println(
                    "📝 Bắt đầu embedding cho text: " + text.substring(0, Math.min(50, text.length())) + "...");
            long startTime = System.currentTimeMillis();

            java.util.List<Double> embedding = embeddingService.embedGoogleNormalized(text);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (embedding == null || embedding.isEmpty()) {
                return "Embedding LỖI ❌: Vector rỗng";
            }

            // Hiển thị một vài giá trị đầu tiên
            StringBuilder vectorPreview = new StringBuilder();
            int previewSize = Math.min(5, embedding.size());
            for (int i = 0; i < previewSize; i++) {
                vectorPreview.append(String.format("%.6f", embedding.get(i)));
                if (i < previewSize - 1) {
                    vectorPreview.append(", ");
                }
            }

            return String.format(
                    "Embedding OK ✅\n" +
                            "Text: '%s'\n" +
                            "Thời gian: %dms\n" +
                            "Vector dimension: %d\n" +
                            "Vector preview (5 giá trị đầu): [%s...]",
                    text, duration, embedding.size(), vectorPreview.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Embedding LỖI: Thread bị interrupt ❌";
        } catch (Exception e) {
            return "Embedding LỖI RUNTIME ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    // 7. ROUTER TEST EMBEDDING NHIỀU CHUNKS: /test/embedding-multiple
    @GetMapping("/embedding-multiple")
    public String testEmbeddingMultiple(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "3") int count) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("Test Embedding ").append(count).append(" chunks với delay 150ms:\n\n");

            long totalStartTime = System.currentTimeMillis();

            for (int i = 1; i <= count; i++) {
                String testText = "Đây là chunk số " + i + " để test embedding service với delay.";

                System.out.println("⏳ Đợi 150ms trước khi gọi Embedding chunk " + i + "...");
                Thread.sleep(150);

                long startTime = System.currentTimeMillis();
                java.util.List<Double> embedding = embeddingService.embedGoogleNormalized(testText);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                if (embedding == null || embedding.isEmpty()) {
                    result.append(String.format("Chunk %d: LỖI ❌ (Vector rỗng)\n", i));
                } else {
                    result.append(String.format(
                            "Chunk %d: OK ✅ (dimension=%d, time=%dms)\n",
                            i, embedding.size(), duration));
                }
            }

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            result.append(String.format("\nTổng thời gian: %dms", totalDuration));

            return result.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Embedding LỖI: Thread bị interrupt ❌";
        } catch (Exception e) {
            return "Embedding LỖI RUNTIME ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName();
        }
    }

    @GetMapping("/ocr-local")
    public String testLocalOcrService(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/020d1b1c-6348-461e-b75b-1be1404aa35e.pdf") String path,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "application/pdf") String mimeType) {
        try {
            System.out.println("📄 Bắt đầu OCR Local (Tesseract/PDFBox) cho file: " + path);
            System.out.println("📄 MIME Type: " + mimeType);
            long startTime = System.currentTimeMillis();

            String extractedText = localOcrService.extract(path);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String cleanedText = extractedText.replaceAll("[\r\n]", " ");

            int maxLength = 500;
            String preview;
            if (cleanedText.length() > maxLength) {
                preview = cleanedText.substring(0, maxLength) + "...";
            } else {
                preview = cleanedText;
            }

            return String.format(
                    "OCR Local OK ✅\n" +
                            "Service: Tesseract (Image) / PDFBox (PDF)\n" +
                            "File: %s\n" +
                            "MIME Type: %s\n" +
                            "Thời gian: %dms\n" +
                            "Độ dài text: %d ký tự\n" +
                            "Preview: '%s'",
                    path, mimeType, duration, cleanedText.length(), preview);
        } catch (Exception e) {
            return "OCR Local LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    @GetMapping("/ocr-local-image")
    public String testLocalOcrImage(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/12a6a4b9-edf5-49f9-b4e7-9fc89166bf13.jpg") String path) {
        try {
            System.out.println("🖼️ Bắt đầu OCR Local (Tesseract) cho ảnh: " + path);
            long startTime = System.currentTimeMillis();

            String extractedText = localOcrService.extract(path);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String cleanedText = extractedText.replaceAll("[\r\n]", " ");

            int maxLength = 500;
            String preview;
            if (cleanedText.length() > maxLength) {
                preview = cleanedText.substring(0, maxLength) + "...";
            } else {
                preview = cleanedText;
            }

            return String.format(
                    "OCR Local Image OK ✅\n" +
                            "Service: Tesseract\n" +
                            "File: %s\n" +
                            "Thời gian: %dms\n" +
                            "Độ dài text: %d ký tự\n" +
                            "Preview: '%s'",
                    path, duration, cleanedText.length(), preview);
        } catch (Exception e) {
            return "OCR Local Image LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test OCR từ hình ảnh (BufferedImage)
     * GET /test/ocr-image-buffered?path=uploads/image.jpg
     */
    @GetMapping("/ocr-image-buffered")
    public String testOcrImageBuffered(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/12a6a4b9-edf5-49f9-b4e7-9fc89166bf13.jpg") String path) {
        try {
            System.out.println("🖼️ Bắt đầu OCR từ BufferedImage: " + path);
            long startTime = System.currentTimeMillis();

            // Đọc file thành BufferedImage
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                return "LỖI ❌: File không tồn tại: " + path;
            }

            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
            if (image == null) {
                return "LỖI ❌: Không thể đọc hình ảnh từ file: " + path;
            }

            // Convert BufferedImage to temp file for OCR
            java.io.File tempFile = java.io.File.createTempFile("ocr_img_", ".png");
            try {
                javax.imageio.ImageIO.write(image, "png", tempFile);

                // OCR từ file
                String extractedText = localOcrService.extractFromImage(tempFile);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                String cleanedText = extractedText.replaceAll("[\r\n]", " ");

                int maxLength = 500;
                String preview;
                if (cleanedText.length() > maxLength) {
                    preview = cleanedText.substring(0, maxLength) + "...";
                } else {
                    preview = cleanedText;
                }

                return String.format(
                        "OCR từ BufferedImage OK ✅\n" +
                                "File: %s\n" +
                                "Image size: %dx%d\n" +
                                "Thời gian: %dms\n" +
                                "Độ dài text: %d ký tự\n" +
                                "Preview: '%s'",
                        path, image.getWidth(), image.getHeight(), duration, cleanedText.length(), preview);
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            return "OCR từ BufferedImage LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test OCR từ hình ảnh (byte array)
     * GET /test/ocr-image-bytes?path=uploads/image.jpg
     */
    @GetMapping("/ocr-image-bytes")
    public String testOcrImageBytes(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/12a6a4b9-edf5-49f9-b4e7-9fc89166bf13.jpg") String path) {
        try {
            System.out.println("🖼️ Bắt đầu OCR từ byte array: " + path);
            long startTime = System.currentTimeMillis();

            // Đọc file thành byte array
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                return "LỖI ❌: File không tồn tại: " + path;
            }

            byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());
            if (imageBytes == null || imageBytes.length == 0) {
                return "LỖI ❌: File rỗng: " + path;
            }

            // Convert byte array to temp file for OCR
            java.io.File tempFile = java.io.File.createTempFile("ocr_img_", ".png");
            try {
                java.nio.file.Files.write(tempFile.toPath(), imageBytes);

                // OCR từ file
                String extractedText = localOcrService.extractFromImage(tempFile);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                String cleanedText = extractedText.replaceAll("[\r\n]", " ");

                int maxLength = 500;
                String preview;
                if (cleanedText.length() > maxLength) {
                    preview = cleanedText.substring(0, maxLength) + "...";
                } else {
                    preview = cleanedText;
                }

                return String.format(
                        "OCR từ byte array OK ✅\n" +
                                "File: %s\n" +
                                "File size: %d bytes\n" +
                                "Thời gian: %dms\n" +
                                "Độ dài text: %d ký tự\n" +
                                "Preview: '%s'",
                        path, imageBytes.length, duration, cleanedText.length(), preview);
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            return "OCR từ byte array LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test OCR từ hình ảnh (InputStream)
     * GET /test/ocr-image-stream?path=uploads/image.jpg
     */
    @GetMapping("/ocr-image-stream")
    public String testOcrImageStream(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "uploads/12a6a4b9-edf5-49f9-b4e7-9fc89166bf13.jpg") String path) {
        try {
            System.out.println("🖼️ Bắt đầu OCR từ InputStream: " + path);
            long startTime = System.currentTimeMillis();

            // Đọc file thành InputStream
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                return "LỖI ❌: File không tồn tại: " + path;
            }

            // Convert InputStream to temp file for OCR
            java.io.File tempFile = java.io.File.createTempFile("ocr_img_", ".png");
            try (java.io.InputStream imageStream = new java.io.FileInputStream(file)) {
                java.nio.file.Files.copy(imageStream, tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // OCR từ file
                String extractedText = localOcrService.extractFromImage(tempFile);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                String cleanedText = extractedText.replaceAll("[\r\n]", " ");

                int maxLength = 500;
                String preview;
                if (cleanedText.length() > maxLength) {
                    preview = cleanedText.substring(0, maxLength) + "...";
                } else {
                    preview = cleanedText;
                }

                return String.format(
                        "OCR từ InputStream OK ✅\n" +
                                "File: %s\n" +
                                "File size: %d bytes\n" +
                                "Thời gian: %dms\n" +
                                "Độ dài text: %d ký tự\n" +
                                "Preview: '%s'",
                        path, file.length(), duration, cleanedText.length(), preview);
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            return "OCR từ InputStream LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test Web Search Service
     * GET /test/web-search?query=spring boot
     */
    @GetMapping("/web-search")
    public WebSearchResult testWebSearch(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "spring boot") String query) {
        return webSearchService.search(query);
    }

    /**
     * Test Groq Model
     * GET /test/groq-model?prompt=Hello, how are you?
     */
    @GetMapping("/groq-model")
    public AIModelTestResponse testGroqModel(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "Hello, how are you?") String prompt) {
        try {
            long start = System.currentTimeMillis();
            String response = aiModelService.callGroqModel(prompt);
            long elapsed = System.currentTimeMillis() - start;
            return AIModelTestResponse.success("Groq", prompt, response, elapsed);
        } catch (Exception e) {
            return AIModelTestResponse.error("Groq", prompt,
                    e.getMessage() + " (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Test Gemini Model
     * GET /test/gemini-model?prompt=Hello, how are you?
     */
    @GetMapping("/gemini-model")
    public AIModelTestResponse testGeminiModel(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "Hello, how are you?") String prompt) {
        try {
            long start = System.currentTimeMillis();
            String response = aiModelService.callGeminiModel(prompt);
            long elapsed = System.currentTimeMillis() - start;
            return AIModelTestResponse.success("Gemini", prompt, response, elapsed);
        } catch (Exception e) {
            return AIModelTestResponse.error("Gemini", prompt,
                    e.getMessage() + " (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Test Chat History - lấy 2 cặp chat gần nhất (4 messages).
     * GET /test/chat-history?conversationId=xxx&userId=xxx
     * 
     * @param conversationId Conversation ID (required)
     * @param userId         User ID (required)
     * @return Chat history string với 2 cặp chat gần nhất
     */
    @GetMapping("/chat-history")
    public String testChatHistory(
            @org.springframework.web.bind.annotation.RequestParam UUID conversationId,
            @org.springframework.web.bind.annotation.RequestParam UUID userId) {
        try {
            System.out.println("📝 Bắt đầu test Chat History:");
            System.out.println("   Conversation ID: " + conversationId);
            System.out.println("   User ID: " + userId);

            long startTime = System.currentTimeMillis();

            // Gọi public method để test chat history (chỉ lấy 2 cặp chat gần nhất)
            String chatHistory = chatBotService.getChatHistory(conversationId, userId);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (chatHistory == null || chatHistory.isEmpty()) {
                return String.format(
                        "Chat History Test ✅\n" +
                                "Conversation ID: %s\n" +
                                "User ID: %s\n" +
                                "Thời gian: %dms\n" +
                                "Kết quả: Chat history rỗng (không có messages hoặc không có dữ liệu)",
                        conversationId, userId, duration);
            }

            // Format output để dễ đọc
            String formattedHistory = chatHistory.replace("\n\n", "\n");
            int maxLength = 1002000;
            String preview;
            if (formattedHistory.length() > maxLength) {
                preview = formattedHistory.substring(0, maxLength) + "\n\n...[truncated, total length: "
                        + formattedHistory.length() + " chars]";
            } else {
                preview = formattedHistory;
            }

            // Đếm số dòng và số ký tự
            int lineCount = chatHistory.split("\n").length;
            int charCount = chatHistory.length();

            // Kiểm tra xem có OCR text không
            boolean hasOcrText = chatHistory.contains("[Câu hỏi bổ sung từ hình ảnh:") ||
                    chatHistory.contains("[Câu hỏi từ hình ảnh:") ||
                    chatHistory.contains("[Thông tin từ hình ảnh:");

            return String.format(
                    "Chat History Test ✅\n" +
                            "Conversation ID: %s\n" +
                            "User ID: %s\n" +
                            "Thời gian: %dms\n" +
                            "Số dòng: %d\n" +
                            "Số ký tự: %d\n" +
                            "Có OCR text: %s\n\n" +
                            "--- CHAT HISTORY (2 cặp chat gần nhất) ---\n%s",
                    conversationId, userId, duration, lineCount, charCount,
                    hasOcrText ? "Có ✅" : "Không ❌", preview);

        } catch (Exception e) {
            return "Chat History Test LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test Summarize Documents - tóm tắt tài liệu từ danh sách files.
     * GET /test/summarize-documents?fileIds=uuid1,uuid2,uuid3&modelId=uuid
     * Hoặc: GET
     * /test/summarize-documents?fileIds=uuid1&fileIds=uuid2&fileIds=uuid3&modelId=uuid
     * 
     * @param fileIds List of file IDs (có thể truyền nhiều lần hoặc
     *                comma-separated) (required)
     * @param modelId LLM Model ID (optional, sẽ dùng model mặc định nếu null)
     * @return Summarized text
     */
    @GetMapping("/summarize-documents")
    public String testSummarizeDocuments(
            @org.springframework.web.bind.annotation.RequestParam List<String> fileIds,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID modelId) {
        try {
            System.out.println("📝 Bắt đầu test Summarize Documents:");
            System.out.println("   File IDs: " + fileIds);
            System.out.println("   Model ID: " + modelId);

            if (fileIds == null || fileIds.isEmpty()) {
                return "Lỗi: Không có file ID nào được cung cấp";
            }

            // Parse fileIds - hỗ trợ cả comma-separated string và multiple params
            List<UUID> fileIdList = new ArrayList<>();
            for (String fileIdParam : fileIds) {
                // Nếu có comma, split ra
                if (fileIdParam.contains(",")) {
                    String[] fileIdStrings = fileIdParam.split(",");
                    for (String fileIdStr : fileIdStrings) {
                        try {
                            UUID fileId = UUID.fromString(fileIdStr.trim());
                            if (!fileIdList.contains(fileId)) {
                                fileIdList.add(fileId);
                            }
                        } catch (IllegalArgumentException e) {
                            return "Lỗi: File ID không hợp lệ: " + fileIdStr;
                        }
                    }
                } else {
                    // Single UUID
                    try {
                        UUID fileId = UUID.fromString(fileIdParam.trim());
                        if (!fileIdList.contains(fileId)) {
                            fileIdList.add(fileId);
                        }
                    } catch (IllegalArgumentException e) {
                        return "Lỗi: File ID không hợp lệ: " + fileIdParam;
                    }
                }
            }

            if (fileIdList.isEmpty()) {
                return "Lỗi: Không có file ID hợp lệ nào được cung cấp";
            }

            // Lấy NotebookFile từ fileIds
            List<NotebookFile> files = new ArrayList<>();
            List<String> notFoundFiles = new ArrayList<>();
            for (UUID fileId : fileIdList) {
                NotebookFile file = notebookFileRepository.findById(fileId)
                        .orElse(null);
                if (file == null) {
                    notFoundFiles.add(fileId.toString());
                } else {
                    files.add(file);
                }
            }

            if (!notFoundFiles.isEmpty()) {
                return "Lỗi: Không tìm thấy các file với ID: " + String.join(", ", notFoundFiles);
            }

            if (files.isEmpty()) {
                return "Lỗi: Không có file nào để tóm tắt";
            }

            // Lấy LlmModel nếu có
            LlmModel llmModel = null;
            if (modelId != null) {
                llmModel = llmModelRepository.findById(modelId).orElse(null);
                if (llmModel == null) {
                    return "Lỗi: Không tìm thấy model với ID: " + modelId;
                }
            }

            long startTime = System.currentTimeMillis();

            // Gọi hàm summarizeDocuments từ AiAsyncTaskService
            String summarizedText = aiAsyncTaskService.summarizeDocuments(files, llmModel);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (summarizedText == null || summarizedText.isEmpty()) {
                return String.format(
                        "Summarize Documents Test ✅\n" +
                                "Số file: %d\n" +
                                "File IDs: %s\n" +
                                "Model ID: %s\n" +
                                "Thời gian: %dms\n" +
                                "Kết quả: Text rỗng (không có chunks hoặc không có dữ liệu)",
                        files.size(), String.join(", ", fileIdList.stream().map(UUID::toString).toList()), modelId,
                        duration);
            }

            // Format output
            int charCount = summarizedText.length();
            int lineCount = summarizedText.split("\n").length;
            int maxPreviewLength = 2000;
            String preview = summarizedText.length() > maxPreviewLength
                    ? summarizedText.substring(0, maxPreviewLength) + "\n\n...[truncated, total length: "
                            + summarizedText.length() + " chars]"
                    : summarizedText;

            // Tạo danh sách file names để hiển thị
            List<String> fileNames = files.stream()
                    .map(f -> f.getOriginalFilename() != null ? f.getOriginalFilename() : f.getId().toString())
                    .toList();

            return String.format(
                    "Summarize Documents Test ✅\n" +
                            "Số file: %d\n" +
                            "File IDs: %s\n" +
                            "File names: %s\n" +
                            "Model ID: %s\n" +
                            "Thời gian: %dms\n" +
                            "Số dòng: %d\n" +
                            "Số ký tự: %d\n\n" +
                            "--- SUMMARIZED TEXT ---\n%s",
                    files.size(),
                    String.join(", ", fileIdList.stream().map(UUID::toString).toList()),
                    String.join(", ", fileNames),
                    modelId, duration, lineCount, charCount, preview);

        } catch (Exception e) {
            return "Summarize Documents Test LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }

    /**
     * Test Generate Quiz - tạo quiz từ các notebook files.
     * GET
     * /test/generate-quiz?notebookId=uuid&fileIds=uuid1,uuid2,uuid3&numberOfQuestions=standard&difficultyLevel=medium
     * Hoặc: GET
     * /test/generate-quiz?notebookId=uuid&fileIds=uuid1&fileIds=uuid2&numberOfQuestions=many&difficultyLevel=hard
     * 
     * numberOfQuestions: "few" | "standard" | "many"
     * difficultyLevel: "easy" | "medium" | "hard"
     * 
     * @param notebookId        Notebook ID (required)
     * @param fileIds           Danh sách file IDs (có thể truyền nhiều lần hoặc
     *                          comma-separated) (required)
     * @param numberOfQuestions Số lượng câu hỏi: "few" | "standard" | "many"
     *                          (optional, mặc định: "standard")
     * @param difficultyLevel   Độ khó: "easy" | "medium" | "hard" (optional, mặc
     *                          định: "medium")
     * @return Quiz generation result
     */
    @GetMapping("/generate-quiz")
    public String testGenerateQuiz(
            @org.springframework.web.bind.annotation.RequestParam UUID notebookId,
            @org.springframework.web.bind.annotation.RequestParam UUID userId,
            @org.springframework.web.bind.annotation.RequestParam List<String> fileIds,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "standard") String numberOfQuestions,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "medium") String difficultyLevel) {
        try {
            System.out.println("📝 Bắt đầu test Generate Quiz:");
            System.out.println("   Notebook ID: " + notebookId);
            System.out.println("   User ID: " + userId);
            System.out.println("   File IDs: " + fileIds);
            System.out.println("   Number of Questions: " + numberOfQuestions);
            System.out.println("   Difficulty Level: " + difficultyLevel);

            // Parse fileIds từ comma-separated string hoặc multiple params
            List<UUID> fileIdList = new ArrayList<>();
            for (String fileIdParam : fileIds) {
                if (fileIdParam.contains(",")) {
                    String[] fileIdStrings = fileIdParam.split(",");
                    for (String fileIdStr : fileIdStrings) {
                        try {
                            UUID fileId = UUID.fromString(fileIdStr.trim());
                            if (!fileIdList.contains(fileId)) {
                                fileIdList.add(fileId);
                            }
                        } catch (IllegalArgumentException e) {
                            return "Lỗi: File ID không hợp lệ: " + fileIdStr;
                        }
                    }
                } else {
                    try {
                        UUID fileId = UUID.fromString(fileIdParam.trim());
                        if (!fileIdList.contains(fileId)) {
                            fileIdList.add(fileId);
                        }
                    } catch (IllegalArgumentException e) {
                        return "Lỗi: File ID không hợp lệ: " + fileIdParam;
                    }
                }
            }

            if (fileIdList.isEmpty()) {
                return "Lỗi: Không có file ID hợp lệ nào được cung cấp";
            }

            long startTime = System.currentTimeMillis();

            // Gọi hàm generateQuiz với userId từ AiGenerationService
            Map<String, Object> result = aiGenerationService.generateQuiz(notebookId, userId, fileIdList,
                    numberOfQuestions, difficultyLevel, null);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Format output
            StringBuilder output = new StringBuilder();
            output.append("Generate Quiz Test ");

            if (result.containsKey("error")) {
                output.append("LỖI ❌\n");
                output.append("Error: ").append(result.get("error")).append("\n");
                if (result.containsKey("errorType")) {
                    output.append("Error Type: ").append(result.get("errorType")).append("\n");
                }
            } else {
                output.append("✅\n");
            }

            output.append("Notebook ID: ").append(result.getOrDefault("notebookId", "N/A")).append("\n");
            output.append("Selected Files Count: ").append(result.getOrDefault("selectedFilesCount", 0)).append("\n");

            if (result.containsKey("selectedFileIds")) {
                @SuppressWarnings("unchecked")
                List<String> selectedFileIds = (List<String>) result.get("selectedFileIds");
                output.append("Selected File IDs: ").append(String.join(", ", selectedFileIds)).append("\n");
            }

            if (result.containsKey("requestedFileIds")) {
                @SuppressWarnings("unchecked")
                List<String> requestedFileIds = (List<String>) result.get("requestedFileIds");
                output.append("Requested File IDs: ").append(String.join(", ", requestedFileIds)).append("\n");
            }

            output.append("Summary Length: ").append(result.getOrDefault("summaryLength", 0)).append(" chars\n");

            if (result.containsKey("summaryPreview")) {
                output.append("Summary Preview: ").append(result.get("summaryPreview")).append("\n");
            }

            output.append("Prompt Length: ").append(result.getOrDefault("promptLength", 0)).append(" chars\n");
            output.append("Raw Response Length: ").append(result.getOrDefault("rawResponseLength", 0))
                    .append(" chars\n");
            output.append("Number of Questions: ").append(numberOfQuestions).append("\n");
            output.append("Difficulty Level: ").append(difficultyLevel).append("\n");
            output.append("Thời gian: ").append(duration).append("ms\n\n");

            if (result.containsKey("parsedQuiz")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> quizList = (List<Map<String, Object>>) result.get("parsedQuiz");
                output.append("Quiz Count: ").append(quizList.size()).append("\n");
                output.append("Success: ").append(result.getOrDefault("success", false)).append("\n\n");

                // Hiển thị preview của quiz (chỉ 2 câu đầu)
                output.append("--- QUIZ PREVIEW (first 2 questions) ---\n");
                int previewCount = Math.min(2, quizList.size());
                for (int i = 0; i < previewCount; i++) {
                    Map<String, Object> quiz = quizList.get(i);
                    output.append("\nQuestion ").append(i + 1).append(":\n");
                    output.append("  Question: ").append(quiz.get("question")).append("\n");
                    output.append("  Explanation: ").append(quiz.get("explanation")).append("\n");
                    output.append("  Difficulty: ").append(quiz.get("difficulty_level")).append("\n");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> options = (List<Map<String, Object>>) quiz.get("options");
                    if (options != null) {
                        output.append("  Options (").append(options.size()).append("):\n");
                        for (Map<String, Object> option : options) {
                            output.append("    - ").append(option.get("text"))
                                    .append(" [").append(option.get("is_correct")).append("]\n");
                        }
                    }
                }

                if (quizList.size() > previewCount) {
                    output.append("\n... (còn ").append(quizList.size() - previewCount).append(" câu hỏi nữa)\n");
                }
            } else if (result.containsKey("rawResponse")) {
                output.append("--- RAW RESPONSE (first 1000 chars) ---\n");
                String rawResponse = (String) result.get("rawResponse");
                if (rawResponse.length() > 1000) {
                    output.append(rawResponse.substring(0, 1000)).append("\n... [truncated]\n");
                } else {
                    output.append(rawResponse).append("\n");
                }
            }

            return output.toString();

        } catch (Exception e) {
            return "Generate Quiz Test LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }
}