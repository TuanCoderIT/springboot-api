package com.example.springboot_api.services.shared.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springboot_api.models.SupportedLanguage;
import com.example.springboot_api.repositories.shared.SupportedLanguageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gọi Piston API để chạy code.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PistonService {

    private final SupportedLanguageRepository languageRepository;

    @Value("${piston.url:http://localhost:2000}")
    private String pistonUrl;

    @Value("${piston.timeout:20}")
    private int timeout;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                    .baseUrl(pistonUrl)
                    .build();
        }
        return webClient;
    }

    /**
     * Chạy code qua Piston API.
     *
     * @param language Tên ngôn ngữ (python, javascript, ...)
     * @param version  Version (3.10.0, ...)
     * @param files    Danh sách files [{name: "main.py", content:
     *                 "print('hello')"}]
     * @param stdin    Input cho program
     * @return Response từ Piston
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> runCode(String language, String version,
            List<Map<String, String>> files,
            String stdin) {
        log.info("🚀 [PISTON] Running code: language={}, version={}, files={}",
                language, version, files.size());

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("language", language);
        if (version != null) {
            payload.put("version", version);
        }
        payload.put("files", files);
        if (stdin != null && !stdin.isEmpty()) {
            payload.put("stdin", stdin);
        }

        try {
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/api/v2/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("✅ [PISTON] Execution completed");
            return response;
        } catch (Exception e) {
            log.error("❌ [PISTON] Execution failed: {}", e.getMessage());
            return Map.of(
                    "error", true,
                    "message", e.getMessage());
        }
    }

    /**
     * Lấy danh sách runtime được Piston hỗ trợ.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRuntimes() {
        log.info("📋 [PISTON] Getting runtimes...");
        try {
            List<Map<String, Object>> runtimes = getWebClient()
                    .get()
                    .uri("/api/v2/runtimes")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ [PISTON] Found {} runtimes", runtimes != null ? runtimes.size() : 0);
            return runtimes;
        } catch (Exception e) {
            log.error("❌ [PISTON] Failed to get runtimes: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Sync danh sách ngôn ngữ từ Piston vào database.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public int syncLanguages() {
        log.info("🔄 [PISTON] Syncing languages from Piston...");

        List<Map<String, Object>> runtimes = getRuntimes();
        if (runtimes.isEmpty()) {
            log.warn("⚠️ [PISTON] No runtimes found");
            return 0;
        }

        int inserted = 0;
        for (Map<String, Object> rt : runtimes) {
            String name = (String) rt.get("language");
            String version = (String) rt.get("version");
            List<String> aliases = (List<String>) rt.get("aliases");
            String runtime = (String) rt.get("runtime");

            // Check exists
            if (languageRepository.findByNameAndVersion(name, version).isPresent()) {
                continue;
            }

            SupportedLanguage lang = SupportedLanguage.builder()
                    .name(name)
                    .version(version)
                    .aliases(aliases)
                    .runtime(runtime)
                    .isActive(true)
                    .lastSync(LocalDateTime.now())
                    .build();

            languageRepository.save(lang);
            inserted++;
        }

        log.info("✅ [PISTON] Synced {} new languages", inserted);
        return inserted;
    }
}
