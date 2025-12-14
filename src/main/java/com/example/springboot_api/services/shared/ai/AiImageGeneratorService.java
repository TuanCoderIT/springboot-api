package com.example.springboot_api.services.shared.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import lombok.extern.slf4j.Slf4j;

/**
 * Service sinh hình ảnh AI từ prompt text.
 *
 * Gemini Image Generation:
 * - gemini-2.5-flash-image
 */
@Slf4j
@Service
public class AiImageGeneratorService {

    private final Client geminiClient;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8386}")
    private String baseUrl;

    private static final String IMAGE_MODEL = "gemini-2.5-flash-image";

    public AiImageGeneratorService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String generateAndSaveImage(String prompt) throws IOException {
        byte[] imageBytes = generateImage(prompt);
        if (imageBytes == null)
            return null;

        String filename = UUID.randomUUID() + ".png";
        Path imagesDir = Paths.get(uploadDir, "ai-images");
        Files.createDirectories(imagesDir);

        Path imagePath = imagesDir.resolve(filename);
        Files.write(imagePath, imageBytes);

        log.info("✅ AI image saved: {}", imagePath);
        return baseUrl + "/uploads/ai-images/" + filename;
    }

    /**
     * CORE: sinh image bytes
     */
    public byte[] generateImage(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            log.warn("⚠️ Empty prompt");
            return null;
        }

        try {
            log.info("🖼️ Generating image with Gemini 2.5 Flash Image");

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities(List.of("IMAGE"))
                    .build();

            Content content = Content.builder()
                    .parts(List.of(
                            Part.builder().text(prompt).build()))
                    .build();

            GenerateContentResponse response = geminiClient.models.generateContent(
                    IMAGE_MODEL,
                    List.of(content),
                    config);

            Optional<List<Candidate>> candidatesOpt = response.candidates();
            if (candidatesOpt.isEmpty() || candidatesOpt.get().isEmpty()) {
                log.error("❌ No candidates returned");
                return null;
            }

            Candidate candidate = candidatesOpt.get().get(0);

            // Truy cập image data qua candidate.content().parts()
            Optional<Content> contentOpt = candidate.content();
            if (contentOpt.isPresent()) {
                byte[] imageBytes = extractImageFromParts(contentOpt.get().parts());
                if (imageBytes != null)
                    return imageBytes;
            }

            log.error("❌ No image data found in candidate");
            return null;

        } catch (Exception e) {
            log.error("❌ Error generating image", e);
            return null;
        }
    }

    /**
     * Helper parse image từ parts
     */
    private byte[] extractImageFromParts(Optional<List<Part>> partsOpt) {
        if (partsOpt.isEmpty())
            return null;

        for (Part part : partsOpt.get()) {
            Optional<Blob> blobOpt = part.inlineData();
            if (blobOpt.isPresent()) {
                Optional<byte[]> dataOpt = blobOpt.get().data();
                if (dataOpt.isPresent()) {
                    byte[] bytes = dataOpt.get();
                    log.info("✅ Image extracted ({} bytes)", bytes.length);
                    return bytes;
                }
            }
        }
        return null;
    }

    public String generateImageBase64(String prompt) {
        byte[] imageBytes = generateImage(prompt);
        if (imageBytes == null)
            return null;
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public String generateSlideImageBase64(String topic) {
        String prompt = String.format(
                "Modern flat illustration for a presentation slide about %s. "
                        + "Clean, minimal, professional, soft gradients. "
                        + "No text, no words, no labels. "
                        + "16:9 composition.",
                topic);
        return generateImageBase64(prompt);
    }

    public String generateIconBase64(String concept) {
        String prompt = String.format(
                "Simple flat icon illustration of %s. "
                        + "Minimal design, single object centered. "
                        + "Soft gradient background. No text.",
                concept);
        return generateImageBase64(prompt);
    }

    public boolean isConfigured() {
        return geminiClient != null;
    }
}
