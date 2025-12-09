package com.example.springboot_api.controllers.shared;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.shared.ai.AIModelTestResponse;
import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.EmbeddingService;
import com.example.springboot_api.services.shared.ai.OcrGoogleService;
import com.example.springboot_api.services.shared.ai.OcrService;
import com.example.springboot_api.services.shared.ai.WebSearchService;
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

    public DbTestController(
            EmbeddingService embeddingService,
            OcrGoogleService ocrService,
            OcrService localOcrService,
            WebSearchService webSearchService,
            AIModelService aiModelService,
            ChatBotService chatBotService) {
        this.embeddingService = embeddingService;
        this.ocrService = ocrService;
        this.localOcrService = localOcrService;
        this.webSearchService = webSearchService;
        this.aiModelService = aiModelService;
        this.chatBotService = chatBotService;
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
     * Test Chat History với OCR text
     * GET /test/chat-history?conversationId=xxx&userId=xxx&excludeMessageId=xxx
     * 
     * @param conversationId   Conversation ID (required)
     * @param userId           User ID (required)
     * @param excludeMessageId Message ID để exclude (optional)
     * @return Chat history string với OCR text từ hình ảnh
     */
    @GetMapping("/chat-history")
    public String testChatHistory(
            @org.springframework.web.bind.annotation.RequestParam UUID conversationId,
            @org.springframework.web.bind.annotation.RequestParam UUID userId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID excludeMessageId) {
        try {
            System.out.println("📝 Bắt đầu test Chat History:");
            System.out.println("   Conversation ID: " + conversationId);
            System.out.println("   User ID: " + userId);
            System.out.println("   Exclude Message ID: " + excludeMessageId);

            long startTime = System.currentTimeMillis();

            // Gọi public method để test chat history
            String chatHistory = chatBotService.getChatHistoryForTest(conversationId, userId, excludeMessageId);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (chatHistory == null || chatHistory.isEmpty()) {
                return String.format(
                        "Chat History Test ✅\n" +
                                "Conversation ID: %s\n" +
                                "User ID: %s\n" +
                                "Exclude Message ID: %s\n" +
                                "Thời gian: %dms\n" +
                                "Kết quả: Chat history rỗng (không có messages hoặc không có dữ liệu)",
                        conversationId, userId, excludeMessageId, duration);
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
                            "Exclude Message ID: %s\n" +
                            "Thời gian: %dms\n" +
                            "Số dòng: %d\n" +
                            "Số ký tự: %d\n" +
                            "Có OCR text: %s\n\n" +
                            "--- CHAT HISTORY ---\n%s",
                    conversationId, userId, excludeMessageId, duration, lineCount, charCount,
                    hasOcrText ? "Có ✅" : "Không ❌", preview);

        } catch (Exception e) {
            return "Chat History Test LỖI ❌: " + e.getMessage() + "\n" +
                    "Error Type: " + e.getClass().getSimpleName() + "\n" +
                    "Stack: " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "N/A");
        }
    }
}