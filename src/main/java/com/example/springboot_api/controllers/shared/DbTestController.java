package com.example.springboot_api.controllers.shared;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.services.shared.ai.EmbeddingService;
import com.example.springboot_api.services.shared.ai.OcrGoogleService;
import com.example.springboot_api.services.shared.ai.OcrService;

@RestController
@RequestMapping("/test")
public class DbTestController {

    private final DataSource dataSource;
    private final EmbeddingService embeddingService;
    private final OcrGoogleService ocrService;
    private final OcrService localOcrService;

    public DbTestController(
            DataSource dataSource,
            EmbeddingService embeddingService,
            OcrGoogleService ocrService,
            OcrService localOcrService) {
        this.dataSource = dataSource;
        this.embeddingService = embeddingService;
        this.ocrService = ocrService;
        this.localOcrService = localOcrService;
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

            String extractedText = localOcrService.extractTextFromDocument(path, mimeType);

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

            String extractedText = localOcrService.extractTextFromDocument(path, "image/jpeg");

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
}