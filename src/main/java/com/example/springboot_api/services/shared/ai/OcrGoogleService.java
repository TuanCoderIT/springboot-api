package com.example.springboot_api.services.shared.ai;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OcrGoogleService {

    private final com.google.genai.Client geminiClient;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final String OCR_PROMPT = "Trích xuất tất cả văn bản dưới dạng raw text, giữ nguyên định dạng dòng, từ file này:";
    private static final String OCR_MODEL = "gemini-2.5-flash";
    private static final String TEXT_MODEL = "gemini-2.5-flash"; // Dùng cho file Word đã trích xuất text

    public String extractTextFromDocument(String localFilePath, String mimeType) throws IOException {
        Path path = getValidPath(localFilePath);

        String detectedMimeType = detectMimeType(path, mimeType);

        System.out.println("OCR: Original MIME type: " + mimeType);
        System.out.println("OCR: Detected MIME type: " + detectedMimeType);
        System.out.println("OCR: File path: " + path);

        if (detectedMimeType.startsWith("image/") || detectedMimeType.equals("application/pdf")) {
            byte[] fileBytes = Files.readAllBytes(path);

            try {
                Part filePart = Part.fromBytes(fileBytes, detectedMimeType);
                List<Part> parts = Arrays.asList(
                        filePart,
                        Part.fromText(OCR_PROMPT));

                Content content = Content.fromParts(parts.toArray(new Part[0]));

                GenerateContentResponse response = geminiClient.models.generateContent(
                        OCR_MODEL,
                        List.of(content),
                        null);

                String result = response.text();
                System.out.println("✅ OCR API call thành công");
                return result;
            } catch (IllegalArgumentException e) {
                System.err.println("❌ Lỗi khi tạo Part với MIME type: " + detectedMimeType);
                System.err.println("Error: " + e.getMessage());
                throw new BadRequestException(
                        "Không thể xử lý file với MIME type: " + detectedMimeType + ". Lỗi: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("❌ Lỗi OCR API call tại OcrService: " + e.getMessage());
                System.err.println("❌ Exception type: " + e.getClass().getName());
                e.printStackTrace();
                throw e;
            }

        } else if (detectedMimeType.contains("word")
                || detectedMimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            String extractedText = extractTextFromDocx(path);
            return cleanAndProcessText(extractedText);

        } else {
            throw new BadRequestException("Định dạng file không được hỗ trợ: " + detectedMimeType);
        }
    }

    private String detectMimeType(Path path, String providedMimeType) {
        if (providedMimeType != null && !providedMimeType.equals("application/octet-stream")) {
            return providedMimeType;
        }

        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".doc")) {
            return "application/msword";
        }

        throw new BadRequestException("Không thể xác định định dạng file: " + fileName);
    }

    /**
     * Sử dụng Apache POI để đọc nội dung text từ file DOCX.
     */
    private String extractTextFromDocx(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
                XWPFDocument document = new XWPFDocument(fis);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            throw new IOException("Lỗi khi đọc file DOCX: " + e.getMessage(), e);
        }
    }

    /**
     * Phương thức bổ sung: Gửi văn bản thô lên Gemini để đảm bảo chất lượng.
     * (Có thể thay đổi để thực hiện tóm tắt, RAG,...)
     */
    private String cleanAndProcessText(String rawText) {
        if (rawText.isEmpty()) {
            return "Nội dung văn bản trống.";
        }

        String processingPrompt = "Dọn dẹp văn bản thô sau, loại bỏ các ký tự lạ, và chuẩn hóa định dạng câu. Giữ nguyên nội dung và ngôn ngữ.";

        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    TEXT_MODEL,
                    List.of(Content.fromParts(Part.fromText(processingPrompt + "\n\n" + rawText))),
                    null);

            return response.text();
        } catch (Exception e) {
            System.err.println("LỖI XỬ LÝ TEXT BẰNG GEMINI: " + e.getMessage());
            return rawText; // Trả về text thô nếu lỗi
        }
    }

    private Path getValidPath(String storageUrl) {
        String filePath = storageUrl;

        if (filePath.startsWith("/uploads/")) {
            String filename = filePath.replaceFirst("^/uploads/", "");
            filePath = Paths.get(uploadDir, filename).toString();
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Path absolutePath = Paths.get(System.getProperty("user.dir"), filePath);
            if (Files.exists(absolutePath)) {
                return absolutePath;
            }
            throw new BadRequestException("Không tìm thấy file tại đường dẫn: " + storageUrl);
        }
        return path;
    }
}