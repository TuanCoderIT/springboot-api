package com.example.springboot_api.dto.user.code;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response kết quả chạy code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeResponse {

    /**
     * Trạng thái tổng: passed, failed.
     */
    private String status;

    /**
     * Số test passed.
     */
    private int passed;

    /**
     * Số test failed.
     */
    private int failed;

    /**
     * Tổng số test.
     */
    private int total;

    /**
     * Đã lưu (pass tất cả).
     */
    private boolean saved;

    /**
     * Chi tiết từng test case.
     */
    private List<TestResult> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private UUID id;
        private Integer index;
        private String input;
        private String expected;
        private String output;
        private String stderr;
        private Integer exitCode;
        private Double cpuTime;
        private Long memory;
        private String result; // passed, failed, runtime_error, time_limit_exceeded
        private Boolean isHidden;
    }
}
