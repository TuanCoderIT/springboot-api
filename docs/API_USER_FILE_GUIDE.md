# API Hướng dẫn - User Notebook File Management

Tài liệu này hướng dẫn cách sử dụng các API endpoints để quản lý files trong notebook cho user.

## Base URL

```
/user/notebooks/{notebookId}/files
```

## Authentication

Tất cả các endpoints đều yêu cầu authentication token trong header:

```
Authorization: Bearer {token}
```

---

## 1. Upload Files

Upload một hoặc nhiều file vào notebook.

### Endpoint

```
POST /user/notebooks/{notebookId}/files
```

### Headers

| Key           | Value                        |
| ------------- | ---------------------------- |
| Authorization | Bearer {token}               |
| Content-Type  | multipart/form-data          |

### Path Parameters

| Tên        | Kiểu | Mô tả           |
| ---------- | ---- | --------------- |
| notebookId | UUID | ID của notebook |

### Request Body

Form data với key `files` (có thể upload nhiều files):

```
files: File[] (PDF, DOC, DOCX)
```

**Lưu ý:**
- Chỉ chấp nhận file PDF và Word (.doc, .docx)
- Phải chọn ít nhất một file

### Response (201 Created)

```typescript
NotebookFileResponse[]
```

### Ví dụ (TypeScript/React)

```typescript
interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  storageUrl: string;
  status: 'pending' | 'approved' | 'rejected' | 'processing' | 'done' | 'failed';
  pagesCount: number | null;
  ocrDone: boolean;
  embeddingDone: boolean;
  chunkSize: number;
  chunkOverlap: number;
  chunksCount: number | null;
  uploadedBy: {
    id: string;
    fullName: string;
    email: string;
    avatarUrl: string | null;
  } | null;
  notebook: {
    id: string;
    title: string;
    description: string;
    type: string;
    visibility: string;
    thumbnailUrl: string | null;
  } | null;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}

async function uploadFiles(
  notebookId: string,
  files: File[],
  token: string
): Promise<NotebookFileResponse[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await fetch(
    `/user/notebooks/${notebookId}/files`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to upload files');
  }

  return response.json();
}

// Sử dụng
const handleUpload = async (files: FileList | null) => {
  if (!files || files.length === 0) return;

  const fileArray = Array.from(files);
  try {
    const result = await uploadFiles(notebookId, fileArray, token);
    console.log('Uploaded files:', result);
  } catch (error) {
    console.error('Upload error:', error);
  }
};
```

---

## 2. Get List of Files

Lấy danh sách files trong notebook.

### Endpoint

```
GET /user/notebooks/{notebookId}/files
GET /user/notebooks/{notebookId}/files?search={keyword}
```

### Path Parameters

| Tên        | Kiểu | Mô tả           |
| ---------- | ---- | --------------- |
| notebookId | UUID | ID của notebook |

### Query Parameters

| Tên    | Kiểu   | Bắt buộc | Mô tả                    |
| ------ | ------ | -------- | ------------------------ |
| search | String | No       | Tìm kiếm theo tên file    |

### Response (200 OK)

```typescript
NotebookFileResponse[]
```

**Lưu ý:**
- Trả về file có status = 'done' (đã duyệt và xử lý xong)
- Trả về file của user hiện tại với các status khác (pending, failed, rejected, processing)

### Ví dụ (TypeScript/React)

```typescript
async function getFiles(
  notebookId: string,
  search?: string,
  token: string
): Promise<NotebookFileResponse[]> {
  const url = search
    ? `/user/notebooks/${notebookId}/files?search=${encodeURIComponent(search)}`
    : `/user/notebooks/${notebookId}/files`;

  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to fetch files');
  }

  return response.json();
}

// Sử dụng
const files = await getFiles(notebookId, undefined, token);
const searchResults = await getFiles(notebookId, 'document', token);
```

---

## 3. Get File Detail

Lấy chi tiết thông tin của một file.

### Endpoint

```
GET /user/notebooks/{notebookId}/files/{fileId}
```

### Path Parameters

| Tên        | Kiểu | Mô tả               |
| ---------- | ---- | ------------------- |
| notebookId | UUID | ID của notebook     |
| fileId     | UUID | ID của file cần lấy |

### Response (200 OK)

```typescript
interface UserNotebookFileDetailResponse {
  fileInfo: NotebookFileResponse;
  fullContent: string; // Toàn bộ text content (hiện tại là empty string)
  generatedContentCounts: {
    video: number;
    podcast: number;
    flashcard: number;
    quiz: number;
  };
  contributor: {
    id: string;
    fullName: string;
    email: string;
    avatarUrl: string | null;
  } | null;
}
```

### Ví dụ Response

```json
{
  "fileInfo": {
    "id": "6523c2d1-c4dc-4173-8b56-3353057096a8",
    "originalFilename": "document.pdf",
    "mimeType": "application/pdf",
    "fileSize": 16110,
    "storageUrl": "http://localhost:8386/uploads/0553dd93-099a-472c-a9ef-939483ea8801.pdf",
    "status": "done",
    "pagesCount": 10,
    "ocrDone": true,
    "embeddingDone": true,
    "chunkSize": 3000,
    "chunkOverlap": 250,
    "chunksCount": null,
    "uploadedBy": {
      "id": "991c40a1-c2b1-4e62-972a-33deafd708ff",
      "fullName": "John Doe",
      "email": "john@example.com",
      "avatarUrl": "http://localhost:8386/uploads/avatar.jpg"
    },
    "notebook": {
      "id": "c3a7f558-faa7-4218-ae41-4ef57f976f34",
      "title": "My Notebook",
      "description": "Description here",
      "type": "community",
      "visibility": "public",
      "thumbnailUrl": "http://localhost:8386/uploads/thumbnail.jpeg"
    },
    "createdAt": "2025-12-03T14:48:59.628486Z",
    "updatedAt": "2025-12-03T15:07:45.887456Z"
  },
  "fullContent": "",
  "generatedContentCounts": {
    "video": 2,
    "podcast": 1,
    "flashcard": 5,
    "quiz": 3
  },
  "contributor": {
    "id": "991c40a1-c2b1-4e62-972a-33deafd708ff",
    "fullName": "John Doe",
    "email": "john@example.com",
    "avatarUrl": "http://localhost:8386/uploads/avatar.jpg"
  }
}
```

### Ví dụ (TypeScript/React)

```typescript
interface UserNotebookFileDetailResponse {
  fileInfo: NotebookFileResponse;
  fullContent: string;
  generatedContentCounts: {
    video: number;
    podcast: number;
    flashcard: number;
    quiz: number;
  };
  contributor: {
    id: string;
    fullName: string;
    email: string;
    avatarUrl: string | null;
  } | null;
}

async function getFileDetail(
  notebookId: string,
  fileId: string,
  token: string
): Promise<UserNotebookFileDetailResponse> {
  const response = await fetch(
    `/user/notebooks/${notebookId}/files/${fileId}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to fetch file detail');
  }

  return response.json();
}

// Sử dụng
const fileDetail = await getFileDetail(notebookId, fileId, token);
console.log('File info:', fileDetail.fileInfo);
console.log('Generated content:', fileDetail.generatedContentCounts);
```

---

## 4. Get File Chunks

Lấy danh sách chunks (phân đoạn text) của một file với text content.

### Endpoint

```
GET /user/notebooks/{notebookId}/files/{fileId}/chunks
```

### Path Parameters

| Tên        | Kiểu | Mô tả               |
| ---------- | ---- | ------------------- |
| notebookId | UUID | ID của notebook     |
| fileId     | UUID | ID của file cần lấy |

### Response (200 OK)

```typescript
FileChunkResponse[]
```

### Ví dụ Response

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkIndex": 0,
    "content": "Đây là nội dung của chunk đầu tiên. Chunk này chứa phần đầu của tài liệu..."
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "chunkIndex": 1,
    "content": "Đây là nội dung của chunk thứ hai. Chunk này tiếp tục từ chunk trước với một phần overlap..."
  },
  {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "chunkIndex": 2,
    "content": "Đây là nội dung của chunk thứ ba..."
  }
]
```

### Ví dụ (TypeScript/React)

```typescript
interface FileChunkResponse {
  id: string;
  chunkIndex: number;
  content: string;
}

async function getFileChunks(
  notebookId: string,
  fileId: string,
  token: string
): Promise<FileChunkResponse[]> {
  const response = await fetch(
    `/user/notebooks/${notebookId}/files/${fileId}/chunks`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to fetch file chunks');
  }

  return response.json();
}

// Sử dụng
const chunks = await getFileChunks(notebookId, fileId, token);
console.log(`Total chunks: ${chunks.length}`);

// Hiển thị từng chunk
chunks.forEach((chunk, index) => {
  console.log(`Chunk ${chunk.chunkIndex}:`, chunk.content);
});

// Lấy full content bằng cách join tất cả chunks
const fullContent = chunks.map(chunk => chunk.content).join('\n\n');
console.log('Full content:', fullContent);
```

### React Component Example

```typescript
import React, { useState, useEffect } from 'react';

interface FileChunkViewerProps {
  notebookId: string;
  fileId: string;
  token: string;
}

const FileChunkViewer: React.FC<FileChunkViewerProps> = ({
  notebookId,
  fileId,
  token,
}) => {
  const [chunks, setChunks] = useState<FileChunkResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchChunks = async () => {
      try {
        setLoading(true);
        const data = await getFileChunks(notebookId, fileId, token);
        setChunks(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load chunks');
      } finally {
        setLoading(false);
      }
    };

    fetchChunks();
  }, [notebookId, fileId, token]);

  if (loading) return <div>Loading chunks...</div>;
  if (error) return <div>Error: {error}</div>;
  if (chunks.length === 0) return <div>No chunks found</div>;

  return (
    <div>
      <h3>File Chunks ({chunks.length} chunks)</h3>
      {chunks.map((chunk) => (
        <div key={chunk.id} style={{ marginBottom: '20px', padding: '10px', border: '1px solid #ccc' }}>
          <h4>Chunk {chunk.chunkIndex}</h4>
          <p>{chunk.content}</p>
        </div>
      ))}
    </div>
  );
};

export default FileChunkViewer;
```

---

## Error Handling

Tất cả các endpoints có thể trả về các lỗi sau:

### 401 Unauthorized
```json
{
  "message": "User chưa đăng nhập."
}
```

### 400 Bad Request
```json
{
  "message": "Vui lòng chọn ít nhất một file để upload."
}
```

```json
{
  "message": "Chỉ chấp nhận file PDF và Word (.doc, .docx). File không hợp lệ: example.txt"
}
```

```json
{
  "message": "Bạn chưa tham gia nhóm này"
}
```

### 404 Not Found
```json
{
  "message": "Notebook không tồn tại"
}
```

```json
{
  "message": "File không tồn tại"
}
```

### Ví dụ Error Handling

```typescript
async function handleApiCall<T>(
  apiCall: () => Promise<T>
): Promise<T> {
  try {
    return await apiCall();
  } catch (error) {
    if (error instanceof Error) {
      // Handle specific error messages
      if (error.message.includes('chưa đăng nhập')) {
        // Redirect to login
        window.location.href = '/login';
      } else if (error.message.includes('không tồn tại')) {
        // Show not found message
        alert('Không tìm thấy tài nguyên');
      } else {
        // Show generic error
        alert(`Lỗi: ${error.message}`);
      }
    }
    throw error;
  }
}

// Sử dụng
const files = await handleApiCall(() => getFiles(notebookId, undefined, token));
```

---

## Tóm tắt Endpoints

| Method | Endpoint                                    | Mô tả                    |
| ------ | ------------------------------------------- | ------------------------ |
| POST   | `/user/notebooks/{notebookId}/files`        | Upload files             |
| GET    | `/user/notebooks/{notebookId}/files`        | Lấy danh sách files      |
| GET    | `/user/notebooks/{notebookId}/files/{fileId}` | Lấy chi tiết file      |
| GET    | `/user/notebooks/{notebookId}/files/{fileId}/chunks` | Lấy chunks với content |

---

## Lưu ý

1. **File Types**: Chỉ chấp nhận PDF và Word (.doc, .docx)
2. **Permissions**: User phải là member của notebook (status = 'approved')
3. **Status**: 
   - `pending`: Đang chờ duyệt (chỉ hiển thị cho user upload)
   - `approved`: Đã duyệt, đang xử lý
   - `processing`: Đang xử lý OCR/embedding
   - `done`: Đã hoàn thành
   - `failed`: Xử lý thất bại
   - `rejected`: Bị từ chối
4. **Chunks**: Chunks được sắp xếp theo `chunkIndex` từ nhỏ đến lớn
5. **Content**: Text content trong chunks có thể rất dài, cần xử lý hiển thị phù hợp

