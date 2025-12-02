package com.example.springboot_api.services.shared.ai;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.BadRequestException;

import net.sourceforge.tess4j.Tesseract;

@Service
public class OcrService {

    private final Tika tika = new Tika();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final Tesseract tess;

    public OcrService() {
        tess = new Tesseract();
        tess.setDatapath("/usr/share/tessdata");
        tess.setLanguage("vie+eng");
        tess.setOcrEngineMode(1);
    }

    public String extractTextFromDocument(String localFilePath, String mimeType) throws IOException {
        Path path = getValidPath(localFilePath);
        File file = path.toFile();

        String detectedMimeType = mimeType;
        if (detectedMimeType == null || detectedMimeType.equals("application/octet-stream")) {
            detectedMimeType = tika.detect(file);
        }

        System.out.println("OCR: Original MIME type: " + mimeType);
        System.out.println("OCR: Detected MIME type: " + detectedMimeType);
        System.out.println("OCR: File path: " + path);

        try {
            String text = extractText(file);

            if (text == null || text.trim().isEmpty()) {
                throw new BadRequestException("OCR không đọc được nội dung từ file.");
            }

            System.out.println("✅ OCR hoàn thành, độ dài text: " + text.length());
            return text;

        } catch (Exception e) {
            System.err.println("❌ Lỗi OCR tại OcrService: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Lỗi khi thực hiện OCR: " + e.getMessage(), e);
        }
    }

    public String extractText(File file) throws Exception {
        String mime = tika.detect(file);

        if (mime.equals("application/pdf")) {
            return extractFromPDF(file);
        } else if (mime.startsWith("image/")) {
            return extractFromImage(file);
        } else if (mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mime.equals("application/msword")) {
            return extractFromWord(file);
        }

        throw new BadRequestException("Định dạng file không được hỗ trợ: " + mime);
    }

    private String extractFromWord(File file) throws Exception {
        String text = tika.parseToString(file);
        return clean(text);
    }

    private String extractFromPDF(File file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(doc.getNumberOfPages());

            String text = stripper.getText(doc);
            return clean(text);
        }
    }

    private String extractFromImage(File file) throws Exception {
        BufferedImage img = ImageIO.read(file);

        if (img == null) {
            return "";
        }

        String text = tess.doOCR(img);
        return clean(text);
    }

    private String clean(String s) {
        if (s == null)
            return "";
        return s
                .replaceAll("[\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private Path getValidPath(String storageUrl) {
        String filePath = storageUrl;

        if (filePath.startsWith("/uploads/")) {
            String filename = filePath.replaceFirst("^/uploads/", "");
            filePath = Paths.get(uploadDir, filename).toString();
        }

        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            return path;
        }

        Path absolutePath = Paths.get(System.getProperty("user.dir"), filePath);

        if (Files.exists(absolutePath)) {
            return absolutePath;
        }

        throw new BadRequestException("Không tìm thấy file tại đường dẫn: " + storageUrl);
    }
}
