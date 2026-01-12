package com.example.springboot_api.services.shared.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý tất cả logic gọi Gemini TTS API.
 * Bao gồm: single-speaker TTS, multi-speaker TTS, convert PCM to WAV.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiTtsService {

    private final TtsAssetRepository ttsAssetRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${google.api.gemini_key:}")
    private String geminiApiKeyConfig;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final String TTS_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent";

    /**
     * Gọi Gemini TTS API để tạo audio (single speaker).
     * 
     * @param text  Nội dung text cần TTS
     * @param voice Tên voice (mặc định: Aoede)
     * @return AudioResult chứa URL và duration
     */
    public AudioResult callGeminiTts(String text, String voice) throws Exception {
        String apiKey = getApiKey();
        String voiceName = (voice != null && !voice.isBlank()) ? voice : "Aoede";

        WebClient client = webClientBuilder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        String resp = client.post()
                .uri(TTS_API_URL)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
                        "generationConfig", Map.of(
                                "responseModalities", List.of("AUDIO"),
                                "speechConfig", Map.of("voiceConfig",
                                        Map.of("prebuiltVoiceConfig", Map.of("voiceName", voiceName))))))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode data = objectMapper.readTree(resp)
                .path("candidates").path(0).path("content").path("parts").path(0).path("inlineData");

        if (!data.has("data")) {
            throw new RuntimeException("No audio data in response");
        }

        byte[] pcm = java.util.Base64.getDecoder().decode(data.path("data").asText());
        byte[] wav = convertPcmToWav(pcm, 24000, 1, 16);
        int durationMs = (int) ((pcm.length / (24000.0 * 2)) * 1000);

        // Lưu file
        Path ttsDir = Paths.get(uploadDir, "tts");
        Files.createDirectories(ttsDir);
        String filename = "tts_" + UUID.randomUUID() + ".wav";
        Files.write(ttsDir.resolve(filename), wav);

        return new AudioResult("/uploads/tts/" + filename, durationMs);
    }

    /**
     * Gọi Gemini TTS cho video (lưu vào path cụ thể).
     */
    public double generateVideoTts(String text, Path outputPath) throws Exception {
        String apiKey = getApiKey();

        WebClient client = webClientBuilder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        String resp = client.post()
                .uri(TTS_API_URL)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
                        "generationConfig", Map.of(
                                "responseModalities", List.of("AUDIO"),
                                "speechConfig", Map.of("voiceConfig",
                                        Map.of("prebuiltVoiceConfig", Map.of("voiceName", "Aoede"))))))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode data = objectMapper.readTree(resp)
                .path("candidates").path(0).path("content").path("parts").path(0).path("inlineData");

        if (!data.has("data")) {
            throw new RuntimeException("No audio data in response");
        }

        byte[] pcm = java.util.Base64.getDecoder().decode(data.path("data").asText());
        Files.write(outputPath, convertPcmToWav(pcm, 24000, 1, 16));
        return (double) pcm.length / (24000.0 * 2);
    }

    /**
     * Gọi Gemini TTS multi-speaker cho podcast/audio overview.
     * 
     * @param script      Kịch bản hội thoại (Host: ...\nExpert: ...)
     * @param expertVoice Voice cho Expert (mặc định: Kore)
     * @param notebook    Notebook sở hữu asset
     * @param user        User tạo
     * @param aiSet       NotebookAiSet liên kết
     * @return TtsAsset đã lưu
     */
    @Transactional
    public TtsAsset generateMultiSpeakerTts(
            String script,
            String expertVoice,
            Notebook notebook,
            User user,
            NotebookAiSet aiSet) {

        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice script overview trống.");
        }

        String apiKey = getApiKey();

        String expertVoiceName = (expertVoice != null && !expertVoice.isBlank()) ? expertVoice : "Orus";
        String hostVoice = "Puck";

        // KHÔNG gọi prepareTtsText() vì nó xóa newlines,
        // khiến Gemini TTS không phân biệt được Host vs Expert
        // Chỉ trim và loại bỏ multiple spaces, GIỮ NGUYÊN newlines
        script = script.trim().replaceAll(" +", " ");

        log.info("🎙️ [TTS] Multi-speaker voices: Host={}, Expert={}", hostVoice, expertVoiceName);
        log.debug("🎙️ [TTS] Script:\n{}", script);

        String conversationPrompt = "TTS the following conversation between Host and Expert:\n" + script;

        try {
            WebClient client = webClientBuilder
                    .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();

            // Multi-speaker config
            Map<String, Object> hostConfig = Map.of(
                    "speaker", "Host",
                    "voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", hostVoice)));

            Map<String, Object> expertConfig = Map.of(
                    "speaker", "Expert",
                    "voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", expertVoiceName)));

            Map<String, Object> multiSpeakerConfig = Map.of(
                    "speakerVoiceConfigs", List.of(hostConfig, expertConfig));

            Map<String, Object> speechConfig = Map.of("multiSpeakerVoiceConfig", multiSpeakerConfig);
            Map<String, Object> generationConfig = Map.of(
                    "responseModalities", List.of("AUDIO"),
                    "speechConfig", speechConfig);

            Map<String, Object> part = Map.of("text", conversationPrompt);
            Map<String, Object> content = Map.of("parts", List.of(part));

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", generationConfig);

            String responseJson = client.post()
                    .uri(TTS_API_URL)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null || responseJson.isEmpty()) {
                throw new RuntimeException("Gemini TTS trả về response rỗng.");
            }

            ObjectNode responseNode = objectMapper.readValue(responseJson, ObjectNode.class);
            JsonNode parts = responseNode.path("candidates").path(0).path("content").path("parts").path(0);

            if (!parts.has("inlineData")) {
                String textRes = parts.path("text").asText();
                throw new RuntimeException("Gemini từ chối sinh Audio. Lý do/Text: " + textRes);
            }

            String audioBase64 = parts.path("inlineData").path("data").asText();
            byte[] pcmBytes = java.util.Base64.getDecoder().decode(audioBase64);
            byte[] wavBytes = convertPcmToWav(pcmBytes, 24000, 1, 16);

            // Lưu file
            Path baseDir = Paths.get(uploadDir);
            Path ttsDir = baseDir.resolve("tts");
            Files.createDirectories(ttsDir);

            String filename = "audio_podcast_" + UUID.randomUUID() + ".wav";
            Path outPath = ttsDir.resolve(filename);
            Files.write(outPath, wavBytes);

            TtsAsset asset = TtsAsset.builder()
                    .notebook(notebook)
                    .createdBy(user)
                    .voiceName(hostVoice + " & " + expertVoiceName)
                    .textSource(script)
                    .audioUrl("/uploads/tts/" + filename)
                    .createdAt(OffsetDateTime.now())
                    .notebookAiSets(aiSet)
                    .build();

            return ttsAssetRepository.save(asset);

        } catch (Exception ex) {
            log.error("Lỗi gọi Gemini TTS Multi-Speaker: {}", ex.getMessage());
            throw new RuntimeException("Lỗi gọi Gemini TTS Multi-Speaker: " + ex.getMessage(), ex);
        }
    }

    /**
     * Chuẩn hóa text cho TTS (loại bỏ newlines, nhiều spaces, etc.)
     */
    public String prepareTtsText(String script) {
        if (script == null)
            return "";

        String cleaned = script
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Tách câu rõ hơn để TTS đọc tự nhiên
        cleaned = cleaned.replaceAll("([a-zA-Z0-9]) ([A-Z])", "$1. $2");

        return cleaned;
    }

    /**
     * Convert raw PCM audio bytes to WAV format.
     * PCM format: signed 16-bit little-endian
     */
    public byte[] convertPcmToWav(byte[] pcmData, int sampleRate, int numChannels, int bitsPerSample) {
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // RIFF header
            baos.write("RIFF".getBytes());
            baos.write(intToLittleEndian(chunkSize, 4));
            baos.write("WAVE".getBytes());

            // fmt subchunk
            baos.write("fmt ".getBytes());
            baos.write(intToLittleEndian(16, 4));
            baos.write(intToLittleEndian(1, 2));
            baos.write(intToLittleEndian(numChannels, 2));
            baos.write(intToLittleEndian(sampleRate, 4));
            baos.write(intToLittleEndian(byteRate, 4));
            baos.write(intToLittleEndian(blockAlign, 2));
            baos.write(intToLittleEndian(bitsPerSample, 2));

            // data subchunk
            baos.write("data".getBytes());
            baos.write(intToLittleEndian(dataSize, 4));
            baos.write(pcmData);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi convert PCM to WAV: " + e.getMessage(), e);
        }
    }

    private byte[] intToLittleEndian(int value, int numBytes) {
        byte[] result = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            result[i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

    private String getApiKey() {
        String apiKey = geminiApiKeyConfig;
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GOOGLE_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình Gemini API Key");
        }
        return apiKey;
    }

    /**
     * Record chứa kết quả audio.
     */
    public record AudioResult(String url, int durationMs) {
    }
}
