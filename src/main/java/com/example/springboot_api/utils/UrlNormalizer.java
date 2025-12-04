package com.example.springboot_api.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class để normalize URL (storageUrl, thumbnailUrl, avatarUrl)
 */
@Component
public class UrlNormalizer {

    @Value("${file.base-url:http://localhost:8386}")
    private String baseUrl;

    public String normalizeToFull(String url) {
        if (url == null) {
            return null;
        }
        return baseUrl + url;
    }

}