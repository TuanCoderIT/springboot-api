package com.example.springboot_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for exam system
 */
@Configuration
@ConfigurationProperties(prefix = "exam")
@Data
public class ExamConfig {
    
    /**
     * Default exam duration in minutes
     */
    private int defaultDurationMinutes = 120;
    
    /**
     * Maximum exam duration in minutes
     */
    private int maxDurationMinutes = 480; // 8 hours
    
    /**
     * Default maximum attempts per exam
     */
    private int defaultMaxAttempts = 1;
    
    /**
     * Maximum attempts allowed per exam
     */
    private int maxAttemptsLimit = 10;
    
    /**
     * Auto-submit buffer time in seconds before exam ends
     */
    private int autoSubmitBufferSeconds = 300; // 5 minutes
    
    /**
     * Maximum questions per exam
     */
    private int maxQuestionsPerExam = 100;
    
    /**
     * Default points per question
     */
    private double defaultPointsPerQuestion = 1.0;
    
    /**
     * Enable anti-cheat features by default
     */
    private boolean enableAntiCheatByDefault = false;
    
    /**
     * Enable proctoring by default
     */
    private boolean enableProctoringByDefault = false;
    
    /**
     * Question generation settings
     */
    private QuestionGeneration questionGeneration = new QuestionGeneration();
    
    @Data
    public static class QuestionGeneration {
        /**
         * Default AI model for question generation
         */
        private String defaultAiModel = "gpt-4";
        
        /**
         * Default number of questions to generate
         */
        private int defaultQuestionCount = 10;
        
        /**
         * Default question types
         */
        private String defaultQuestionTypes = "MCQ";
        
        /**
         * Default difficulty level
         */
        private String defaultDifficultyLevel = "MEDIUM";
        
        /**
         * Default number of MCQ options
         */
        private int defaultMcqOptionsCount = 4;
        
        /**
         * Maximum questions that can be generated at once
         */
        private int maxQuestionsPerGeneration = 50;
        
        /**
         * Timeout for question generation in seconds
         */
        private int generationTimeoutSeconds = 300; // 5 minutes
    }
    
    /**
     * Grading settings
     */
    private Grading grading = new Grading();
    
    @Data
    public static class Grading {
        /**
         * Enable automatic grading for MCQ questions
         */
        private boolean enableAutoGrading = true;
        
        /**
         * Default passing score percentage
         */
        private double defaultPassingPercentage = 60.0;
        
        /**
         * Grade scale
         */
        private GradeScale gradeScale = new GradeScale();
        
        @Data
        public static class GradeScale {
            private double aGrade = 90.0;
            private double bGrade = 80.0;
            private double cGrade = 70.0;
            private double dGrade = 60.0;
            // Below D is F
        }
    }
    
    /**
     * Security settings
     */
    private Security security = new Security();
    
    @Data
    public static class Security {
        /**
         * Enable IP address tracking
         */
        private boolean trackIpAddress = true;
        
        /**
         * Enable browser fingerprinting
         */
        private boolean enableBrowserFingerprinting = true;
        
        /**
         * Maximum tab switches allowed during exam
         */
        private int maxTabSwitches = 5;
        
        /**
         * Maximum copy-paste actions allowed
         */
        private int maxCopyPasteActions = 3;
        
        /**
         * Enable screenshot detection
         */
        private boolean enableScreenshotDetection = false;
        
        /**
         * Enable fullscreen enforcement
         */
        private boolean enforceFullscreen = false;
    }
}