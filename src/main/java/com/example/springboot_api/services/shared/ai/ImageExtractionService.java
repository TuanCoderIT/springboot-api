package com.example.springboot_api.services.shared.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Service để extract hình ảnh từ URL.
 * Nhận URL → trả về imageUrl (nếu tìm được) bằng cách:
 * 1. Gọi HTTP lấy HTML
 * 2. Dò lần lượt các thẻ meta chuẩn: og:image, twitter:image, v.v.
 * 3. Nếu không có, fallback sang <link rel="image_src"> hoặc <img> đầu tiên
 */
@Service
public class ImageExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ImageExtractionService.class);
    private static final int MAX_HTML_SIZE = 2 * 1024 * 1024; // 2MB max
    private static final int TIMEOUT_SECONDS = 10;

    private final WebClient webClient;

    // Patterns để tìm các thẻ meta
    private static final Pattern OG_IMAGE_PATTERN = Pattern
            .compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TWITTER_IMAGE_PATTERN = Pattern
            .compile("<meta\\s+name=[\"']twitter:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TWITTER_IMAGE_ALT_PATTERN = Pattern
            .compile("<meta\\s+property=[\"']twitter:image[\"']\\s+content=[\"']([^\"']+)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_IMAGE_SRC_PATTERN = Pattern
            .compile("<link\\s+rel=[\"']image_src[\"']\\s+href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_TAG_PATTERN = Pattern
            .compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);

    public ImageExtractionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_HTML_SIZE))
                .build();
    }

    /**
     * Extract hình ảnh từ URL.
     * 
     * @param url URL của trang web
     * @return URL của hình ảnh nếu tìm được, null nếu không tìm thấy
     */
    public String extractImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            // Gọi HTTP lấy HTML
            String html = fetchHtml(url);
            if (html == null || html.trim().isEmpty()) {
                return null;
            }

            // Dò lần lượt các thẻ meta chuẩn
            String imageUrl = findImageFromMetaTags(html, url);
            if (imageUrl != null) {
                return imageUrl;
            }

            // Fallback sang <link rel="image_src">
            imageUrl = findImageFromLinkTag(html, url);
            if (imageUrl != null) {
                return imageUrl;
            }

            // Fallback sang <img> đầu tiên
            imageUrl = findImageFromImgTag(html, url);
            return imageUrl;

        } catch (Exception e) {
            log.warn("Error extracting image from URL: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Gọi HTTP GET để lấy HTML từ URL.
     */
    private String fetchHtml(String url) {
        try {
            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));
        } catch (WebClientResponseException e) {
            log.warn("HTTP error when fetching URL: {} - status: {}", url, e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("Error fetching URL: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Tìm hình ảnh từ các thẻ meta chuẩn: og:image, twitter:image.
     */
    private String findImageFromMetaTags(String html, String baseUrl) {
        // Ưu tiên 1: og:image
        Matcher ogMatcher = OG_IMAGE_PATTERN.matcher(html);
        if (ogMatcher.find()) {
            String imageUrl = ogMatcher.group(1);
            return resolveUrl(imageUrl, baseUrl);
        }

        // Ưu tiên 2: twitter:image (name)
        Matcher twitterMatcher = TWITTER_IMAGE_PATTERN.matcher(html);
        if (twitterMatcher.find()) {
            String imageUrl = twitterMatcher.group(1);
            return resolveUrl(imageUrl, baseUrl);
        }

        // Ưu tiên 3: twitter:image (property)
        Matcher twitterAltMatcher = TWITTER_IMAGE_ALT_PATTERN.matcher(html);
        if (twitterAltMatcher.find()) {
            String imageUrl = twitterAltMatcher.group(1);
            return resolveUrl(imageUrl, baseUrl);
        }

        return null;
    }

    /**
     * Tìm hình ảnh từ <link rel="image_src">.
     */
    private String findImageFromLinkTag(String html, String baseUrl) {
        Matcher linkMatcher = LINK_IMAGE_SRC_PATTERN.matcher(html);
        if (linkMatcher.find()) {
            String imageUrl = linkMatcher.group(1);
            return resolveUrl(imageUrl, baseUrl);
        }
        return null;
    }

    /**
     * Tìm hình ảnh từ <img> đầu tiên.
     */
    private String findImageFromImgTag(String html, String baseUrl) {
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(html);
        if (imgMatcher.find()) {
            String imageUrl = imgMatcher.group(1);
            // Bỏ qua các hình ảnh quá nhỏ hoặc không phải hình ảnh thực sự
            if (isValidImageUrl(imageUrl)) {
                return resolveUrl(imageUrl, baseUrl);
            }
        }
        return null;
    }

    /**
     * Kiểm tra xem URL có phải là hình ảnh hợp lệ không.
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        // Bỏ qua data URI, base64, và các URL không phải hình ảnh
        if (lowerUrl.startsWith("data:") || lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("mailto:")) {
            return false;
        }

        // Kiểm tra extension
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png")
                || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg")
                || lowerUrl.contains("/image") || lowerUrl.contains("/img") || lowerUrl.contains("/photo");
    }

    /**
     * Resolve relative URL thành absolute URL.
     */
    private String resolveUrl(String imageUrl, String baseUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        // Nếu đã là absolute URL
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        // Nếu là protocol-relative URL (//example.com/image.jpg)
        if (imageUrl.startsWith("//")) {
            try {
                java.net.URI baseUri = java.net.URI.create(baseUrl);
                String protocol = baseUri.getScheme() != null ? baseUri.getScheme() : "https";
                return protocol + ":" + imageUrl;
            } catch (Exception e) {
                return "https:" + imageUrl;
            }
        }

        // Nếu là relative URL
        try {
            java.net.URI baseUri = java.net.URI.create(baseUrl);
            java.net.URI resolved = baseUri.resolve(imageUrl);
            return resolved.toString();
        } catch (Exception e) {
            log.warn("Error resolving URL: {} with base: {}", imageUrl, baseUrl);
            return imageUrl; // Return as-is if can't resolve
        }
    }
}
