### Online Exam System API Examples

### Variables
@baseUrl = http://localhost:8080/api
@lecturerToken = Bearer your-lecturer-jwt-token-here
@studentToken = Bearer your-student-jwt-token-here
@examId = exam-uuid-here
@classId = class-uuid-here

### 1. Create a new exam (Lecturer)
POST {{baseUrl}}/exams
Authorization: {{lecturerToken}}
Content-Type: application/json

{
  "classId": "{{classId}}",
  "title": "Midterm Exam - Java Programming",
  "description": "Comprehensive exam covering Java fundamentals, OOP, and Spring Boot",
  "startTime": "2024-12-30T09:00:00",
  "endTime": "2024-12-30T12:00:00",
  "durationMinutes": 120,
  "passingScore": 60.0,
  "shuffleQuestions": true,
  "shuffleOptions": true,
  "showResultsImmediately": false,
  "allowReview": true,
  "maxAttempts": 1,
  "enableProctoring": false,
  "enableLockdown": false,
  "enablePlagiarismCheck": false,
  "notebookFileIds": ["file-uuid-1", "file-uuid-2"],
  "numberOfQuestions": 20,
  "questionTypes": "MCQ,TRUE_FALSE",
  "difficultyLevel": "MEDIUM"
}

### 2. Generate questions for exam (Lecturer)
POST {{baseUrl}}/exams/{{examId}}/generate
Authorization: {{lecturerToken}}
Content-Type: application/json

{
  "notebookFileIds": ["file-uuid-1", "file-uuid-2", "file-uuid-3"],
  "numberOfQuestions": 25,
  "questionTypes": "MCQ,TRUE_FALSE,ESSAY",
  "difficultyLevel": "MIXED",
  "mcqOptionsCount": 4,
  "includeExplanation": true,
  "generateImages": false,
  "aiModel": "gpt-4",
  "language": "vi",
  "easyPercentage": 30,
  "mediumPercentage": 50,
  "hardPercentage": 20
}

### 3. Publish exam (Lecturer)
PUT {{baseUrl}}/exams/{{examId}}/publish
Authorization: {{lecturerToken}}

### 4. Activate exam (Lecturer)
PUT {{baseUrl}}/exams/{{examId}}/activate
Authorization: {{lecturerToken}}

### 5. Get exams by class (Lecturer)
GET {{baseUrl}}/exams/class/{{classId}}?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: {{lecturerToken}}

### 6. Get lecturer's exams
GET {{baseUrl}}/exams/lecturer?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: {{lecturerToken}}

### 7. Get exam details
GET {{baseUrl}}/exams/{{examId}}
Authorization: {{lecturerToken}}

### 8. Get available exams for student
GET {{baseUrl}}/exams/available
Authorization: {{studentToken}}

### 9. Check if student can take exam
GET {{baseUrl}}/exams/{{examId}}/can-take
Authorization: {{studentToken}}

### 10. Start exam (Student)
POST {{baseUrl}}/exams/{{examId}}/start
Authorization: {{studentToken}}
Content-Type: application/json

{
  "browserName": "Chrome",
  "browserVersion": "120.0.0.0",
  "operatingSystem": "Windows 11",
  "screenResolution": "1920x1080",
  "timezone": "Asia/Ho_Chi_Minh",
  "deviceType": "desktop",
  "isFullScreen": true,
  "hasCamera": true,
  "hasMicrophone": true,
  "proctoringConsent": false,
  "cameraPermission": false,
  "microphonePermission": false,
  "screenSharePermission": false,
  "academicIntegrityAcknowledged": true,
  "rulesAcknowledged": true
}

### 11. Submit exam (Student)
POST {{baseUrl}}/exams/{{examId}}/submit
Authorization: {{studentToken}}
Content-Type: application/json

{
  "attemptId": "attempt-uuid-here",
  "isAutoSubmit": false,
  "timeSpentSeconds": 7200,
  "answers": [
    {
      "questionId": "question-uuid-1",
      "answerData": {
        "selectedOptionId": "option-uuid-1"
      },
      "timeSpentSeconds": 120,
      "revisionCount": 1,
      "wasSkipped": false,
      "confidence": "HIGH"
    },
    {
      "questionId": "question-uuid-2",
      "answerData": {
        "answer": true
      },
      "timeSpentSeconds": 60,
      "revisionCount": 0,
      "wasSkipped": false,
      "confidence": "MEDIUM"
    },
    {
      "questionId": "question-uuid-3",
      "answerData": {
        "essayText": "This is my essay answer explaining the concepts of object-oriented programming..."
      },
      "timeSpentSeconds": 900,
      "revisionCount": 3,
      "wasSkipped": false,
      "confidence": "HIGH"
    }
  ],
  "finalBrowserInfo": "{}",
  "tabSwitchCount": 2,
  "copyPasteCount": 0,
  "rightClickCount": 1,
  "suspiciousActivities": []
}

### 12. Get exam result (Student)
GET {{baseUrl}}/exams/{{examId}}/result
Authorization: {{studentToken}}

### 13. Get all exam results (Lecturer)
GET {{baseUrl}}/exams/{{examId}}/results?page=0&size=20&sortBy=submittedAt&sortDir=desc
Authorization: {{lecturerToken}}

### 14. Cancel exam (Lecturer)
PUT {{baseUrl}}/exams/{{examId}}/cancel
Authorization: {{lecturerToken}}

### 15. Delete exam (Lecturer)
DELETE {{baseUrl}}/exams/{{examId}}
Authorization: {{lecturerToken}}

### Sample Response Examples

### Exam Response
# {
#   "id": "exam-uuid",
#   "classId": "class-uuid",
#   "className": "IT001.01",
#   "subjectCode": "IT001",
#   "subjectName": "Java Programming",
#   "title": "Midterm Exam - Java Programming",
#   "description": "Comprehensive exam covering Java fundamentals",
#   "startTime": "2024-12-30T09:00:00",
#   "endTime": "2024-12-30T12:00:00",
#   "durationMinutes": 120,
#   "totalQuestions": 25,
#   "totalPoints": 25.0,
#   "passingScore": 15.0,
#   "maxAttempts": 1,
#   "status": "ACTIVE",
#   "canTakeExam": true,
#   "isActive": true,
#   "isTimeUp": false,
#   "remainingAttempts": 1
# }

### Exam Attempt Response
# {
#   "attemptId": "attempt-uuid",
#   "examId": "exam-uuid",
#   "examTitle": "Midterm Exam - Java Programming",
#   "attemptNumber": 1,
#   "status": "IN_PROGRESS",
#   "startedAt": "2024-12-30T09:05:00",
#   "timeSpentSeconds": 0,
#   "remainingTimeSeconds": 7200,
#   "durationMinutes": 120,
#   "questions": [
#     {
#       "questionId": "question-uuid-1",
#       "questionType": "MCQ",
#       "questionText": "What is the main principle of OOP?",
#       "orderIndex": 1,
#       "points": 1.0,
#       "options": [
#         {
#           "optionId": "option-uuid-1",
#           "optionText": "Encapsulation",
#           "orderIndex": 1
#         },
#         {
#           "optionId": "option-uuid-2", 
#           "optionText": "Inheritance",
#           "orderIndex": 2
#         }
#       ]
#     }
#   ]
# }

### Exam Result Response
# {
#   "attemptId": "attempt-uuid",
#   "examId": "exam-uuid",
#   "examTitle": "Midterm Exam - Java Programming",
#   "status": "GRADED",
#   "totalScore": 18.5,
#   "totalPossibleScore": 25.0,
#   "percentageScore": 74.0,
#   "isPassed": true,
#   "grade": "B",
#   "totalQuestions": 25,
#   "correctAnswers": 18,
#   "incorrectAnswers": 5,
#   "skippedQuestions": 2,
#   "timeSpentFormatted": "1h 45m 30s"
# }