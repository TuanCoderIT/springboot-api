package com.example.springboot_api.services.shared.ai.generation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.dto.shared.VideoSlide;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.models.VideoAsset;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.VideoAssetRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.GeminiTtsService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;
import com.example.springboot_api.services.shared.ai.VideoFrameService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý video generation.
 * Pipeline: Summarize → LLM Plan → Render → TTS → Merge
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final GeminiTtsService ttsService;
    private final AiSetStatusService statusService;
    private final JsonParsingService jsonParsingService;
    private final VideoFrameService videoFrameService;

    /**
     * Xử lý video generation ở background.
     */
    @Async
    @Transactional
    public void processVideoGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String templateName, String additionalRequirements,
            int numberOfSlides, boolean generateImages) {

        String sessionId = aiSetId.toString().substring(0, 8);
        String videoTitle = "Video";

        try {
            log.info("🎬 [VIDEO] Session: {} | slides={}", sessionId, numberOfSlides);
            statusService.markProcessing(aiSetId);

            // Validate entities
            Notebook notebook = notebookRepository.findById(notebookId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Notebook/User không tồn tại");
                return;
            }

            List<NotebookFile> files = fileIds.stream()
                    .map(id -> notebookFileRepository.findById(id).orElse(null))
                    .filter(f -> f != null)
                    .toList();
            if (files.isEmpty()) {
                statusService.markFailed(aiSetId, "Không có file");
                return;
            }

            // Step 1: Summarize
            log.info("📝 [VIDEO] Step 1: Tóm tắt...");
            String summary = summarizationService.summarizeDocuments(files, null);
            if (summary == null || summary.isBlank()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt");
                return;
            }

            // Step 2: LLM Plan
            log.info("🤖 [VIDEO] Step 2: Tạo plan...");
            String llmResponse = aiModelService
                    .callGeminiModel(buildVideoPrompt(summary, numberOfSlides, additionalRequirements));
            Map<String, Object> plan = jsonParsingService.parseVideoJson(llmResponse);
            if (plan == null) {
                statusService.markFailed(aiSetId, "Không thể parse plan");
                return;
            }

            videoTitle = extractString(plan.getOrDefault("title", "Video"));

            // Cập nhật title cho AiSet ngay sau khi có từ AI
            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet != null) {
                aiSet.setTitle(videoTitle);
                aiSet.setUpdatedAt(OffsetDateTime.now());
                aiSetRepository.save(aiSet);
                log.info("📝 [VIDEO] Cập nhật title AiSet: {}", videoTitle);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> slidesData = (List<Map<String, Object>>) plan.get("slides");
            if (slidesData == null || slidesData.isEmpty()) {
                statusService.markFailed(aiSetId, "Không có slides");
                return;
            }

            // Build slides
            List<VideoSlide> slides = new ArrayList<>();
            for (int i = 0; i < slidesData.size(); i++) {
                Map<String, Object> sd = slidesData.get(i);
                slides.add(VideoSlide.builder()
                        .index(i)
                        .title(extractString(sd.get("title")))
                        .body(extractString(sd.get("body")))
                        .imagePrompt(generateImages ? extractString(sd.get("imagePrompt")) : null)
                        .audioScript(extractString(sd.get("audioScript")))
                        .build());
            }
            log.info("✅ [VIDEO] Plan: {} slides, title: {}", slides.size(), videoTitle);

            // Setup directories
            Path workDir = Paths.get("uploads", "videos", sessionId);
            Files.createDirectories(workDir.resolve("slides"));
            Files.createDirectories(workDir.resolve("audio"));
            Files.createDirectories(workDir.resolve("clips"));

            // Step 3: Render frames
            log.info("🎨 [VIDEO] Step 3: Render frames...");
            List<String> frameBase64List = videoFrameService.renderVideoFrames(videoTitle,
                    slides.stream().map(s -> VideoFrameService.FrameContent.builder()
                            .title(s.getTitle()).body(s.getBody())
                            .imagePrompt(s.getImagePrompt()).audioScript(s.getAudioScript())
                            .build()).toList(),
                    generateImages);

            // Lưu base64 thành file PNG
            for (int i = 0; i < Math.min(frameBase64List.size(), slides.size()); i++) {
                Path dst = workDir.resolve("slides").resolve(String.format("frame_%02d.png", i + 1));
                byte[] imageBytes = java.util.Base64.getDecoder().decode(frameBase64List.get(i));
                Files.write(dst, imageBytes);
                slides.get(i).setImagePath(dst.toString());
                slides.get(i).setImageReady(true);
            }

            // Step 4: Generate audio
            log.info("🔊 [VIDEO] Step 4: Generate audio...");
            for (var slide : slides) {
                try {
                    String script = slide.getAudioScript();
                    if (script == null || script.isBlank()) {
                        script = slide.getTitle() + ". "
                                + (slide.getBody() != null ? slide.getBody().replaceAll("[•\\-*]", "") : "");
                    }
                    Path audioPath = workDir.resolve("audio")
                            .resolve(String.format("slide_%02d.wav", slide.getIndex() + 1));
                    double duration = ttsService.generateVideoTts(ttsService.prepareTtsText(script), audioPath);
                    slide.setAudioPath(audioPath.toString());
                    slide.setAudioDuration(duration);
                    slide.setAudioReady(true);
                    log.info("  ✅ Audio {}: {:.1}s", slide.getIndex() + 1, duration);
                    Thread.sleep(2500);
                } catch (Exception e) {
                    log.error("  ❌ Audio {}: {}", slide.getIndex() + 1, e.getMessage());
                }
            }

            // Step 5: Create clips
            log.info("🎬 [VIDEO] Step 5: Create clips...");
            List<Path> clipPaths = new ArrayList<>();
            for (var slide : slides) {
                if (slide.isImageReady() && slide.isAudioReady()) {
                    Path clipPath = workDir.resolve("clips")
                            .resolve(String.format("clip_%02d.mp4", slide.getIndex() + 1));
                    if (createClip(slide.getImagePath(), slide.getAudioPath(), slide.getAudioDuration(), clipPath)) {
                        clipPaths.add(clipPath);
                    }
                }
            }

            // Step 6: Merge
            Path finalVideo = workDir.resolve("final.mp4");
            if (!clipPaths.isEmpty()) {
                log.info("🎬 [VIDEO] Step 6: Merge {} clips...", clipPaths.size());
                mergeClips(clipPaths, workDir, finalVideo);
            }

            // Finalize
            if (Files.exists(finalVideo)) {
                String fileName = "video_" + sessionId + ".mp4";
                Path destPath = Paths.get("uploads", "videos", fileName);
                Files.move(finalVideo, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                cleanupDirectory(workDir);

                double totalDuration = slides.stream().mapToDouble(s -> s.getAudioDuration()).sum();
                String videoUrl = "/uploads/videos/" + fileName;

                // Lấy aiSet để link với VideoAsset (title đã được cập nhật ở Step 2)
                NotebookAiSet finalAiSet = aiSetRepository.findById(aiSetId).orElse(null);

                VideoAsset videoAsset = VideoAsset.builder()
                        .notebook(notebook).createdBy(user).style(templateName)
                        .textSource(videoTitle).videoUrl(videoUrl)
                        .durationSeconds((int) totalDuration).createdAt(OffsetDateTime.now())
                        .notebookAiSets(finalAiSet).build();
                VideoAsset savedVideoAsset = videoAssetRepository.save(videoAsset);

                // Flush để đảm bảo tất cả thay đổi được persist
                videoAssetRepository.flush();

                Map<String, Object> stats = Map.of(
                        "slideCount", slides.size(), "clipCount", clipPaths.size(),
                        "title", videoTitle, "videoUrl", videoUrl,
                        "videoAssetId",
                        savedVideoAsset.getId() != null ? savedVideoAsset.getId().toString() : "unknown",
                        "totalDuration", totalDuration);
                statusService.markDone(aiSetId, stats);
                log.info("🎉 [VIDEO] Done! {}", destPath);
            } else {
                statusService.markFailed(aiSetId, "Video merge failed");
            }

        } catch (Exception e) {
            statusService.markFailed(aiSetId, "Error: " + e.getMessage());
            log.error("❌ [VIDEO] {}", e.getMessage(), e);
        }
    }

    private void cleanupDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir).sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("⚠️ Cleanup failed: {}", e.getMessage());
        }
    }

    public String buildVideoPrompt(String summary, int slides, String extra) {
        String additional = (extra != null && !extra.isBlank()) ? "\nYêu cầu thêm: " + extra : "";
        return String.format(
                """
                        Bạn là YouTuber giáo dục nổi tiếng, tạo video giải thích dễ hiểu và cuốn hút.

                        TẠO SCRIPT VIDEO GỒM %d SLIDES từ nội dung sau:
                        ---
                        %s
                        ---%s

                        THÔNG TIN KÊNH:
                        - Video do nhóm F4 phát triển
                        - EduGenius Đại học Vinh - Công cụ học tập thông minh

                        QUY TẮC QUAN TRỌNG:
                        1. VIDEO PHẢI CÓ FLOW LIÊN TỤC - mỗi slide nối tiếp slide trước như một câu chuyện
                        2. Slide ĐẦU TIÊN (INTRO): Chào đón, giới thiệu nhóm F4 phát triển video
                        3. Slide CUỐI CÙNG (OUTRO): Tóm tắt, cảm ơn, kêu gọi like/subscribe EduGenius Đại học Vinh
                        4. Các slide giữa giải thích từng ý TUẦN TỰ

                        CHO MỖI SLIDE:
                        - title: Tiêu đề ngắn gọn (tối đa 10 từ)
                        - body: 2-3 bullet points ngắn
                        - imagePrompt: Mô tả hình ảnh minh họa (tiếng Anh, cartoon style)
                        - audioScript: Script đọc (80-120 từ, xưng "mình" với "các bạn")

                        TRẢ VỀ JSON (KHÔNG có markdown):
                        {"title": "Tên video", "slides": [{"title": "...", "body": "...", "imagePrompt": "...", "audioScript": "..."}]}
                        """,
                slides, summary, additional);
    }

    private boolean createClip(String img, String audio, double duration, Path out) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-loop", "1", "-i", img, "-i", audio,
                    "-c:v", "libx264", "-tune", "stillimage", "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p", "-t", String.format("%.2f", duration), out.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor() == 0 && Files.exists(out);
        } catch (Exception e) {
            return false;
        }
    }

    private void mergeClips(List<Path> clips, Path dir, Path out) {
        try {
            Path list = dir.resolve("clips.txt");
            Files.write(list, clips.stream().map(p -> "file '" + p.toAbsolutePath() + "'").toList());
            new ProcessBuilder("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", list.toString(), "-c", "copy",
                    out.toString())
                    .redirectErrorStream(true).start().waitFor();
        } catch (Exception e) {
            log.error("Merge error: {}", e.getMessage());
        }
    }

    /**
     * Chuyển đổi Object thành String an toàn.
     * Xử lý trường hợp LLM trả về ArrayList thay vì String.
     */
    @SuppressWarnings("unchecked")
    private String extractString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return String.join("\n• ", list.stream()
                    .map(Object::toString)
                    .toList());
        }
        return value.toString();
    }
}
