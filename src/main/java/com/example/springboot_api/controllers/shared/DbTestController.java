package com.example.springboot_api.controllers.shared;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.services.shared.ai.EmbeddingService;
import com.example.springboot_api.services.shared.ai.OcrService;

@RestController
@RequestMapping("/test")
public class DbTestController {

    private final DataSource dataSource;
    private final EmbeddingService embeddingService;
    private final OcrService ocrService;

    public DbTestController(
            DataSource dataSource,
            EmbeddingService embeddingService,
            OcrService ocrService) {
        this.dataSource = dataSource;
        this.embeddingService = embeddingService;
        this.ocrService = ocrService;
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
}