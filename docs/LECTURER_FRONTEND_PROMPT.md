# 🎯 PROMPT FRONTEND – LECTURER AI WORKSPACE (Next.js)

## 📋 Context
Xây dựng giao diện Next.js cho **Lecturer AI Workspace** - hệ thống quản lý workspace và tạo AI content cho giảng viên. Backend đã hoàn thành với đầy đủ API endpoints.

## 🏗️ Architecture Overview

### Core Concept
- **Workspace = Lớp học phần** (Notebook với type="class")
- **Giảng viên** quản lý workspace, upload tài liệu, tạo AI content
- **AI Content**: Summary, Quiz, Flashcard, Video (tái sử dụng logic existing)
- **Chapter-based organization** cho tài liệu và nội dung

### Main User Flows
1. **Workspace Management**: CRUD workspace (lớp học phần)
2. **File Management**: Upload & organize documents by chapter
3. **AI Content Generation**: Create educational content from documents
4. **Content Organization**: View & manage all created content

## 🚀 API Endpoints & Types

### Base URL
```typescript
const API_BASE = 'http://localhost:8386/api/lecturer'
```

### 1. Workspace Management APIs

```typescript
// Workspace CRUD
POST   /workspace-management              // Create workspace
GET    /workspace-management              // List workspaces  
GET    /workspace-management/{id}         // Get workspace details
PUT    /workspace-management/{id}         // Update workspace
DELETE /workspace-management/{id}         // Delete workspace

// Request/Response Types
interface CreateWorkspaceRequest {
  title: string;                    // "Lập trình Java - HK1 2024"
  description?: string;             // Course description
  subject?: string;                 // "Lập trình Java"
  semester?: string;                // "HK1"
  academicYear?: string;            // "2024-2025"
  thumbnailUrl?: string;            // Course thumbnail
}

interface WorkspaceResponse {
  id: string;                       // UUID
  title: string;
  description?: string;
  thumbnailUrl?: string;
  subject?: string;
  semester?: string;
  academicYear?: string;
  createdAt: string;                // ISO date
  updatedAt: string;                // ISO date
}
```

### 2. File Management APIs

```typescript
// File operations
POST   /workspace/{id}/files              // Upload document
GET    /workspace/{id}/files              // List documents
GET    /workspace/{id}/files?chapter=X    // Filter by chapter

// Request/Response Types
interface LecturerWorkspaceFileRequest {
  file: File;                       // Multipart file
  chapter?: string;                 // "Chương 1"
  purpose?: string;                 // "teaching_material"
  notes?: string;                   // Additional notes
}

interface LecturerWorkspaceFileResponse {
  id: string;                       // UUID
  fileName: string;                 // Original filename
  fileUrl: string;                  // Download URL
  fileSize: number;                 // Bytes
  mimeType: string;                 // "application/pdf"
  status: string;                   // "uploaded", "processing", "done", "failed"
  chapter?: string;                 // Chapter name
  purpose?: string;                 // File purpose
  uploadedAt: string;               // ISO date
}
```

### 3. AI Content Generation APIs

```typescript
// AI content generation
POST   /workspace/{id}/ai/summary         // Generate summary
POST   /workspace/{id}/ai/quiz            // Generate quiz
POST   /workspace/{id}/ai/flashcard       // Generate flashcard
POST   /workspace/{id}/ai/video           // Generate video
GET    /workspace/{id}/ai/content         // List AI content
GET    /workspace/{id}/ai/content?contentType=quiz  // Filter by type

// Request/Response Types
interface WorkspaceAiRequest {
  fileIds: string[];                // Selected file UUIDs
  title?: string;                   // Content title
  description?: string;             // Content description
  chapter?: string;                 // Related chapter
  modelCode?: string;               // "gemini" | "groq"
  customPrompt?: string;            // Custom AI prompt
  quizCount?: number;               // For quiz (1-50)
  flashcardCount?: number;          // For flashcard (1-100)
}

interface WorkspaceAiResponse {
  id: string;                       // UUID
  contentType: string;              // "summary" | "quiz" | "flashcard" | "video"
  title: string;                    // Content title
  description?: string;             // Content description
  status: string;                   // "queued" | "processing" | "completed" | "failed"
  statusMessage: string;            // User-friendly status
  modelCode: string;                // AI model used
  chapter?: string;                 // Related chapter
  createdAt: string;                // ISO date
  finishedAt?: string;              // ISO date (if completed)
}
```

### 4. Common Types

```typescript
// API Response wrapper
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// Error handling
interface ApiError {
  success: false;
  message: string;
  error?: string;
}

// Headers for all requests
const headers = {
  'X-User-Id': lecturerId,          // UUID string
  'Content-Type': 'application/json'
}
```

## 📱 Required Pages & Components

### 1. Workspace List Page (`/lecturer/workspaces`)
**Purpose**: Display all lecturer's workspaces
**Features**:
- Grid/list view of workspaces
- Create new workspace button
- Search/filter workspaces
- Quick actions (edit, delete, enter)

### 2. Workspace Detail Page (`/lecturer/workspaces/[id]`)
**Purpose**: Main workspace dashboard
**Features**:
- Workspace info header (title, subject, semester)
- Navigation tabs: Files, AI Content, Settings
- Quick stats (file count, AI content count)
- Recent activity feed

### 3. File Management Tab
**Purpose**: Upload and organize documents
**Features**:
- File upload area (drag & drop)
- Chapter organization (filter/group by chapter)
- File list with status indicators
- File actions (download, delete, view details)
- Bulk operations

### 4. AI Content Generation Tab
**Purpose**: Create and manage AI content
**Features**:
- Content type selector (Summary, Quiz, Flashcard, Video)
- File selection for AI input
- Generation form with custom prompts
- Content list with status tracking
- Preview/view generated content

### 5. AI Generation Modal/Form
**Purpose**: Configure AI content generation
**Features**:
- File selection (multi-select from uploaded files)
- Content type specific options (quiz count, flashcard count)
- Chapter assignment
- Custom prompt input
- Model selection (Gemini, Groq)
- Progress tracking during generation

## 🔄 User Workflow Examples

### Workflow 1: Create New Course Workspace
```
1. Navigate to /lecturer/workspaces
2. Click "Create Workspace"
3. Fill form: title, subject, semester, description
4. Submit → Redirect to workspace detail page
```

### Workflow 2: Upload Course Materials
```
1. In workspace detail → Files tab
2. Drag & drop PDF files or click upload
3. For each file: assign chapter, set purpose
4. Files process automatically (OCR, embedding)
5. View file list with processing status
```

### Workflow 3: Generate AI Quiz
```
1. In workspace detail → AI Content tab
2. Click "Generate Quiz"
3. Select files for quiz generation
4. Configure: quiz count, chapter, custom prompt
5. Submit → Track generation progress
6. View completed quiz when ready
```

### Workflow 4: Organize by Chapter
```
1. Filter files by chapter dropdown
2. Filter AI content by chapter
3. Bulk assign chapter to multiple files
4. Chapter-based content organization
```

## 🎨 UI/UX Requirements

### Design Principles
- **Professional**: Clean, academic-focused design
- **Efficient**: Quick access to common actions
- **Organized**: Clear chapter-based organization
- **Status-aware**: Clear indicators for processing states

### Key UI Elements
- **Status badges**: For file processing and AI generation states
- **Progress indicators**: For long-running AI operations
- **Chapter tags**: Visual chapter organization
- **Content type icons**: Distinguish summary, quiz, flashcard, video
- **Quick actions**: Hover/context menus for common operations

### Responsive Considerations
- **Desktop-first**: Primary use case for lecturers
- **Tablet support**: Secondary priority
- **Mobile**: Basic viewing only

## 🔧 Technical Requirements

### State Management
- **React Query/SWR**: For API data fetching and caching
- **Zustand/Context**: For UI state (selected files, filters)
- **Form handling**: React Hook Form for complex forms

### File Upload
- **Drag & drop**: Modern file upload experience
- **Progress tracking**: Upload progress indicators
- **Validation**: File type, size validation
- **Chunked upload**: For large files (optional)

### Real-time Updates
- **Polling**: Check AI generation status periodically
- **WebSocket** (optional): Real-time status updates
- **Optimistic updates**: Immediate UI feedback

### Error Handling
- **API errors**: User-friendly error messages
- **Network errors**: Retry mechanisms
- **Validation errors**: Form field validation
- **Loading states**: Skeleton screens, spinners

## 📊 Data Flow Examples

### File Upload Flow
```typescript
// 1. User selects files
const handleFileUpload = async (files: File[], chapter: string) => {
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('chapter', chapter);
    formData.append('purpose', 'teaching_material');
    
    const response = await fetch(`/workspace/${workspaceId}/files`, {
      method: 'POST',
      headers: { 'X-User-Id': lecturerId },
      body: formData
    });
    
    // Update UI with upload result
  }
};
```

### AI Generation Flow
```typescript
// 1. User configures AI generation
const generateQuiz = async (request: WorkspaceAiRequest) => {
  const response = await fetch(`/workspace/${workspaceId}/ai/quiz`, {
    method: 'POST',
    headers: { 'X-User-Id': lecturerId, 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });
  
  const aiContent = await response.json();
  
  // 2. Poll for completion
  const pollStatus = setInterval(async () => {
    const statusResponse = await fetch(`/workspace/${workspaceId}/ai/content`);
    const contents = await statusResponse.json();
    const current = contents.data.find(c => c.id === aiContent.data.id);
    
    if (current.status === 'completed' || current.status === 'failed') {
      clearInterval(pollStatus);
      // Update UI with final result
    }
  }, 3000);
};
```

## 🎯 Success Criteria

### Functional Requirements
- ✅ Create/manage workspaces (CRUD operations)
- ✅ Upload files with chapter organization
- ✅ Generate AI content (summary, quiz, flashcard, video)
- ✅ Track processing status in real-time
- ✅ Organize content by chapters
- ✅ Professional, intuitive UI for lecturers

### Performance Requirements
- ✅ Fast page loads (<2s)
- ✅ Smooth file uploads with progress
- ✅ Responsive UI during AI generation
- ✅ Efficient data fetching and caching

### User Experience Requirements
- ✅ Intuitive workflow for course content creation
- ✅ Clear status indicators for all operations
- ✅ Easy chapter-based organization
- ✅ Professional academic interface
- ✅ Error handling with helpful messages

## 🚀 Implementation Priority

### Phase 1: Core Workspace Management
1. Workspace list and CRUD operations
2. Basic file upload and management
3. Simple AI content generation

### Phase 2: Enhanced Features
1. Chapter-based organization
2. Advanced AI generation options
3. Real-time status updates
4. Content preview/viewing

### Phase 3: Polish & Optimization
1. Advanced UI/UX improvements
2. Performance optimizations
3. Error handling enhancements
4. Mobile responsiveness

---

**Note**: Backend APIs are fully implemented and tested. Focus on creating a professional, efficient interface that leverages the complete AI pipeline for educational content creation.