package com.example.springboot_api.services.shared.ai.generation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.models.CodeExercise;
import com.example.springboot_api.models.CodeExerciseFile;
import com.example.springboot_api.models.CodeExerciseTestcase;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.SupportedLanguage;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.CodeExerciseFileRepository;
import com.example.springboot_api.repositories.shared.CodeExerciseRepository;
import com.example.springboot_api.repositories.shared.CodeExerciseTestcaseRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.SupportedLanguageRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiTaskProgressService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;
import com.example.springboot_api.services.shared.ai.PistonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service sinh bài tập code từ tài liệu bằng AI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExerciseGenerationService {

    private final AIModelService aiModelService;
    private final JsonParsingService jsonParsingService;
    private final PistonService pistonService;
    private final SupportedLanguageRepository languageRepository;
    private final CodeExerciseRepository exerciseRepository;
    private final CodeExerciseFileRepository fileRepository;
    private final CodeExerciseTestcaseRepository testcaseRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final AiTaskProgressService progressService;

    /**
     * Sinh bài tập code async.
     */
    @Async
    @Transactional
    public void generateAsync(UUID aiSetId, Notebook notebook, User user,
            String documentContent, int maxExercises,
            String language, String additionalReqs) {

        log.info("🚀 [CODE_EXERCISE] Starting generation for aiSetId: {}", aiSetId);

        try {
            progressService.sendProgress(aiSetId, "analyzing", 10, "Đang phân tích tài liệu...");

            // Lấy danh sách ngôn ngữ hỗ trợ
            List<SupportedLanguage> supportedLangs = languageRepository.findAllActive();
            if (supportedLangs.isEmpty()) {
                // Sync từ Piston
                pistonService.syncLanguages();
                supportedLangs = languageRepository.findAllActive();
            }

            String langsText = buildLanguagesText(supportedLangs);

            progressService.sendProgress(aiSetId, "generating", 30, "Đang sinh bài tập code...");

            // Build prompt
            String prompt = buildPrompt(documentContent, langsText, maxExercises, language, additionalReqs);

            // Gọi AI
            String llmResponse = aiModelService.callGeminiModel(prompt);
            List<Map<String, Object>> exercises = jsonParsingService.parseJsonArray(llmResponse);

            if (exercises == null || exercises.isEmpty()) {
                updateAiSetError(aiSetId,
                        "Không thể sinh bài tập code từ tài liệu này. Tài liệu có thể không phù hợp với lập trình.");
                return;
            }

            progressService.sendProgress(aiSetId, "saving", 60, "Đang lưu bài tập...");

            // Parse và lưu
            int savedCount = saveExercises(aiSetId, notebook, user, exercises, supportedLangs);

            // Update AI Set
            updateAiSetDone(aiSetId, savedCount);

            progressService.sendProgress(aiSetId, "done", 100, "Hoàn thành! Đã tạo " + savedCount + " bài tập.");

            log.info("✅ [CODE_EXERCISE] Generated {} exercises for aiSetId: {}", savedCount, aiSetId);

        } catch (Exception e) {
            log.error("❌ [CODE_EXERCISE] Generation failed: {}", e.getMessage(), e);
            updateAiSetError(aiSetId, e.getMessage());
        }
    }

    private String buildLanguagesText(List<SupportedLanguage> langs) {
        StringBuilder sb = new StringBuilder();
        for (SupportedLanguage lang : langs) {
            sb.append("- ").append(lang.getName())
                    .append(" (v").append(lang.getVersion()).append(")")
                    .append(" — id: ").append(lang.getId())
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String documentContent, String langsText,
            int maxExercises, String language, String additionalReqs) {

        String langInstruction = "vi".equals(language)
                ? "Viết bằng tiếng Việt."
                : "Write in English.";

        return """
                Bạn là **chuyên gia thiết kế bài tập lập trình**.
                Nhiệm vụ: Sinh 1-%d bài tập code thực hành dựa trên nội dung tài liệu.

                ═══════════════════════════════════════
                NỘI DUNG TÀI LIỆU:
                ═══════════════════════════════════════
                %s

                ═══════════════════════════════════════
                DANH SÁCH NGÔN NGỮ HỢP LỆ (chỉ chọn từ đây):
                ═══════════════════════════════════════
                %s

                ═══════════════════════════════════════
                YÊU CẦU:
                ═══════════════════════════════════════
                - %s
                - Mỗi bài gồm: language_id, title, description, difficulty, starter_files, solution_files, testcases
                - Code trong solution_files phải chạy đúng với testcases
                - Testcase: is_sample=false (hiển thị), is_sample=true (ẩn)
                %s

                ═══════════════════════════════════════
                ĐỊNH DẠNG JSON:
                ═══════════════════════════════════════
                [
                  {
                    "language_id": "uuid",
                    "title": "Tên bài",
                    "description": "Mô tả chi tiết",
                    "difficulty": "easy|medium|hard",
                    "starter_files": [
                      {"filename": "main.py", "content": "# TODO", "is_main": true}
                    ],
                    "solution_files": [
                      {"filename": "main.py", "content": "print(int(input())+int(input()))", "is_main": true}
                    ],
                    "testcases": [
                      {"input": "1\\n2\\n", "expected_output": "3\\n", "is_sample": false, "order_index": 0},
                      {"input": "5\\n7\\n", "expected_output": "12\\n", "is_sample": true, "order_index": 1}
                    ]
                  }
                ]

                ⚠️ NẾU TÀI LIỆU KHÔNG PHÙ HỢP VỚI LẬP TRÌNH → trả về mảng rỗng: []

                CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC.
                """.formatted(maxExercises, documentContent, langsText, langInstruction,
                additionalReqs != null ? "- " + additionalReqs : "");
    }

    @SuppressWarnings("unchecked")
    private int saveExercises(UUID aiSetId, Notebook notebook, User user,
            List<Map<String, Object>> exercises,
            List<SupportedLanguage> supportedLangs) {

        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
        if (aiSet == null)
            return 0;

        int savedCount = 0;
        int orderIndex = 0;

        for (Map<String, Object> ex : exercises) {
            try {
                String langId = (String) ex.get("language_id");
                SupportedLanguage lang = findLanguage(langId, supportedLangs);
                if (lang == null) {
                    log.warn("⚠️ Language not found: {}", langId);
                    continue;
                }

                // Lấy solution files và testcases từ AI response
                List<Map<String, Object>> solutionFiles = (List<Map<String, Object>>) ex.get("solution_files");
                List<Map<String, Object>> testcases = (List<Map<String, Object>>) ex.get("testcases");

                if (solutionFiles == null || solutionFiles.isEmpty()) {
                    log.warn("⚠️ Skipping exercise '{}': No solution files", ex.get("title"));
                    continue;
                }
                if (testcases == null || testcases.isEmpty()) {
                    log.warn("⚠️ Skipping exercise '{}': No testcases", ex.get("title"));
                    continue;
                }

                // 🔥 PRE-TEST: Chạy solution qua Piston, đảm bảo pass hết mới lưu
                boolean solutionPassed = testSolutionCode(lang, solutionFiles, testcases);
                if (!solutionPassed) {
                    log.warn("⚠️ Skipping exercise '{}': Solution failed testcases", ex.get("title"));
                    continue;
                }

                log.info("✅ Exercise '{}' solution passed all {} testcases", ex.get("title"), testcases.size());

                CodeExercise exercise = CodeExercise.builder()
                        .notebook(notebook)
                        .notebookAiSet(aiSet)
                        .language(lang)
                        .title((String) ex.get("title"))
                        .description((String) ex.get("description"))
                        .difficulty((String) ex.getOrDefault("difficulty", "medium"))
                        .orderIndex(orderIndex++)
                        .createdBy(user)
                        .build();

                exerciseRepository.save(exercise);

                // Save starter files
                List<Map<String, Object>> starterFiles = (List<Map<String, Object>>) ex.get("starter_files");
                if (starterFiles != null) {
                    for (Map<String, Object> f : starterFiles) {
                        saveFile(exercise, null, f, "starter");
                    }
                }

                // Save solution files
                for (Map<String, Object> f : solutionFiles) {
                    saveFile(exercise, null, f, "solution");
                }

                // Save testcases
                int tcIndex = 0;
                for (Map<String, Object> tc : testcases) {
                    saveTestcase(exercise, tc, tcIndex++);
                }

                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save exercise: {}", e.getMessage());
            }
        }

        return savedCount;
    }

    private SupportedLanguage findLanguage(String langId, List<SupportedLanguage> langs) {
        if (langId == null)
            return null;
        try {
            UUID uuid = UUID.fromString(langId);
            return langs.stream()
                    .filter(l -> l.getId().equals(uuid))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Chạy solution code qua Piston để test với tất cả testcases.
     * Trả về true nếu tất cả pass, false nếu có bất kỳ lỗi nào.
     */
    @SuppressWarnings("unchecked")
    private boolean testSolutionCode(SupportedLanguage lang,
            List<Map<String, Object>> solutionFiles,
            List<Map<String, Object>> testcases) {

        try {
            // Chuẩn bị files cho Piston
            List<Map<String, String>> pistonFiles = solutionFiles.stream()
                    .sorted((a, b) -> Boolean.compare(
                            Boolean.TRUE.equals(b.get("is_main")),
                            Boolean.TRUE.equals(a.get("is_main"))))
                    .map(f -> Map.of(
                            "name", (String) f.get("filename"),
                            "content", (String) f.get("content")))
                    .toList();

            // Chạy từng testcase
            for (Map<String, Object> tc : testcases) {
                String input = (String) tc.get("input");
                String expectedOutput = (String) tc.get("expected_output");

                // Normalize newlines từ JSON (\\n -> \n)
                if (input != null)
                    input = input.replace("\\n", "\n");
                if (expectedOutput != null)
                    expectedOutput = expectedOutput.replace("\\n", "\n").trim();

                Map<String, Object> response = pistonService.runCode(
                        lang.getName(),
                        lang.getVersion(),
                        new java.util.ArrayList<>(pistonFiles),
                        input);

                // Check response
                if (response.containsKey("error") && Boolean.TRUE.equals(response.get("error"))) {
                    log.warn("❌ Piston error: {}", response.get("message"));
                    return false;
                }

                Map<String, Object> run = (Map<String, Object>) response.getOrDefault("run", Map.of());
                String stdout = ((String) run.getOrDefault("stdout", "")).trim();
                int exitCode = run.get("code") instanceof Number ? ((Number) run.get("code")).intValue() : -1;
                String stderr = (String) run.getOrDefault("stderr", "");

                if (exitCode != 0 || (stderr != null && !stderr.isEmpty())) {
                    log.warn("❌ Solution runtime error: exit={}, stderr={}", exitCode, stderr);
                    return false;
                }

                if (!stdout.equals(expectedOutput)) {
                    log.warn("❌ Solution output mismatch: expected='{}', got='{}'", expectedOutput, stdout);
                    return false;
                }
            }

            return true; // All passed!

        } catch (Exception e) {
            log.error("❌ Error testing solution: {}", e.getMessage());
            return false;
        }
    }

    private void saveFile(CodeExercise exercise, User user, Map<String, Object> f, String role) {
        CodeExerciseFile file = CodeExerciseFile.builder()
                .exercise(exercise)
                .user(user)
                .filename((String) f.get("filename"))
                .content((String) f.get("content"))
                .role(role)
                .isMain(Boolean.TRUE.equals(f.get("is_main")))
                .build();
        fileRepository.save(file);
    }

    private void saveTestcase(CodeExercise exercise, Map<String, Object> tc, int defaultIndex) {
        CodeExerciseTestcase testcase = CodeExerciseTestcase.builder()
                .exercise(exercise)
                .input((String) tc.get("input"))
                .expectedOutput((String) tc.get("expected_output"))
                .isSample(Boolean.TRUE.equals(tc.get("is_sample")))
                .orderIndex(tc.get("order_index") instanceof Number
                        ? ((Number) tc.get("order_index")).intValue()
                        : defaultIndex)
                .build();
        testcaseRepository.save(testcase);
    }

    private void updateAiSetDone(UUID aiSetId, int count) {
        aiSetRepository.findById(aiSetId).ifPresent(aiSet -> {
            aiSet.setStatus("done");
            aiSet.setFinishedAt(java.time.OffsetDateTime.now());
            aiSet.setOutputStats(Map.of("exerciseCount", count));
            aiSetRepository.save(aiSet);
        });
    }

    private void updateAiSetError(UUID aiSetId, String errorMessage) {
        aiSetRepository.findById(aiSetId).ifPresent(aiSet -> {
            aiSet.setStatus("error");
            aiSet.setErrorMessage(errorMessage);
            aiSet.setFinishedAt(java.time.OffsetDateTime.now());
            aiSetRepository.save(aiSet);
        });
    }
}
