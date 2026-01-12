package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.user.code.CodeExerciseGenerateRequest;
import com.example.springboot_api.dto.user.code.CodeExerciseResponse;
import com.example.springboot_api.dto.user.code.RunCodeRequest;
import com.example.springboot_api.dto.user.code.RunCodeResponse;
import com.example.springboot_api.models.CodeExerciseFile;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.SupportedLanguage;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.services.user.CodeExerciseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller cho Code Exercises - TEST VERSION (no auth).
 * TODO: Thêm lại @AuthenticationPrincipal sau khi test xong.
 */
@RestController
@RequestMapping("/user/notebooks/{notebookId}/ai/code-exercises")
@RequiredArgsConstructor
@Slf4j
public class CodeExerciseController {

    // Trigger reload
    private final CodeExerciseService codeExerciseService;
    private final UserRepository userRepository;

    /**
     * Lấy mock user cho test.
     */
    private User getMockUser() {
        // Lấy user đầu tiên trong DB để test
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    /**
     * Sinh bài tập code từ tài liệu.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateExercises(
            @PathVariable UUID notebookId,
            @Valid @RequestBody CodeExerciseGenerateRequest request) {

        log.info("📝 [CODE_EXERCISE] Generate request for notebook: {}", notebookId);

        User user = getMockUser();
        if (user == null) {
            return ResponseEntity.badRequest().body("No user found for testing");
        }

        NotebookAiSet aiSet = codeExerciseService.generateExercises(notebookId, user, request);

        return ResponseEntity.ok(java.util.Map.of(
                "aiSetId", aiSet.getId(),
                "status", aiSet.getStatus(),
                "message", "Đang sinh bài tập code..."));
    }

    /**
     * Lấy danh sách bài tập của AI Set.
     */
    @GetMapping("/{aiSetId}")
    public ResponseEntity<List<CodeExerciseResponse>> getExercises(
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        List<CodeExerciseResponse> exercises = codeExerciseService.getExercisesByAiSet(aiSetId);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Lấy chi tiết một bài tập.
     */
    @GetMapping("/exercise/{exerciseId}")
    public ResponseEntity<CodeExerciseResponse> getExerciseDetail(
            @PathVariable UUID notebookId,
            @PathVariable UUID exerciseId) {

        CodeExerciseResponse exercise = codeExerciseService.getExerciseDetail(exerciseId);
        return ResponseEntity.ok(exercise);
    }

    /**
     * Chạy code user với test cases.
     */
    @PostMapping("/exercise/{exerciseId}/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @PathVariable UUID notebookId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody RunCodeRequest request) {

        log.info("🚀 [CODE_EXERCISE] Run code for exercise: {}", exerciseId);

        User user = getMockUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        RunCodeResponse result = codeExerciseService.runUserCode(exerciseId, user, request);
        return ResponseEntity.ok(result);

    }

    /**
     * Lấy code mẫu (solution) của bài tập.
     */
    @GetMapping("/exercise/{exerciseId}/solution")
    public ResponseEntity<List<RunCodeRequest.CodeFile>> getSolution(
            @PathVariable UUID notebookId,
            @PathVariable UUID exerciseId) {

        List<CodeExerciseFile> files = codeExerciseService.getSolutionCode(exerciseId);

        List<RunCodeRequest.CodeFile> response = files.stream()
                .map(f -> RunCodeRequest.CodeFile.builder()
                        .filename(f.getFilename())
                        .content(f.getContent())
                        .isMain(f.getIsMain())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách ngôn ngữ hỗ trợ.
     */
    @GetMapping("/languages")
    public ResponseEntity<List<SupportedLanguage>> getSupportedLanguages(
            @PathVariable UUID notebookId) {

        return ResponseEntity.ok(codeExerciseService.getSupportedLanguages());
    }

    /**
     * Sync languages từ Piston.
     */
    @PostMapping("/languages/sync")
    public ResponseEntity<?> syncLanguages(@PathVariable UUID notebookId) {

        int count = codeExerciseService.syncLanguages();
        return ResponseEntity.ok(java.util.Map.of(
                "synced", count,
                "message", "Đã sync " + count + " ngôn ngữ từ Piston"));
    }
}
