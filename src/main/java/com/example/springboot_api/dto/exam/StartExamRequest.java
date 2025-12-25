package com.example.springboot_api.dto.exam;

import lombok.Data;

@Data
public class StartExamRequest {
    
    // Browser information for anti-cheat
    private String browserName;
    private String browserVersion;
    private String operatingSystem;
    private String screenResolution;
    private String timezone;
    
    // Device information
    private String deviceType; // desktop, mobile, tablet
    private Boolean isFullScreen;
    private Boolean hasCamera;
    private Boolean hasMicrophone;
    
    // Network information
    private String ipAddress;
    private String userAgent;
    
    // Proctoring consent
    private Boolean proctoringConsent = false;
    private Boolean cameraPermission = false;
    private Boolean microphonePermission = false;
    private Boolean screenSharePermission = false;
    
    // Academic integrity acknowledgment
    private Boolean academicIntegrityAcknowledged = false;
    private Boolean rulesAcknowledged = false;
}