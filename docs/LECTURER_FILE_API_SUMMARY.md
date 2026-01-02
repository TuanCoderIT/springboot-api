# Lecturer Notebook File API - Summary

## Tổng Quan
Đã tạo API riêng cho lecturer để quản lý files notebook, tối ưu cho việc tạo câu hỏi AI. API này rút gọn so với admin API, chỉ tập trung vào những tính năng cần thiết.

## Files Đã Tạo

### 1. Controller
- `src/main/java/com/example/springboot_api/controllers/lecturer/LecturerNotebookFileController.java`
- Endpoints:
  - `GET /lecturer/notebooks/accessible` - Lấy notebooks có quyền truy cập
  - `POST /lecturer/notebooks/{notebookId}/files` - Upload files
  - `GET /lecturer/notebooks/{notebookId}/files` - Lấy files theo notebook
  - `GET /lecturer/notebooks/files` - Lấy tất cả files accessible
  - `GET /lecturer/notebooks/{notebookId}/files/{fileId}` - Chi tiết file
  - `DELETE /lecturer/notebooks/{notebookId}/files/{fileId}` - Xóa file

### 2. DTOs
- `LecturerNotebookFileResponse.java` - Response cho file list
- `LecturerNotebookSummary.java` - Summary notebook info
- `LecturerFileDetailResponse.java` - Chi tiết file với content preview

### 3. Service
- `LecturerNotebookFileService.java` - Business logic cho lecturer file management
  - Upload files với auto-approve
  - Delete files với permission check
  - File validation và processing

## Ưu Điểm So Với User API

### 1. Phân Quyền Rõ Ràng
- API riêng cho lecturer với logic phân quyền phù hợp
- Có thể truy cập files từ nhiều notebooks (classes mà lecturer dạy)

### 2. Thông Tin Tối Ưu
- **Content Preview**: 200 ký tự đầu để lecturer biết nội dung file
- **Chunks Count**: Biết file có bao nhiều chunks (độ dài nội dung)
- **Notebook Info**: Hiển thị notebook title, type để dễ phân biệt
- **Ready Files Only**: Chỉ lấy files có status = 'done' để đảm bảo có thể dùng

### 3. Multiple Access Patterns
- Lấy files theo notebook cụ thể
- Lấy tất cả files accessible (cross-notebook)
- Lấy danh sách notebooks để chọn

### 4. Content Preview
- **Content Summary**: Tóm tắt từ 3 chunks đầu (max 1000 chars)
- **First Chunk**: Nội dung chunk đầu tiên để preview
- **Metadata**: Chunk size, overlap để hiểu cấu trúc

## So Sánh APIs

| Feature | User API | Lecturer API | Admin API |
|---------|----------|--------------|-----------|
| Scope | Single notebook | Multiple notebooks | All notebooks |
| File Status | All statuses | Done/Approved only | All statuses |
| Content Preview | ❌ | ✅ | ❌ |
| Cross-notebook | ❌ | ✅ | ✅ |
| Upload Files | ✅ | ✅ | ✅ |
| Delete Files | ✅ (own only) | ✅ (with permission) | ✅ |
| Approve/Reject | ❌ | ❌ | ✅ |
| Notebook List | ❌ | ✅ | ✅ |
| Auto Processing | ✅ | ✅ | ✅ |

## Frontend Integration

### Workflow Mới
```javascript
// 1. Lấy danh sách notebooks
const notebooks = await fetchAccessibleNotebooks();

// 2. Hiển thị dropdown chọn notebook
<select onChange={handleNotebookChange}>
  {notebooks.map(nb => (
    <option key={nb.id} value={nb.id}>
      {nb.title} ({nb.readyFiles}/{nb.totalFiles} files)
    </option>
  ))}
</select>

// 3. Load files theo notebook đã chọn
const files = await fetchNotebookFiles(selectedNotebookId);

// 4. Upload files mới nếu cần (với cấu hình)
const handleFileUpload = async (selectedFiles) => {
  const uploadedFiles = await uploadFiles(selectedNotebookId, selectedFiles, {
    chunkSize: 3500,
    chunkOverlap: 300
  });
  // Refresh file list
  const updatedFiles = await fetchNotebookFiles(selectedNotebookId);
  setFiles(updatedFiles);
};

// 5. Hiển thị files với preview và actions
{files.map(file => (
  <div key={file.id}>
    <h4>{file.originalFilename}</h4>
    <p>Chunks: {file.chunksCount}</p>
    <p>Preview: {file.contentPreview}</p>
    <small>From: {file.notebookTitle}</small>
    <button onClick={() => handleDelete(file.id)}>Xóa</button>
  </div>
))}
```

### Error Handling
```javascript
try {
  const files = await fetchNotebookFiles(notebookId);
} catch (error) {
  if (error.status === 403) {
    showError("Bạn không có quyền truy cập notebook này");
  } else if (error.status === 404) {
    showError("Notebook không tồn tại");
  }
}
```

## TODO - Cần Implement

### 1. Permission Logic
- Kiểm tra lecturer có quyền truy cập notebook không
- Logic để lấy notebooks mà lecturer dạy
- Xử lý notebooks public/community

### 2. Repository Methods
- Cần thêm methods trong repository để query theo lecturer permissions
- Optimize queries cho performance

### 3. Caching
- Cache danh sách notebooks accessible
- Cache file content previews

## Migration Guide

### Từ User API sang Lecturer API
```javascript
// CŨ
const files = await fetch(`/user/notebooks/${notebookId}/files`);

// MỚI  
const notebooks = await fetch('/lecturer/notebooks/accessible');
const files = await fetch(`/lecturer/notebooks/${notebookId}/files`);
```

### Benefits
1. **Better UX**: Lecturer thấy được content preview trước khi chọn
2. **Cross-notebook**: Có thể chọn files từ nhiều notebooks
3. **Optimized**: Chỉ load files ready để dùng
4. **Contextual**: Biết file thuộc notebook nào

API mới này sẽ cải thiện đáng kể trải nghiệm tạo câu hỏi AI cho lecturer.