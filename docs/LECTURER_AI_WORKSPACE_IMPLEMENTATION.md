# Lecturer AI Workspace Implementation

## 📋 Tổng quan

Hệ thống AI Workspace cho giảng viên được xây dựng **tái sử dụng hoàn toàn** logic AI từ Notebook user, không duplicate code. Concept chính:

- **Workspace = Notebook với type="class"**
- **Giảng viên = owner/lecturer của notebook**
- **Sinh viên = members của notebook**
- **Tất cả AI features đều dùng chung pipeline existing**

## 🏗️ Kiến trúc

### Core Components

```
📁 Lecturer AI Workspace
├── 🎯 LecturerWorkspaceService (AI Content Generation)
├── 🏢 LecturerWorkspaceManagementService (Workspace CRUD)
├── 🌐 LecturerWorkspaceController (AI APIs)
├── 🌐 LecturerWorkspaceManagementController (Management APIs)
└── 📄 DTOs (Request/Response objects)
```

### Reused Services (No Code Duplication)

```
🔄 Shared AI Pipeline
├── SummaryGenerationService ✅
├── QuizGenerationService ✅
├── FlashcardGenerationService ✅
├── VideoGenerationService ✅
├── AudioOverviewService ✅
├── FileProcessingTaskService ✅
└── All other AI services ✅
```

## 📊 Data Model

### Notebook (Workspace)
```sql
-- Existing table, no changes needed
notebooks {
  id: UUID
  title: VARCHAR(255) -- "Lập trình Java - HK1 2024"
  description: TEXT
  type: VARCHAR(50) -- "class" (important!)
  visibility: VARCHAR(50) -- "private"
  created_by: UUID -- lecturer ID
  thumbnail_url: TEXT
  metadata: JSONB -- {
    "lecturerWorkspace": true,
    "subject": "Lập trình Java",
    "semester": "HK1",
    "academicYear": "2024-2025"
  }
  created_at: TIMESTAMP
  updated_at: TIMESTAMP
}
```

### NotebookMember (Permissions)
```sql
-- Existing table, no changes needed
notebook_members {
  id: UUID
  notebook_id: UUID -- workspace ID
  user_id: UUID
  role: VARCHAR(50) -- "owner", "lecturer", "student"
  status: VARCHAR(50) -- "approved", "pending"
  joined_at: TIMESTAMP
}
```

### NotebookFile (Documents)
```sql
-- Existing table, enhanced metadata
notebook_files {
  id: UUID
  notebook_id: UUID -- workspace ID
  uploaded_by: UUID -- lecturer ID
  file_name: VARCHAR(255)
  file_url: TEXT
  file_size: BIGINT
  mime_type: VARCHAR(100)
  status: VARCHAR(50)
  metadata: JSONB -- {
    "chapter": "Chương 1",
    "lecturerWorkspace": true,
    "purpose": "teaching_material"
  }
  created_at: TIMESTAMP
}
```

### NotebookAiSet (AI Content)
```sql
-- Existing table, enhanced metadata
notebook_ai_sets {
  id: UUID
  notebook_id: UUID -- workspace ID
  created_by: UUID -- lecturer ID
  set_type: VARCHAR -- "summary", "quiz", "flashcard", "video"
  status: VARCHAR(50) -- "queued", "processing", "completed", "failed"
  title: TEXT
  description: TEXT
  model_code: VARCHAR(50) -- "gemini", "groq"
  provider: VARCHAR(50) -- "google"
  metadata: JSONB -- {
    "lecturerWorkspace": true,
    "chapter": "Chương 1",
    "purpose": "teaching_content"
  }
  input_config: JSONB -- {
    "fileIds": [...],
    "customPrompt": "...",
    "quizCount": 10
  }
  created_at: TIMESTAMP
  finished_at: TIMESTAMP
}
```

## 🚀 API Endpoints

### Workspace Management

```http
# Tạo workspace mới
POST /api/lecturer/workspace-management
Content-Type: application/json
X-User-Id: {lecturerId}

{
  "title": "Lập trình Java - HK1 2024",
  "description": "Lớp học phần Lập trình Java học kỳ 1 năm 2024",
  "subject": "Lập trình Java",
  "semester": "HK1",
  "academicYear": "2024-2025",
  "thumbnailUrl": "https://example.com/thumbnail.jpg"
}

# Lấy danh sách workspace
GET /api/lecturer/workspace-management
X-User-Id: {lecturerId}

# Lấy chi tiết workspace
GET /api/lecturer/workspace-management/{workspaceId}
X-User-Id: {lecturerId}

# Cập nhật workspace
PUT /api/lecturer/workspace-management/{workspaceId}
X-User-Id: {lecturerId}

# Xóa workspace
DELETE /api/lecturer/workspace-management/{workspaceId}
X-User-Id: {lecturerId}
```

### File Management

```http
# Upload tài liệu
POST /api/lecturer/workspace/{notebookId}/files
Content-Type: multipart/form-data
X-User-Id: {lecturerId}

file: [binary file]
chapter: "Chương 1"
purpose: "teaching_material"
notes: "Tài liệu bài giảng chương 1"

# Lấy danh sách tài liệu
GET /api/lecturer/workspace/{notebookId}/files?chapter=Chương 1
X-User-Id: {lecturerId}
```

### AI Content Generation

```http
# Tạo tóm tắt AI
POST /api/lecturer/workspace/{notebookId}/ai/summary
Content-Type: application/json
X-User-Id: {lecturerId}

{
  "fileIds": ["uuid1", "uuid2"],
  "title": "Tóm tắt Chương 1",
  "description": "Tóm tắt nội dung chương 1 về cơ bản Java",
  "chapter": "Chương 1",
  "modelCode": "gemini",
  "customPrompt": "Tóm tắt theo cấu trúc: Khái niệm - Ví dụ - Ứng dụng"
}

# Tạo quiz AI
POST /api/lecturer/workspace/{notebookId}/ai/quiz
Content-Type: application/json
X-User-Id: {lecturerId}

{
  "fileIds": ["uuid1", "uuid2"],
  "title": "Quiz Chương 1",
  "chapter": "Chương 1",
  "quizCount": 15,
  "customPrompt": "Tạo câu hỏi từ cơ bản đến nâng cao"
}

# Tạo flashcard AI
POST /api/lecturer/workspace/{notebookId}/ai/flashcard
Content-Type: application/json
X-User-Id: {lecturerId}

{
  "fileIds": ["uuid1", "uuid2"],
  "title": "Flashcard Chương 1",
  "chapter": "Chương 1",
  "flashcardCount": 25,
  "customPrompt": "Tập trung vào thuật ngữ và khái niệm quan trọng"
}

# Tạo video learning content AI
POST /api/lecturer/workspace/{notebookId}/ai/video
Content-Type: application/json
X-User-Id: {lecturerId}

{
  "fileIds": ["uuid1", "uuid2"],
  "title": "Video Chương 1",
  "chapter": "Chương 1",
  "customPrompt": "Tạo video giải thích từng bước với ví dụ cụ thể"
}

# Lấy danh sách AI content
GET /api/lecturer/workspace/{notebookId}/ai/content?contentType=quiz
X-User-Id: {lecturerId}
```

## 🔧 Implementation Details

### 1. Service Layer Architecture

```java
@Service
public class LecturerWorkspaceService {
    // Reuse existing AI services - NO duplication
    private final SummaryGenerationService summaryGenerationService;
    private final QuizGenerationService quizGenerationService;
    private final FlashcardGenerationService flashcardGenerationService;
    private final VideoGenerationService videoGenerationService;
    
    // File handling - reuse existing
    private final FileStorageService fileStorageService;
    private final FileProcessingTaskService fileProcessingTaskService;
    
    // Core repositories - reuse existing
    private final NotebookRepository notebookRepository;
    private final NotebookFileRepository fileRepository;
    private final NotebookAiSetRepository aiSetRepository;
}
```

### 2. Permission Validation

```java
private void validateLecturerPermission(UUID notebookId, UUID lecturerId) {
    Notebook notebook = getNotebook(notebookId);
    
    // Check if notebook is class type
    if (!"class".equals(notebook.getType())) {
        throw new BadRequestException("Đây không phải là workspace lớp học phần");
    }
    
    // Check lecturer is owner or has lecturer role
    NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, lecturerId)
            .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập workspace này"));
    
    if (!"owner".equals(member.getRole()) && !"lecturer".equals(member.getRole())) {
        throw new ForbiddenException("Chỉ giảng viên mới có quyền sử dụng workspace này");
    }
}
```

### 3. AI Content Generation (Reuse Existing)

```java
public WorkspaceAiResponse generateSummary(UUID notebookId, UUID lecturerId, WorkspaceAiRequest request) {
    // Validate permission
    validateLecturerPermission(notebookId, lecturerId);
    
    // Create AI Set (reuse existing structure)
    NotebookAiSet aiSet = createAiSet(notebookId, lecturerId, "summary", request);
    
    // Use existing summary generation service - EXACT SAME LOGIC
    summaryGenerationService.processSummaryGenerationAsync(
        aiSet.getId(),
        notebookId,
        lecturerId,
        request.getFileIds(),
        "vi-VN-Standard-A", // Default voice
        "vi", // Default language
        request.getCustomPrompt()
    );
    
    return mapToAiResponse(aiSet, "Đang tạo tóm tắt...");
}
```

## 🎯 Key Benefits

### 1. Zero Code Duplication
- **100% tái sử dụng** logic AI từ notebook user
- Không viết lại bất kỳ AI service nào
- Maintain consistency across features

### 2. Scalable Architecture
- Dễ dàng mở rộng cho:
  - Quản lý đề thi
  - Phân phối nội dung cho sinh viên
  - Theo dõi tiến độ học tập
  - Analytics và báo cáo

### 3. Consistent Data Model
- Sử dụng chung cấu trúc database
- Metadata để phân biệt lecturer workspace
- Permissions thông qua NotebookMember

### 4. Future Extensions

```java
// Dễ dàng thêm features mới
public class LecturerWorkspaceService {
    
    // Exam management
    public ExamResponse createExam(UUID notebookId, UUID lecturerId, CreateExamRequest request) {
        // Reuse quiz generation + add exam-specific logic
    }
    
    // Student content distribution
    public void distributeContentToStudents(UUID notebookId, UUID contentId, List<UUID> studentIds) {
        // Use existing notification + permission system
    }
    
    // Progress tracking
    public StudentProgressResponse getStudentProgress(UUID notebookId, UUID studentId) {
        // Use existing AI sets + student interaction data
    }
}
```

## 🔄 Workflow Example

### Giảng viên tạo nội dung cho lớp học

1. **Tạo workspace**
   ```http
   POST /api/lecturer/workspace-management
   # Tạo notebook với type="class"
   ```

2. **Upload tài liệu**
   ```http
   POST /api/lecturer/workspace/{id}/files
   # Upload PDF bài giảng, slides, etc.
   ```

3. **Tạo AI content**
   ```http
   POST /api/lecturer/workspace/{id}/ai/summary
   POST /api/lecturer/workspace/{id}/ai/quiz
   POST /api/lecturer/workspace/{id}/ai/flashcard
   # Sử dụng CHUNG pipeline AI
   ```

4. **Quản lý và chia sẻ**
   ```http
   GET /api/lecturer/workspace/{id}/ai/content
   # Xem tất cả nội dung đã tạo
   # Sau này: chia sẻ cho sinh viên
   ```

## 📈 Extensibility Roadmap

### Phase 1: Core Workspace ✅
- [x] Workspace management (CRUD)
- [x] File upload & management
- [x] AI content generation (summary, quiz, flashcard, video)
- [x] Permission system

### Phase 2: Student Management (Future)
- [ ] Add students to workspace
- [ ] Student role permissions
- [ ] Content visibility control

### Phase 3: Exam System (Future)
- [ ] Create exams from AI quizzes
- [ ] Exam scheduling & distribution
- [ ] Auto grading integration

### Phase 4: Analytics (Future)
- [ ] Student progress tracking
- [ ] Content engagement analytics
- [ ] Performance reports

## 🛠️ Technical Notes

### Database Changes
- **ZERO schema changes** required
- All existing tables support lecturer workspace
- Only metadata fields enhanced

### Code Organization
```
src/main/java/com/example/springboot_api/
├── services/lecturer/
│   ├── LecturerWorkspaceService.java ✅
│   └── LecturerWorkspaceManagementService.java ✅
├── controllers/lecturer/
│   ├── LecturerWorkspaceController.java ✅
│   └── LecturerWorkspaceManagementController.java ✅
└── dto/lecturer/workspace/
    ├── WorkspaceAiRequest.java ✅
    ├── WorkspaceAiResponse.java ✅
    ├── LecturerWorkspaceFileRequest.java ✅
    ├── LecturerWorkspaceFileResponse.java ✅
    ├── CreateWorkspaceRequest.java ✅
    └── WorkspaceResponse.java ✅
```

### Repository Enhancements
- Added methods to existing repositories
- No new repositories needed
- Maintains backward compatibility

## 🎉 Conclusion

Hệ thống Lecturer AI Workspace được thiết kế **tối ưu** để:

1. **Tái sử dụng 100%** logic AI existing
2. **Không duplicate code** nào
3. **Dễ dàng mở rộng** cho các tính năng tương lai
4. **Maintain consistency** với hệ thống hiện tại
5. **Scalable architecture** cho growth

Giảng viên có thể sử dụng tất cả AI features như notebook user, nhưng trong context quản lý lớp học chuyên nghiệp với khả năng mở rộng cho exam management và student distribution.