package com.example.springboot_api.services.exam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for automatic exam management tasks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamSchedulerService {
    
    private final ExamService examService;
    
    /**
     * Auto-submit expired exam attempts every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void autoSubmitExpiredAttempts() {
        try {
            log.debug("Running auto-submit for expired attempts");
            examService.autoSubmitExpiredAttempts();
        } catch (Exception e) {
            log.error("Error in auto-submit expired attempts: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update exam statuses every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void updateExamStatuses() {
        try {
            log.debug("Updating exam statuses");
            examService.updateExamStatuses();
        } catch (Exception e) {
            log.error("Error updating exam statuses: {}", e.getMessage(), e);
        }
    }
}