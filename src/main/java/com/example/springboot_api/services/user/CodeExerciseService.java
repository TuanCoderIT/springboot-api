package com.example.springboot_api.services.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.code.CodeExerciseGenerateRequest;
import com.example.springboot_api.dto.user.code.CodeExerciseResponse;
import com.example.springboot_api.dto.user.code.RunCodeRequest;
import com.example.springboot_api.dto.user.code.RunCodeResponse;
import com.example.springboot_api.models.CodeExercise;
import com.example.springboot_api.models.CodeExerciseFile;
import com.example.springboot_api.models.CodeExerciseTestcase;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.SupportedLanguage;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.CodeExerciseFileRepository;
import com.example.springboot_api.repositories.shared.CodeExerciseRepository;
import com.example.springboot_api.repositories.shared.CodeExerciseTestcaseRepository;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.SupportedLanguageRepository;
import com.example.springboot_api.services.shared.ai.PistonService;
import com.example.springboot_api.services.shared.ai.generation.CodeExerciseGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service chính cho Code Exercises.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExerciseService {

        private final NotebookRepository notebookRepository;
        private final NotebookFileRepository fileRepository;
        private final FileChunkRepository chunkRepository;
        private final NotebookAiSetRepository aiSetRepository;
        private final SupportedLanguageRepository languageRepository;
        private final CodeExerciseRepository exerciseRepository;
        private final CodeExerciseFileRepository exerciseFileRepository;
        private final CodeExerciseTestcaseRepository testcaseRepository;
        private final PistonService pistonService;
        private final CodeExerciseGenerationService generationService;

        /**
         * Sinh bài tập code từ tài liệu.
         */
        @Transactional
        public NotebookAiSet generateExercises(UUID notebookId, User user,
                        CodeExerciseGenerateRequest request) {

                Notebook notebook = notebookRepository.findById(notebookId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy notebook"));

                // Lấy nội dung tài liệu (Optimized: avoid loading full entities with embedding)
                List<String> contents = chunkRepository.findContentByFileIds(request.getFileIds());
                if (contents.isEmpty()) {
                        throw new BadRequestException("Không tìm thấy nội dung tài liệu");
                }
                String documentContent = String.join("\n\n---\n\n", contents);

                if (documentContent.isBlank()) {
                        throw new BadRequestException("Tài liệu chưa được xử lý OCR");
                }

                // Tạo AI Set
                NotebookAiSet aiSet = NotebookAiSet.builder()
                                .notebook(notebook)
                                .setType("code_exercise")
                                .title("Bài tập code từ " + request.getFileIds().size() + " tài liệu")
                                .status("processing")
                                .createdBy(user)
                                .createdAt(java.time.OffsetDateTime.now())
                                .startedAt(java.time.OffsetDateTime.now())
                                .build();
                aiSetRepository.save(aiSet);

                // Trigger async generation
                generationService.generateAsync(
                                aiSet.getId(),
                                notebook,
                                user,
                                documentContent,
                                request.getMaxExercises() != null ? request.getMaxExercises() : 3,
                                request.getLanguage() != null ? request.getLanguage() : "vi",
                                request.getAdditionalRequirements());

                return aiSet;
        }

        /**
         * Lấy danh sách bài tập của AI Set.
         */
        @Transactional(readOnly = true)
        public List<CodeExerciseResponse> getExercisesByAiSet(UUID aiSetId) {
                List<CodeExercise> exercises = exerciseRepository.findByAiSetId(aiSetId);
                return exercises.stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Lấy chi tiết một bài tập.
         */
        @Transactional(readOnly = true)
        public CodeExerciseResponse getExerciseDetail(UUID exerciseId) {
                CodeExercise exercise = exerciseRepository.findById(exerciseId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập"));
                return toResponse(exercise);
        }

        /**
         * Chạy code của user với tất cả test cases.
         */
        @Transactional
        @SuppressWarnings("unchecked")
        public RunCodeResponse runUserCode(UUID exerciseId, User user, RunCodeRequest request) {

                CodeExercise exercise = exerciseRepository.findById(exerciseId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập"));

                SupportedLanguage lang;
                if (request.getLanguageId() != null) {
                        lang = languageRepository.findById(request.getLanguageId())
                                        .orElseThrow(() -> new BadRequestException("Ngôn ngữ không hợp lệ"));
                } else {
                        lang = exercise.getLanguage();
                        if (lang == null) {
                                throw new BadRequestException("Bài tập chưa được gán ngôn ngữ");
                        }
                }

                // Lấy test cases
                List<CodeExerciseTestcase> testcases = testcaseRepository.findByExerciseId(exerciseId);
                if (testcases.isEmpty()) {
                        throw new BadRequestException("Bài tập không có test cases");
                }

                // Chuẩn bị files (main đi trước)
                List<Map<String, String>> files = request.getFiles().stream()
                                .sorted((a, b) -> Boolean.compare(
                                                Boolean.TRUE.equals(b.getIsMain()),
                                                Boolean.TRUE.equals(a.getIsMain())))
                                .map(f -> Map.of("name", f.getFilename(), "content", f.getContent()))
                                .toList();

                List<RunCodeResponse.TestResult> results = new ArrayList<>();
                int timeLimit = exercise.getTimeLimit() != null ? exercise.getTimeLimit() : 2;
                long memoryLimit = exercise.getMemoryLimit() != null ? exercise.getMemoryLimit() : 256000000L;

                // Chạy từng test case
                for (CodeExerciseTestcase tc : testcases) {
                        Map<String, Object> response = pistonService.runCode(
                                        lang.getName(),
                                        lang.getVersion(),
                                        new ArrayList<>(files),
                                        tc.getInput());

                        Map<String, Object> run = (Map<String, Object>) response.getOrDefault("run", Map.of());
                        String stdout = ((String) run.getOrDefault("stdout", "")).trim();
                        String stderr = ((String) run.getOrDefault("stderr", "")).trim();
                        int exitCode = run.get("code") instanceof Number ? ((Number) run.get("code")).intValue() : 0;
                        double cpuTime = run.get("cpu_time") instanceof Number
                                        ? ((Number) run.get("cpu_time")).doubleValue()
                                        : 0;
                        long memory = run.get("memory") instanceof Number ? ((Number) run.get("memory")).longValue()
                                        : 0;

                        String expected = tc.getExpectedOutput() != null ? tc.getExpectedOutput().trim() : "";

                        // Phân loại kết quả
                        String verdict;
                        // Piston cpu_time thường là ms, timeLimit là seconds
                        double cpuTimeSeconds = cpuTime / 1000.0;

                        if (response.containsKey("error") && Boolean.TRUE.equals(response.get("error"))) {
                                verdict = "runtime_error";
                        } else if (exitCode != 0 || !stderr.isEmpty()) {
                                verdict = "runtime_error";
                        } else if (cpuTimeSeconds > timeLimit) {
                                verdict = "time_limit_exceeded";
                        } else if (memory > memoryLimit) {
                                verdict = "memory_limit_exceeded";
                        } else if (stdout.equals(expected)) {
                                verdict = "passed";
                        } else {
                                verdict = "failed";
                        }

                        // Test ẩn không hiển thị input/output
                        if (Boolean.TRUE.equals(tc.getIsSample())) {
                                results.add(RunCodeResponse.TestResult.builder()
                                                .id(tc.getId())
                                                .index(tc.getOrderIndex())
                                                .result(verdict)
                                                .isHidden(true)
                                                .build());
                        } else {
                                results.add(RunCodeResponse.TestResult.builder()
                                                .id(tc.getId())
                                                .index(tc.getOrderIndex())
                                                .input(tc.getInput())
                                                .expected(expected)
                                                .output(stdout)
                                                .stderr(stderr)
                                                .exitCode(exitCode)
                                                .cpuTime(cpuTime)
                                                .memory(memory)
                                                .result(verdict)
                                                .isHidden(false)
                                                .build());
                        }
                }

                // Tổng kết
                int passed = (int) results.stream().filter(r -> "passed".equals(r.getResult())).count();
                int total = results.size();
                boolean allPassed = passed == total;

                // Nếu pass toàn bộ → lưu và đánh dấu is_pass=true
                if (allPassed) {
                        saveUserCode(exercise, user, request.getFiles());
                }

                return RunCodeResponse.builder()
                                .status(allPassed ? "passed" : "failed")
                                .passed(passed)
                                .failed(total - passed)
                                .total(total)
                                .saved(allPassed)
                                .details(results)
                                .build();
        }

        /**
         * Lấy danh sách ngôn ngữ hỗ trợ.
         */
        @Transactional(readOnly = true)
        public List<SupportedLanguage> getSupportedLanguages() {
                return languageRepository.findAllActive();
        }

        /**
         * Lấy code mẫu (solution) của bài tập.
         */
        @Transactional(readOnly = true)
        public List<CodeExerciseFile> getSolutionCode(UUID exerciseId) {
                return exerciseFileRepository.findSolutionFiles(exerciseId);
        }

        /**
         * Sync languages từ Piston.
         */
        @Transactional
        public int syncLanguages() {
                return pistonService.syncLanguages();
        }

        private void saveUserCode(CodeExercise exercise, User user, List<RunCodeRequest.CodeFile> files) {
                // Xóa code cũ
                List<CodeExerciseFile> oldFiles = exerciseFileRepository.findUserFiles(exercise.getId(), user.getId());
                exerciseFileRepository.deleteAll(oldFiles);

                // Lưu code mới
                for (RunCodeRequest.CodeFile f : files) {
                        CodeExerciseFile file = CodeExerciseFile.builder()
                                        .exercise(exercise)
                                        .user(user)
                                        .filename(f.getFilename())
                                        .content(f.getContent())
                                        .role("user")
                                        .isMain(Boolean.TRUE.equals(f.getIsMain()))
                                        .isPass(true)
                                        .build();
                        exerciseFileRepository.save(file);
                }
        }

        private CodeExerciseResponse toResponse(CodeExercise exercise) {
                // Starter files
                List<CodeExerciseFile> starterFiles = exerciseFileRepository
                                .findByExerciseIdAndRole(exercise.getId(), "starter");

                // Sample testcases (không ẩn)
                List<CodeExerciseTestcase> sampleTestcases = testcaseRepository
                                .findSampleTestcases(exercise.getId());

                SupportedLanguage lang = exercise.getLanguage();

                return CodeExerciseResponse.builder()
                                .id(exercise.getId())
                                .aiSetId(exercise.getNotebookAiSet().getId())
                                .title(exercise.getTitle())
                                .description(exercise.getDescription())
                                .difficulty(exercise.getDifficulty())
                                .timeLimit(exercise.getTimeLimit())
                                .memoryLimit(exercise.getMemoryLimit())
                                .orderIndex(exercise.getOrderIndex())
                                .language(CodeExerciseResponse.LanguageInfo.builder()
                                                .id(lang.getId())
                                                .name(lang.getName())
                                                .version(lang.getVersion())
                                                .build())
                                .starterFiles(starterFiles.stream()
                                                .map(f -> CodeExerciseResponse.CodeFileResponse.builder()
                                                                .id(f.getId())
                                                                .filename(f.getFilename())
                                                                .content(f.getContent())
                                                                .isMain(f.getIsMain())
                                                                .build())
                                                .toList())
                                .sampleTestcases(sampleTestcases.stream()
                                                .map(tc -> CodeExerciseResponse.TestcaseResponse.builder()
                                                                .id(tc.getId())
                                                                .input(tc.getInput())
                                                                .expectedOutput(tc.getExpectedOutput())
                                                                .orderIndex(tc.getOrderIndex())
                                                                .isHidden(false)
                                                                .build())
                                                .toList())
                                .createdAt(exercise.getCreatedAt())
                                .build();
        }
}
