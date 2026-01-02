# Frontend Prompt: Lecturer Document Upload System

## Mô tả tổng quan
Tạo giao diện upload tài liệu cho giảng viên trong hệ thống tạo câu hỏi AI. Giảng viên có thể upload files PDF/Word vào notebooks để làm nguồn tạo câu hỏi thi.

## Luồng hoạt động chính

### 1. Khởi tạo trang
- Load danh sách notebooks mà giảng viên có quyền truy cập
- Hiển thị dropdown/select để chọn notebook
- Mỗi notebook hiển thị: tên, số files hiện có, loại notebook

### 2. Chọn notebook
- Khi chọn notebook, tự động load danh sách files hiện có
- Hiển thị files với thông tin: tên file, kích thước, trạng thái xử lý, preview nội dung
- Cho phép xem chi tiết file, xóa file

### 3. Upload files
- Drag & drop hoặc click để chọn files
- Validate file types (chỉ PDF, DOC, DOCX)
- Hiển thị preview files trước khi upload
- Progress bar khi upload
- Thông báo thành công/lỗi

### 4. Quản lý files
- Refresh danh sách sau khi upload
- Xóa files không cần thiết
- Xem preview nội dung file
- Filter/search files theo tên

## API Endpoints

### Base URL
```
http://localhost:8386
```

### Authentication
Tất cả requests cần header:
```
Authorization: Bearer <jwt-token>
```

### 1. Lấy danh sách notebooks
```
GET /lecturer/notebooks/accessible

Response:
[
  {
    "id": "notebook-uuid",
    "title": "Java Programming Course",
    "description": "Materials for Java programming course",
    "type": "class",
    "totalFiles": 15,
    "readyFiles": 12,
    "classId": "class-uuid",
    "className": "IT001.01",
    "subjectCode": "IT001",
    "subjectName": "Java Programming"
  }
]
```

### 2. Lấy files theo notebook
```
GET /lecturer/notebooks/{notebookId}/files?search={searchTerm}

Response:
[
  {
    "id": "file-uuid",
    "originalFilename": "Java_Chapter1.pdf",
    "mimeType": "application/pdf",
    "fileSize": 2048576,
    "status": "done",
    "ocrDone": true,
    "embeddingDone": true,
    "createdAt": "2024-12-30T10:00:00",
    "notebookId": "notebook-uuid",
    "notebookTitle": "Java Programming Course",
    "notebookType": "class",
    "uploadedBy": {
      "id": "user-uuid",
      "fullName": "Dr. Nguyễn Văn A",
      "email": "lecturer@university.edu"
    },
    "chunksCount": 45,
    "contentPreview": "Chương 1: Giới thiệu về Java..."
  }
]
```

### 3. Upload files (Simple - Khuyến nghị)
```
POST /lecturer/notebooks/{notebookId}/files/simple
Content-Type: multipart/form-data

Body:
- files: MultipartFile[] (PDF, DOC, DOCX files)

Response: Array of uploaded files (same structure as get files)
```

### 4. Upload files (Advanced - Optional)
```
POST /lecturer/notebooks/{notebookId}/files
Content-Type: multipart/form-data

Body:
- request: JSON string {"chunkSize": 3000, "chunkOverlap": 250}
- files: MultipartFile[] (PDF, DOC, DOCX files)

Response: Array of uploaded files
```

### 5. Xem chi tiết file
```
GET /lecturer/notebooks/{notebookId}/files/{fileId}

Response:
{
  "id": "file-uuid",
  "originalFilename": "Java_Chapter1.pdf",
  "mimeType": "application/pdf",
  "fileSize": 2048576,
  "status": "done",
  "ocrDone": true,
  "embeddingDone": true,
  "createdAt": "2024-12-30T10:00:00",
  "notebookId": "notebook-uuid",
  "notebookTitle": "Java Programming Course",
  "uploadedBy": {
    "id": "user-uuid",
    "fullName": "Dr. Nguyễn Văn A",
    "email": "lecturer@university.edu"
  },
  "contentSummary": "Chương 1: Giới thiệu về Java...",
  "totalChunks": 45,
  "firstChunkContent": "Nội dung đầy đủ chunk đầu tiên...",
  "chunkSize": 3000,
  "chunkOverlap": 250
}
```

### 6. Xóa file
```
DELETE /lecturer/notebooks/{notebookId}/files/{fileId}

Response: 204 No Content
```

## Trạng thái files

### File Status
- **"approved"**: File đã được duyệt, đang xử lý
- **"processing"**: Đang xử lý AI (OCR, embedding)
- **"done"**: Đã xử lý xong, sẵn sàng tạo câu hỏi
- **"failed"**: Xử lý thất bại

### Processing Status
- **ocrDone**: true/false - Đã trích xuất text từ file
- **embeddingDone**: true/false - Đã tạo vector embedding

## Validation Rules

### File Upload
- Chỉ chấp nhận: PDF (.pdf), Word (.doc, .docx)
- Kích thước tối đa: 100MB per file
- Tối đa 10 files per upload

### UI Validation
- Hiển thị lỗi nếu file type không hợp lệ
- Hiển thị warning nếu file quá lớn
- Disable upload button khi đang upload

## Error Handling

### Common Errors
```json
{
  "status": 400,
  "message": "Chỉ chấp nhận file PDF và Word (.doc, .docx)",
  "timestamp": "2024-12-30T10:00:00"
}

{
  "status": 401,
  "message": "Token không hợp lệ",
  "timestamp": "2024-12-30T10:00:00"
}

{
  "status": 403,
  "message": "Bạn không có quyền truy cập notebook này",
  "timestamp": "2024-12-30T10:00:00"
}

{
  "status": 404,
  "message": "Notebook không tồn tại",
  "timestamp": "2024-12-30T10:00:00"
}
```

## UX Requirements

### Loading States
- Skeleton loading khi load notebooks
- Spinner khi load files
- Progress bar khi upload
- Loading overlay khi xử lý

### Success/Error Messages
- Toast notification cho upload thành công
- Error alerts cho validation failures
- Confirmation dialog trước khi xóa file

### File Display
- Grid/list view toggle
- File icons theo type (PDF, Word)
- File size formatting (KB, MB)
- Upload date formatting
- Processing status indicators

### Search & Filter
- Real-time search trong danh sách files
- Filter theo file type
- Filter theo trạng thái xử lý
- Sort theo tên, ngày, kích thước

## Integration Notes

### State Management
- Notebook selection state
- Files list state  
- Upload progress state
- Error/success message state

### API Integration
- Implement retry logic cho failed uploads
- Handle file upload progress
- Implement proper error boundaries
- Cache notebooks list

### Performance
- Lazy load file content previews
- Pagination cho danh sách files lớn
- Debounce search input
- Optimize re-renders

## Security Notes
- Validate JWT token expiry
- Handle 401 errors với redirect to login
- Sanitize file names display
- Validate file content types (not just extensions)