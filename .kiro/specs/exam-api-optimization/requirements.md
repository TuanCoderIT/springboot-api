# Requirements Document

## Introduction

This document specifies the requirements for optimizing the online exam system API. The system enables lecturers to create, manage, and conduct online exams while providing students with a secure testing environment. The optimization focuses on creating a clean, complete, and extensible API architecture that separates concerns between exam configuration, exam attempts, and results management.

## Glossary

- **Exam_System**: The complete online examination platform
- **Lecturer**: Faculty member who creates and manages exams
- **Student**: User who takes exams
- **Exam**: Configuration and question set for a testing session
- **Exam_Attempt**: A single instance of a student taking an exam
- **Notebook**: Source material used for generating exam questions
- **Anti_Cheat_System**: Monitoring system for detecting suspicious behavior during exams

## Requirements

### Requirement 1: Exam Creation and Management

**User Story:** As a lecturer, I want to create and manage exam configurations, so that I can set up structured testing sessions for my students.

#### Acceptance Criteria

1. WHEN a lecturer creates a new exam, THE Exam_System SHALL create an exam with status DRAFT
2. WHEN an exam is in DRAFT status, THE Exam_System SHALL allow modifications to exam configuration
3. WHEN a lecturer generates questions from a notebook, THE Exam_System SHALL create exam questions and set status to GENERATED
4. WHEN a lecturer publishes an exam, THE Exam_System SHALL set status to PUBLISHED and prevent further question modifications
5. WHEN an exam reaches its start time and status is PUBLISHED, THE Exam_System SHALL automatically make it available for students
6. WHEN a lecturer cancels an exam, THE Exam_System SHALL set status to CANCELLED and prevent student access

### Requirement 2: Question Generation and Preview

**User Story:** As a lecturer, I want to generate questions from notebook content and preview the complete exam, so that I can ensure exam quality before publishing.

#### Acceptance Criteria

1. WHEN a lecturer requests question generation from a notebook, THE Exam_System SHALL create questions based on notebook content
2. WHEN questions are generated, THE Exam_System SHALL store them in the exam_questions table
3. WHEN a lecturer requests exam preview, THE Exam_System SHALL display all questions, answers, and scoring information
4. WHEN previewing an exam, THE Exam_System SHALL show the complete exam structure without affecting exam status
5. THE Exam_System SHALL provide preview functionality separate from general exam viewing

### Requirement 3: Exam Deletion Controls

**User Story:** As a lecturer, I want controlled deletion of exams, so that I can remove drafts while protecting active exams with student data.

#### Acceptance Criteria

1. WHEN a lecturer attempts to delete an exam with status DRAFT and no attempts, THE Exam_System SHALL allow deletion
2. WHEN a lecturer attempts to delete an exam with existing attempts, THE Exam_System SHALL prevent deletion and return an error
3. WHEN a lecturer attempts to delete an exam with status other than DRAFT, THE Exam_System SHALL prevent deletion and return an error

### Requirement 4: Student Exam Access

**User Story:** As a student, I want to check exam availability and start taking exams, so that I can participate in scheduled assessments.

#### Acceptance Criteria

1. WHEN a student checks if they can take an exam, THE Exam_System SHALL verify eligibility based on exam status, timing, and student enrollment
2. WHEN an eligible student starts an exam, THE Exam_System SHALL create an exam attempt record
3. WHEN an exam attempt is created, THE Exam_System SHALL snapshot the current exam questions
4. WHEN an exam attempt is created, THE Exam_System SHALL start the exam timer
5. WHEN a student submits an exam attempt, THE Exam_System SHALL calculate scores and finalize the attempt

### Requirement 5: Anti-Cheat Monitoring

**User Story:** As a lecturer, I want to monitor student behavior during exams, so that I can detect and prevent cheating attempts.

#### Acceptance Criteria

1. WHEN a student performs a monitored action during an exam, THE Anti_Cheat_System SHALL record the event with attempt ID
2. WHEN anti-cheat events are recorded, THE Anti_Cheat_System SHALL store event type and occurrence count
3. WHEN suspicious behavior is detected, THE Anti_Cheat_System SHALL flag the exam attempt for review
4. THE Anti_Cheat_System SHALL support extensible event types including tab switches, window focus changes, and copy-paste attempts

### Requirement 6: Results and Analytics

**User Story:** As a lecturer, I want to view individual and aggregate exam results, so that I can assess student performance and exam effectiveness.

#### Acceptance Criteria

1. WHEN a student completes an exam, THE Exam_System SHALL provide individual result access to the student
2. WHEN a lecturer requests exam results, THE Exam_System SHALL provide aggregate results for all attempts
3. WHEN results are displayed, THE Exam_System SHALL include scores, completion times, and anti-cheat event summaries
4. THE Exam_System SHALL provide results filtering by class and student groups

### Requirement 7: Data Export Capabilities

**User Story:** As a lecturer, I want to export exam results in standard formats, so that I can integrate with gradebook systems and perform further analysis.

#### Acceptance Criteria

1. WHEN a lecturer requests result export, THE Exam_System SHALL generate files in Excel and CSV formats
2. WHEN exporting results, THE Exam_System SHALL include student information, scores, and completion details
3. WHEN exporting by class, THE Exam_System SHALL filter results to include only students from the specified class
4. THE Exam_System SHALL provide export functionality for both individual exams and multiple exam comparisons

### Requirement 8: API Architecture Separation

**User Story:** As a system architect, I want clear separation between exam configuration, attempts, and results, so that the system is maintainable and extensible.

#### Acceptance Criteria

1. WHEN handling exam operations, THE Exam_System SHALL separate exam configuration from attempt management
2. WHEN managing exam attempts, THE Exam_System SHALL use attempt-specific endpoints rather than exam-based submission
3. WHEN accessing results, THE Exam_System SHALL provide dedicated result endpoints separate from exam configuration
4. THE Exam_System SHALL maintain clear data boundaries between exams, attempts, and notebooks

### Requirement 9: System Extensibility

**User Story:** As a system architect, I want the exam system to support future enhancements, so that new question types and anti-cheat measures can be added without major restructuring.

#### Acceptance Criteria

1. WHEN new question types are added, THE Exam_System SHALL support them without changing core exam logic
2. WHEN new anti-cheat measures are implemented, THE Anti_Cheat_System SHALL integrate them through the existing event framework
3. WHEN exam formats are extended, THE Exam_System SHALL maintain backward compatibility with existing exams
4. THE Exam_System SHALL provide extension points for custom grading algorithms and question generators