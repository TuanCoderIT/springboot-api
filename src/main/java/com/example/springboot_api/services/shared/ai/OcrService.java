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
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.BadRequestException;

@Service
public class OcrService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final Tika tika = new Tika();

    // Auto detect tesseract path
    private String getTesseractBinary() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            // Brew path
            if (Files.exists(Path.of("/opt/homebrew/bin/tesseract")))
                return "/opt/homebrew/bin/tesseract";
            if (Files.exists(Path.of("/usr/local/bin/tesseract")))
                return "/usr/local/bin/tesseract";
        }

        if (os.contains("linux")) {
            return "/usr/bin/tesseract";
        }

        if (os.contains("win")) {
            return "tesseract.exe"; // yêu cầu set PATH
        }

        return "tesseract";
    }

    // ============= MAIN ENTRY =============

    public String extract(String filePath) throws Exception {
        Path path = resolvePath(filePath);
        File file = path.toFile();

        String mime = tika.detect(file);

        if (mime.equals("application/pdf"))
            return extractFromPDF(file);

        if (mime.startsWith("image/"))
            return extractFromImage(file);

        if (mime.contains("word"))
            return extractFromWord(file);

        throw new BadRequestException("Không hỗ trợ OCR file: " + mime);
    }

    // ============= OCR IMAGE =============

    public String extractFromImage(File file) throws Exception {
        return runTesseract(file);
    }

    // ============= OCR WORD =============
    // dùng Tika, miễn phí, không lỗi
    public String extractFromWord(File file) throws Exception {
        String text = tika.parseToString(file);
        return cleanText(text);
    }

    // ============= OCR PDF =============
    // convert từng page -> PNG -> OCR
    public String extractFromPDF(File file) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int totalPages = doc.getNumberOfPages();

            for (int i = 0; i < totalPages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 300); // DPI cao → OCR đẹp
                File temp = convertBufferedImageToTemp(img);

                String text = runTesseract(temp);
                sb.append(text).append("\n");

                temp.delete();
            }
        }

        return cleanText(sb.toString());
    }

    // ============= SUPPORT: run Tesseract CLI =============

    private String runTesseract(File file) throws Exception {
        String tesseract = getTesseractBinary();

        ProcessBuilder pb = new ProcessBuilder(
                tesseract,
                file.getAbsolutePath(),
                "stdout",
                "-l", "vie+eng");

        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0)
            throw new BadRequestException("OCR thất bại (exit " + exitCode + "): " + output);

        return cleanText(output);
    }

    // convert buffer image -> file tạm
    private File convertBufferedImageToTemp(BufferedImage image) throws IOException {
        File temp = File.createTempFile("ocr_img_", ".png");
        ImageIO.write(image, "png", temp);
        return temp;
    }

    // ============= CLEAN TEXT =============

    private String cleanText(String text) {
        if (text == null)
            return "";
        return text
                .replaceAll("[\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    // ============= PATH =============

    private Path resolvePath(String storageUrl) {
        if (storageUrl.startsWith("/uploads/")) {
            storageUrl = storageUrl.replaceFirst("^/uploads/", "");
        }

        Path p1 = Paths.get(uploadDir, storageUrl);
        if (Files.exists(p1))
            return p1;

        Path p2 = Paths.get(storageUrl);
        if (Files.exists(p2))
            return p2;

        throw new BadRequestException("Không tìm thấy file: " + storageUrl);
    }
}
