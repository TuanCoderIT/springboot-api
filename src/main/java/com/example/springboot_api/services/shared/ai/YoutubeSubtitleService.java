package com.example.springboot_api.services.shared.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service extracting subtitles from YouTube videos using yt-dlp.
 * Requirement: yt-dlp must be installed on the system path.
 */
@Service
public class YoutubeSubtitleService {

    private static final Logger logger = LoggerFactory.getLogger(YoutubeSubtitleService.class);
    private static final String YT_DLP_COMMAND = "yt-dlp";

    // Regex để nhận diện dòng timestamp trong VTT (ví dụ: 00:00:00.000 -->
    // 00:00:03.000)
    private static final Pattern TIMESTAMP_PATTERN = Pattern
            .compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s-->\\s(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*");

    /**
     * DTO chứa thông tin một đoạn phụ đề với timestamp.
     */
    public record SubtitleSegment(
            String startTime, // "00:00:01.500"
            String endTime, // "00:00:04.200"
            String text // "Nội dung phụ đề"
    ) {
    }

    /**
     * Internal record để xử lý segment thô.
     */
    private record RawSegment(String startTime, String text) {
    }

    /**
     * DTO kết quả trả về: text đã format có timestamp và full text thuần.
     */
    public record SubtitleResult(
            String formattedText, // "[00:00:01] Nội dung...\n[00:00:05] Tiếp theo..."
            String fullText // "Nội dung... Tiếp theo..."
    ) {
    }

    /**
     * Tải và trích xuất phụ đề từ video YouTube (có timestamp).
     *
     * @param videoUrl URL của video YouTube.
     * @return SubtitleResult chứa danh sách segments và full text.
     */
    public SubtitleResult extractSubtitleWithTimestamps(String videoUrl) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("yt_subs_" + UUID.randomUUID());
        File workingDir = tempDir.toFile();

        try {
            logger.info("Starting subtitle extraction for URL: {}", videoUrl);

            ProcessBuilder pb = new ProcessBuilder(
                    YT_DLP_COMMAND,
                    "--write-auto-sub",
                    "--skip-download",
                    "--sub-format", "vtt",
                    "--sub-lang", "vi", // Ưu tiên phụ đề tiếng Việt
                    "--cookies-from-browser", "chrome",
                    "--no-playlist",
                    "--output", "subtitle",
                    videoUrl);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("yt-dlp output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("yt-dlp failed with exit code: " + exitCode);
            }

            File[] files = workingDir.listFiles((dir, name) -> name.endsWith(".vtt"));

            if (files == null || files.length == 0) {
                logger.warn("No .vtt file found after yt-dlp execution.");
                return new SubtitleResult("", "");
            }

            File subtitleFile = files[0];
            logger.info("Subtitle file found: {}", subtitleFile.getName());

            return parseVttFileWithTimestamps(subtitleFile.toPath());

        } finally {
            deleteDirectory(workingDir);
        }
    }

    /**
     * Tải và trích xuất phụ đề từ video YouTube (chỉ text, không timestamp).
     */
    public String extractSubtitle(String videoUrl) throws IOException, InterruptedException {
        SubtitleResult result = extractSubtitleWithTimestamps(videoUrl);
        return result.fullText();
    }

    /**
     * Parse file VTT thành text có timestamp inline.
     */
    private SubtitleResult parseVttFileWithTimestamps(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        // Bước 1: Thu thập tất cả segments thô
        List<RawSegment> rawSegments = new ArrayList<>();
        String currentStartTime = null;
        StringBuilder currentText = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                if (currentStartTime != null && !currentText.isEmpty()) {
                    String text = currentText.toString().trim();
                    rawSegments.add(new RawSegment(currentStartTime, text));
                }
                currentStartTime = null;
                currentText = new StringBuilder();
                continue;
            }

            if (trimmed.equals("WEBVTT") || trimmed.startsWith("Kind:") || trimmed.startsWith("Language:")) {
                continue;
            }

            Matcher matcher = TIMESTAMP_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                currentStartTime = matcher.group(1);
                continue;
            }

            String cleanText = trimmed.replaceAll("<[^>]*>", "");
            if (!cleanText.isEmpty()) {
                if (!currentText.isEmpty()) {
                    currentText.append(" ");
                }
                currentText.append(cleanText);
            }
        }

        // Xử lý segment cuối
        if (currentStartTime != null && !currentText.isEmpty()) {
            rawSegments.add(new RawSegment(currentStartTime, currentText.toString().trim()));
        }

        // Bước 2: Gộp các segment chồng lấp (rolling subtitles)
        // Logic: Nếu segment mới CHỨA segment trước đó (hoặc ngược lại), chỉ giữ bản
        // dài nhất
        List<RawSegment> mergedSegments = new ArrayList<>();

        for (RawSegment current : rawSegments) {
            if (mergedSegments.isEmpty()) {
                mergedSegments.add(current);
                continue;
            }

            RawSegment last = mergedSegments.get(mergedSegments.size() - 1);

            // Nếu text hiện tại CHỨA text trước đó (rolling forward) -> thay thế
            if (current.text.contains(last.text)) {
                mergedSegments.set(mergedSegments.size() - 1, current);
            }
            // Nếu text trước đó CHỨA text hiện tại -> bỏ qua (duplicate)
            else if (last.text.contains(current.text)) {
                // Skip - already have longer version
            }
            // Nếu text khác hoàn toàn -> thêm mới
            else {
                mergedSegments.add(current);
            }
        }

        // Bước 3: Build output
        StringBuilder formattedText = new StringBuilder();
        StringBuilder fullText = new StringBuilder();

        for (RawSegment seg : mergedSegments) {
            formattedText.append("[").append(formatTime(seg.startTime)).append("] ")
                    .append(seg.text).append("\n");
            fullText.append(seg.text).append(" ");
        }

        return new SubtitleResult(formattedText.toString().trim(), fullText.toString().trim());
    }

    /**
     * Chuyển timestamp VTT (00:01:23.456) thành dạng ngắn gọn (00:01:23).
     */
    private String formatTime(String vttTime) {
        // Bỏ phần milliseconds: 00:01:23.456 -> 00:01:23
        if (vttTime != null && vttTime.length() > 8) {
            return vttTime.substring(0, 8);
        }
        return vttTime;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
