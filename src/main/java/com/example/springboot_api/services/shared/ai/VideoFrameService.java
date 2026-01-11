package com.example.springboot_api.services.shared.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Service render video frames kiểu NotebookLM.
 * Features:
 * - Intro frame
 * - Content frames với 2 AI images
 * - Ending frame
 * - Kawaii/playful style
 */
@Slf4j
@Service
public class VideoFrameService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8386}")
    private String baseUrl;

    private final AiImageGeneratorService imageGeneratorService;

    private com.microsoft.playwright.Playwright playwright;
    private com.microsoft.playwright.Browser browser;

    private static final int FRAME_WIDTH = 1920;
    private static final int FRAME_HEIGHT = 1080;
    private static final String BRANDING = "EduGenius Đại học Vinh";

    public VideoFrameService(AiImageGeneratorService imageGeneratorService) {
        this.imageGeneratorService = imageGeneratorService;
    }

    @PostConstruct
    public void init() {
        try {
            playwright = com.microsoft.playwright.Playwright.create();
            browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            log.info("✅ VideoFrameService ready");
        } catch (Exception e) {
            log.error("❌ VideoFrameService init failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (browser != null)
                browser.close();
            if (playwright != null)
                playwright.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Render video frames - trả về danh sách base64 PNG.
     * LLM đã tạo sẵn intro (slide đầu) và ending (slide cuối) trong danh sách.
     */
    public List<String> renderVideoFrames(String videoTitle, List<FrameContent> frames, boolean generateImages)
            throws IOException {
        List<String> base64Images = new ArrayList<>();

        var context = browser.newContext(
                new com.microsoft.playwright.Browser.NewContextOptions()
                        .setViewportSize(FRAME_WIDTH, FRAME_HEIGHT));
        var page = context.newPage();

        int total = frames.size();

        try {
            for (int i = 0; i < frames.size(); i++) {
                FrameContent frame = frames.get(i);
                log.info("🎨 Rendering frame {}/{}...", i + 1, total);

                // Generate images if enabled
                String image1 = null, image2 = null;
                if (generateImages && frame.getImagePrompt() != null) {
                    String basePrompt = frame.getImagePrompt();
                    image1 = imageGeneratorService
                            .generateSlideImageBase64(basePrompt + ", illustration style, vibrant colors");
                    Thread.sleep(2000); // Rate limit
                    image2 = imageGeneratorService
                            .generateSlideImageBase64(basePrompt + ", different angle, cartoon style");
                }

                String html = buildContentFrame(frame, image1, image2, i + 1, total);
                page.setContent(html);

                // Trả về base64 thay vì lưu file
                byte[] bytes = page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                        .setType(com.microsoft.playwright.options.ScreenshotType.PNG));
                base64Images.add(java.util.Base64.getEncoder().encodeToString(bytes));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            context.close();
        }

        log.info("✅ Rendered {} frames total", base64Images.size());
        return base64Images;
    }

    /**
     * Backward compatible method.
     */
    public List<String> renderVideoFrames(List<FrameContent> frames, boolean generateImages) throws IOException {
        String title = frames.isEmpty() ? "Video Overview" : frames.get(0).getTitle();
        return renderVideoFrames(title, frames, generateImages);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTRO FRAME
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildIntroFrame(String title, int totalSlides) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                @import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap');
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                body {
                                    width: %dpx; height: %dpx;
                                    font-family: 'Nunito', sans-serif;
                                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    overflow: hidden;
                                    position: relative;
                                }
                                .circles {
                                    position: absolute;
                                    width: 100%%; height: 100%%;
                                }
                                .circle { position: absolute; border-radius: 50%%; opacity: 0.2; }
                                .c1 { width: 400px; height: 400px; background: #fff; top: -100px; left: -100px; }
                                .c2 { width: 300px; height: 300px; background: #fff; bottom: -80px; right: -80px; }
                                .c3 { width: 200px; height: 200px; background: #f39c12; top: 50%%; right: 10%%; }
                                .c4 { width: 150px; height: 150px; background: #e74c3c; bottom: 20%%; left: 15%%; }

                                .content {
                                    text-align: center;
                                    z-index: 10;
                                    padding: 60px;
                                }
                                .emoji { font-size: 100px; margin-bottom: 30px; }
                                .title {
                                    font-size: 80px;
                                    font-weight: 900;
                                    color: #fff;
                                    line-height: 1.1;
                                    margin-bottom: 30px;
                                    text-shadow: 0 10px 40px rgba(0,0,0,0.3);
                                    max-width: 1400px;
                                }
                                .subtitle {
                                    font-size: 32px;
                                    color: rgba(255,255,255,0.9);
                                    font-weight: 600;
                                }
                                .meta {
                                    margin-top: 50px;
                                    display: flex;
                                    gap: 30px;
                                    justify-content: center;
                                }
                                .meta-item {
                                    background: rgba(255,255,255,0.2);
                                    padding: 16px 32px;
                                    border-radius: 50px;
                                    font-size: 20px;
                                    font-weight: 700;
                                    color: #fff;
                                }
                                .branding {
                                    position: absolute;
                                    bottom: 50px;
                                    font-size: 24px;
                                    font-weight: 800;
                                    color: rgba(255,255,255,0.8);
                                }
                            </style>
                        </head>
                        <body>
                            <div class="circles">
                                <div class="circle c1"></div>
                                <div class="circle c2"></div>
                                <div class="circle c3"></div>
                                <div class="circle c4"></div>
                            </div>
                            <div class="content">
                                <div class="emoji">🎬</div>
                                <h1 class="title">%s</h1>
                                <p class="subtitle">Video tổng quan được tạo bởi AI</p>
                                <div class="meta">
                                    <div class="meta-item">📊 %d Slides</div>
                                    <div class="meta-item">🤖 AI Generated</div>
                                </div>
                            </div>
                            <div class="branding">📚 %s</div>
                        </body>
                        </html>
                        """,
                FRAME_WIDTH, FRAME_HEIGHT, escapeHtml(title), totalSlides, BRANDING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTENT FRAME - 2 images + short text
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildContentFrame(FrameContent frame, String img1, String img2, int num, int total) {
        java.util.Random random = new java.util.Random();

        String[] bgColors = {
                "linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)",
                "linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)",
                "linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)",
                "linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)",
                "linear-gradient(135deg, #fbc7d4 0%, #9796f0 100%)",
                "linear-gradient(135deg, #a1ffce 0%, #faffd1 100%)",
                "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
                "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
                "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)",
        };
        String[] accents = { "#ff6b6b", "#4ecdc4", "#a55eea", "#3498db", "#e056fd", "#26de81", "#f39c12", "#e74c3c",
                "#9b59b6", "#1abc9c" };

        // Random chọn màu sắc cho mỗi frame
        String bg = bgColors[random.nextInt(bgColors.length)];
        String accent = accents[random.nextInt(accents.length)];

        String img1Html = img1 != null
                ? String.format("<img src=\"data:image/png;base64,%s\" />", img1)
                : "<div class=\"placeholder\">🖼️</div>";
        String img2Html = img2 != null
                ? String.format("<img src=\"data:image/png;base64,%s\" />", img2)
                : "<div class=\"placeholder\">🖼️</div>";

        // Lấy tối đa 2 bullet points
        String shortBody = getShortBody(frame.getBody(), 2);

        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                @import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap');
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                body {
                                    width: %dpx; height: %dpx;
                                    font-family: 'Nunito', sans-serif;
                                    background: %s;
                                    position: relative;
                                    overflow: hidden;
                                }

                                /* Top section - Title */
                                .top {
                                    position: absolute;
                                    top: 0; left: 0; right: 0;
                                    padding: 50px 80px;
                                    display: flex;
                                    align-items: center;
                                    gap: 30px;
                                }
                                .step {
                                    width: 70px; height: 70px;
                                    background: %s;
                                    border-radius: 20px;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 28px;
                                    font-weight: 900;
                                    color: #fff;
                                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                                }
                                .title {
                                    flex: 1;
                                    font-size: 52px;
                                    font-weight: 900;
                                    color: #2d3436;
                                    line-height: 1.1;
                                }

                                /* Middle - Images */
                                .images {
                                    position: absolute;
                                    top: 180px; left: 80px; right: 80px;
                                    height: 450px;
                                    display: flex;
                                    gap: 40px;
                                }
                                .img-card {
                                    flex: 1;
                                    background: #fff;
                                    border-radius: 30px;
                                    padding: 15px;
                                    box-shadow: 0 20px 60px rgba(0,0,0,0.15);
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    overflow: hidden;
                                }
                                .img-card img {
                                    width: 100%%;
                                    height: 100%%;
                                    object-fit: cover;
                                    border-radius: 20px;
                                }
                                .placeholder {
                                    font-size: 80px;
                                    opacity: 0.3;
                                }

                                /* Bottom - Short text */
                                .bottom {
                                    position: absolute;
                                    bottom: 60px; left: 80px; right: 80px;
                                    display: flex;
                                    gap: 30px;
                                }
                                .point {
                                    flex: 1;
                                    background: rgba(255,255,255,0.95);
                                    border-radius: 24px;
                                    padding: 28px 35px;
                                    display: flex;
                                    align-items: center;
                                    gap: 20px;
                                    box-shadow: 0 10px 40px rgba(0,0,0,0.08);
                                }
                                .point-icon {
                                    width: 56px; height: 56px;
                                    background: %s;
                                    border-radius: 16px;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 26px;
                                    flex-shrink: 0;
                                }
                                .point-text {
                                    font-size: 22px;
                                    font-weight: 600;
                                    color: #2d3436;
                                    line-height: 1.3;
                                }

                                /* Progress */
                                .progress {
                                    position: absolute;
                                    bottom: 25px; right: 80px;
                                    font-size: 18px;
                                    font-weight: 700;
                                    color: rgba(0,0,0,0.4);
                                    background: rgba(255,255,255,0.8);
                                    padding: 10px 20px;
                                    border-radius: 20px;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="top">
                                <div class="step">%02d</div>
                                <h1 class="title">%s</h1>
                            </div>

                            <div class="images">
                                <div class="img-card">%s</div>
                                <div class="img-card">%s</div>
                            </div>

                            <div class="bottom">%s</div>

                            <div class="progress">%d / %d</div>
                        </body>
                        </html>
                        """,
                FRAME_WIDTH, FRAME_HEIGHT, bg, accent, accent,
                num - 1, escapeHtml(frame.getTitle()),
                img1Html, img2Html,
                shortBody,
                num, total);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ENDING FRAME
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildEndingFrame(String title, int total) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                @import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap');
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                body {
                                    width: %dpx; height: %dpx;
                                    font-family: 'Nunito', sans-serif;
                                    background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%);
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    overflow: hidden;
                                    position: relative;
                                }
                                .circles { position: absolute; width: 100%%; height: 100%%; }
                                .circle { position: absolute; border-radius: 50%%; background: #fff; opacity: 0.15; }
                                .c1 { width: 500px; height: 500px; top: -150px; right: -150px; }
                                .c2 { width: 350px; height: 350px; bottom: -100px; left: -100px; }

                                .content { text-align: center; z-index: 10; }
                                .emoji { font-size: 120px; margin-bottom: 30px; }
                                .thanks {
                                    font-size: 90px;
                                    font-weight: 900;
                                    color: #fff;
                                    margin-bottom: 20px;
                                    text-shadow: 0 10px 40px rgba(0,0,0,0.2);
                                }
                                .subtitle {
                                    font-size: 32px;
                                    color: rgba(255,255,255,0.9);
                                    margin-bottom: 50px;
                                }
                                .cta {
                                    display: flex;
                                    gap: 30px;
                                    justify-content: center;
                                }
                                .btn {
                                    background: rgba(255,255,255,0.25);
                                    padding: 20px 40px;
                                    border-radius: 50px;
                                    font-size: 22px;
                                    font-weight: 700;
                                    color: #fff;
                                }
                                .branding {
                                    position: absolute;
                                    bottom: 50px;
                                    font-size: 28px;
                                    font-weight: 800;
                                    color: #fff;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="circles">
                                <div class="circle c1"></div>
                                <div class="circle c2"></div>
                            </div>
                            <div class="content">
                                <div class="emoji">🎉</div>
                                <h1 class="thanks">Cảm ơn đã xem!</h1>
                                <p class="subtitle">%s</p>
                                <div class="cta">
                                    <div class="btn">👍 Thích video</div>
                                    <div class="btn">🔔 Đăng ký</div>
                                </div>
                            </div>
                            <div class="branding">📚 %s</div>
                        </body>
                        </html>
                        """,
                FRAME_WIDTH, FRAME_HEIGHT, escapeHtml(title), BRANDING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private String getShortBody(String body, int maxPoints) {
        if (body == null || body.isBlank())
            return "";

        StringBuilder html = new StringBuilder();
        String[] lines = body.split("\n");
        String[] icons = { "💡", "🎯", "⚡", "✨" };

        int count = 0;
        for (String line : lines) {
            line = line.trim().replaceFirst("^[•\\-*]\\s*", "");
            if (line.isEmpty())
                continue;
            if (count >= maxPoints)
                break;

            // Rút gọn nếu quá dài
            if (line.length() > 60)
                line = line.substring(0, 57) + "...";

            html.append(String.format("""
                    <div class="point">
                        <div class="point-icon">%s</div>
                        <div class="point-text">%s</div>
                    </div>
                    """, icons[count % icons.length], escapeHtml(line)));
            count++;
        }
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FrameContent {
        private String title;
        private String body;
        private String imagePrompt;
        private String audioScript; // Script cho TTS
    }
}
