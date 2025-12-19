package com.example.springboot_api.services.shared.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý parse JSON response từ LLM.
 * Bao gồm các utility để strip markdown wrapper và extract JSON.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsonParsingService {

    private final ObjectMapper objectMapper;

    /**
     * Trích xuất JSON array từ LLM response (có thể có markdown wrapper).
     * Hỗ trợ: [...], ```json ... ```, plain text có chứa array.
     */
    public String extractJsonArrayFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String trimmed = response.trim();

        // Đã là valid JSON array
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        // Tìm trong code block ```json ... ``` hoặc ``` ... ```
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = codeBlockPattern.matcher(trimmed);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                return content;
            }
        }

        // Tìm array pattern [ ... ]
        int startIndex = trimmed.indexOf('[');
        int endIndex = trimmed.lastIndexOf(']');
        if (startIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1);
        }

        return trimmed;
    }

    /**
     * Trích xuất JSON object từ LLM response (có thể có markdown wrapper).
     * Hỗ trợ: {...}, ```json ... ```, plain text có chứa object.
     */
    public String extractJsonObjectFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String trimmed = response.trim();

        // Đã là valid JSON object
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // Tìm trong code block ```json ... ``` hoặc ``` ... ```
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = codeBlockPattern.matcher(trimmed);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                return content;
            }
        }

        // Tìm object pattern { ... }
        int startIndex = trimmed.indexOf('{');
        int endIndex = trimmed.lastIndexOf('}');
        if (startIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1);
        }

        return trimmed;
    }

    /**
     * Parse JSON response từ LLM thành list (quiz, flashcard, etc.).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseJsonArray(String llmResponse) {
        try {
            String jsonString = extractJsonArrayFromResponse(llmResponse);
            if (jsonString == null) {
                return null;
            }
            return objectMapper.readValue(jsonString, List.class);
        } catch (Exception e) {
            log.error("❌ Lỗi parse JSON array: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse JSON response từ LLM thành Map (mindmap, suggestion, etc.).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJsonObject(String llmResponse) {
        try {
            String jsonString = extractJsonObjectFromResponse(llmResponse);
            if (jsonString == null) {
                return null;
            }
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            log.error("❌ Lỗi parse JSON object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse mindmap JSON từ LLM response.
     * Wrap lại nếu cần thiết để đảm bảo có root node.
     */
    public Map<String, Object> parseMindmapJson(String llmResponse) {
        try {
            String cleaned = extractJsonObjectFromResponse(llmResponse);
            if (cleaned == null || cleaned.isBlank()) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(cleaned, Map.class);

            // Nếu không có root, thử wrap lại
            if (!data.containsKey("root") && data.containsKey("id") && data.containsKey("title")) {
                Map<String, Object> wrapped = new HashMap<>();
                wrapped.put("root", data);
                return wrapped;
            }

            return data.containsKey("root") ? data : null;
        } catch (Exception e) {
            log.error("❌ Lỗi parse mindmap JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse suggestion JSON từ LLM response.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseSuggestionJson(String llmResponse) {
        try {
            String cleaned = extractJsonObjectFromResponse(llmResponse);
            if (cleaned == null || cleaned.isBlank()) {
                // Thử parse như array
                cleaned = extractJsonArrayFromResponse(llmResponse);
                if (cleaned != null && cleaned.trim().startsWith("[")) {
                    return objectMapper.readValue(cleaned, List.class);
                }
                return null;
            }

            Map<String, Object> data = objectMapper.readValue(cleaned, Map.class);
            if (data.containsKey("suggestions")) {
                return (List<Map<String, Object>>) data.get("suggestions");
            }

            return null;
        } catch (Exception e) {
            log.error("❌ Lỗi parse suggestion JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse video JSON từ LLM response.
     * Hỗ trợ cả format object {title, slides} và array trực tiếp.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseVideoJson(String llmResponse) {
        try {
            String json = extractJsonArrayFromResponse(llmResponse);
            if (json == null) {
                json = extractJsonObjectFromResponse(llmResponse);
            }
            if (json == null)
                return null;

            json = json.trim();
            if (json.startsWith("[")) {
                List<Map<String, Object>> slides = objectMapper.readValue(json, List.class);
                return Map.of("title", "Video", "slides", slides);
            }
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("❌ Lỗi parse video JSON: {}", e.getMessage());
            return null;
        }
    }
}
