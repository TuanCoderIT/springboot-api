# API Hướng dẫn - Lấy File Chunks

Tài liệu này hướng dẫn cách sử dụng 2 API endpoints để lấy thông tin chunks (phân đoạn text) của file.

## Base URL

```
/user/notebooks/{notebookId}/files/{fileId}
```

## Authentication

Tất cả các endpoints đều yêu cầu authentication token trong header:

```
Authorization: Bearer {token}
```

---

## 1. Get File Detail (Lấy chi tiết file)

Lấy thông tin chi tiết của một file, bao gồm thông tin file, số lượng content đã generate, và thông tin người upload.

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
    "id": "f4a552b4-17a4-40b4-a602-3d1d6a2b3c2b",
    "originalFilename": "document.pdf",
    "mimeType": "application/pdf",
    "fileSize": 16110,
    "storageUrl": "http://localhost:8386/uploads/document.pdf",
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
interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  storageUrl: string;
  status:
    | "pending"
    | "approved"
    | "rejected"
    | "processing"
    | "done"
    | "failed";
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
  createdAt: string;
  updatedAt: string;
}

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
    throw new Error(error.message || "Failed to fetch file detail");
  }

  return response.json();
}

// Sử dụng
const fileDetail = await getFileDetail(notebookId, fileId, token);
console.log("File info:", fileDetail.fileInfo);
console.log("Generated content:", fileDetail.generatedContentCounts);
console.log("Contributor:", fileDetail.contributor);
```

### React Component Example

```typescript
import React, { useState, useEffect } from "react";

interface FileDetailViewerProps {
  notebookId: string;
  fileId: string;
  token: string;
}

const FileDetailViewer: React.FC<FileDetailViewerProps> = ({
  notebookId,
  fileId,
  token,
}) => {
  const [fileDetail, setFileDetail] =
    useState<UserNotebookFileDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchFileDetail = async () => {
      try {
        setLoading(true);
        const data = await getFileDetail(notebookId, fileId, token);
        setFileDetail(data);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load file detail"
        );
      } finally {
        setLoading(false);
      }
    };

    fetchFileDetail();
  }, [notebookId, fileId, token]);

  if (loading) return <div>Loading file detail...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!fileDetail) return <div>No file detail found</div>;

  return (
    <div>
      <h2>{fileDetail.fileInfo.originalFilename}</h2>
      <p>Status: {fileDetail.fileInfo.status}</p>
      <p>Size: {(fileDetail.fileInfo.fileSize / 1024).toFixed(2)} KB</p>

      <div>
        <h3>Generated Content</h3>
        <ul>
          <li>Videos: {fileDetail.generatedContentCounts.video}</li>
          <li>Podcasts: {fileDetail.generatedContentCounts.podcast}</li>
          <li>Flashcards: {fileDetail.generatedContentCounts.flashcard}</li>
          <li>Quizzes: {fileDetail.generatedContentCounts.quiz}</li>
        </ul>
      </div>

      {fileDetail.contributor && (
        <div>
          <h3>Contributor</h3>
          <p>
            {fileDetail.contributor.fullName} ({fileDetail.contributor.email})
          </p>
        </div>
      )}
    </div>
  );
};

export default FileDetailViewer;
```

---

## 2. Get File Chunks (Lấy danh sách chunks với text content)

Lấy danh sách tất cả chunks (phân đoạn text) của một file với text content đầy đủ. API này chỉ trả về các thông tin cần thiết: `id`, `chunkIndex`, và `content` để tối ưu performance.

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
    "content": "Đây là nội dung của chunk đầu tiên. Chunk này chứa phần đầu của tài liệu với khoảng 3000 ký tự..."
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "chunkIndex": 1,
    "content": "Đây là nội dung của chunk thứ hai. Chunk này tiếp tục từ chunk trước với một phần overlap khoảng 250 ký tự..."
  },
  {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "chunkIndex": 2,
    "content": "Đây là nội dung của chunk thứ ba. Mỗi chunk được tách ra từ tài liệu gốc với kích thước và overlap đã được cấu hình..."
  }
]
```

**Lưu ý:**

- Chunks được sắp xếp theo `chunkIndex` từ nhỏ đến lớn
- Nếu file chưa có chunks (chưa được xử lý), API sẽ trả về mảng rỗng `[]`
- Mỗi chunk chỉ chứa 3 thông tin: `id`, `chunkIndex`, và `content` để tối ưu performance

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
    throw new Error(error.message || "Failed to fetch file chunks");
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
const fullContent = chunks.map((chunk) => chunk.content).join("\n\n");
console.log("Full content:", fullContent);
```

### React Component Example

```typescript
import React, { useState, useEffect } from "react";

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
  const [selectedChunkIndex, setSelectedChunkIndex] = useState<number | null>(
    null
  );

  useEffect(() => {
    const fetchChunks = async () => {
      try {
        setLoading(true);
        const data = await getFileChunks(notebookId, fileId, token);
        setChunks(data);
        if (data.length > 0) {
          setSelectedChunkIndex(0);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load chunks");
      } finally {
        setLoading(false);
      }
    };

    fetchChunks();
  }, [notebookId, fileId, token]);

  if (loading) return <div>Loading chunks...</div>;
  if (error) return <div>Error: {error}</div>;
  if (chunks.length === 0)
    return <div>No chunks found. File may not have been processed yet.</div>;

  const selectedChunk =
    selectedChunkIndex !== null ? chunks[selectedChunkIndex] : null;
  const fullContent = chunks.map((chunk) => chunk.content).join("\n\n");

  return (
    <div style={{ display: "flex", gap: "20px" }}>
      {/* Sidebar: Danh sách chunks */}
      <div
        style={{
          width: "300px",
          borderRight: "1px solid #ccc",
          padding: "10px",
        }}
      >
        <h3>Chunks ({chunks.length})</h3>
        <div style={{ maxHeight: "600px", overflowY: "auto" }}>
          {chunks.map((chunk, index) => (
            <div
              key={chunk.id}
              onClick={() => setSelectedChunkIndex(index)}
              style={{
                padding: "10px",
                marginBottom: "5px",
                cursor: "pointer",
                backgroundColor:
                  selectedChunkIndex === index ? "#e3f2fd" : "#f5f5f5",
                border:
                  selectedChunkIndex === index
                    ? "2px solid #2196f3"
                    : "1px solid #ddd",
                borderRadius: "4px",
              }}
            >
              <strong>Chunk {chunk.chunkIndex}</strong>
              <p style={{ fontSize: "12px", color: "#666", marginTop: "5px" }}>
                {chunk.content.substring(0, 100)}...
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* Main content: Hiển thị chunk được chọn hoặc full content */}
      <div style={{ flex: 1, padding: "10px" }}>
        <div style={{ marginBottom: "20px" }}>
          <button
            onClick={() => setSelectedChunkIndex(null)}
            style={{
              padding: "8px 16px",
              marginRight: "10px",
              backgroundColor: selectedChunkIndex === null ? "#2196f3" : "#ccc",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
            }}
          >
            Show Full Content
          </button>
          <span>Total: {chunks.length} chunks</span>
        </div>

        {selectedChunkIndex === null ? (
          <div>
            <h3>Full Content</h3>
            <div
              style={{
                padding: "15px",
                backgroundColor: "#f9f9f9",
                borderRadius: "4px",
                maxHeight: "600px",
                overflowY: "auto",
                whiteSpace: "pre-wrap",
              }}
            >
              {fullContent}
            </div>
          </div>
        ) : selectedChunk ? (
          <div>
            <h3>Chunk {selectedChunk.chunkIndex}</h3>
            <div
              style={{
                padding: "15px",
                backgroundColor: "#f9f9f9",
                borderRadius: "4px",
                maxHeight: "600px",
                overflowY: "auto",
                whiteSpace: "pre-wrap",
              }}
            >
              {selectedChunk.content}
            </div>
            <div style={{ marginTop: "10px", fontSize: "12px", color: "#666" }}>
              Chunk ID: {selectedChunk.id}
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default FileChunkViewer;
```

### Use Cases

#### 1. Hiển thị danh sách chunks với preview

```typescript
function ChunkList({ chunks }: { chunks: FileChunkResponse[] }) {
  return (
    <div>
      <h3>Document Chunks ({chunks.length})</h3>
      {chunks.map((chunk) => (
        <div
          key={chunk.id}
          style={{
            marginBottom: "15px",
            padding: "10px",
            border: "1px solid #ddd",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              marginBottom: "5px",
            }}
          >
            <strong>Chunk {chunk.chunkIndex}</strong>
            <span style={{ fontSize: "12px", color: "#666" }}>
              {chunk.content.length} characters
            </span>
          </div>
          <p style={{ fontSize: "14px", color: "#333" }}>
            {chunk.content.substring(0, 200)}...
          </p>
        </div>
      ))}
    </div>
  );
}
```

#### 2. Tìm kiếm trong chunks

```typescript
function searchInChunks(
  chunks: FileChunkResponse[],
  searchTerm: string
): FileChunkResponse[] {
  const lowerSearchTerm = searchTerm.toLowerCase();
  return chunks.filter((chunk) =>
    chunk.content.toLowerCase().includes(lowerSearchTerm)
  );
}

// Sử dụng
const chunks = await getFileChunks(notebookId, fileId, token);
const searchResults = searchInChunks(chunks, "machine learning");
console.log(
  `Found ${searchResults.length} chunks containing "machine learning"`
);
```

#### 3. Lấy full content và xử lý

```typescript
async function getFullFileContent(
  notebookId: string,
  fileId: string,
  token: string
): Promise<string> {
  const chunks = await getFileChunks(notebookId, fileId, token);

  // Join tất cả chunks với separator
  return chunks
    .sort((a, b) => a.chunkIndex - b.chunkIndex)
    .map((chunk) => chunk.content)
    .join("\n\n");
}

// Sử dụng
const fullContent = await getFullFileContent(notebookId, fileId, token);
console.log("Full document content:", fullContent);
```

---

## So sánh 2 API

| Tính năng          | Get File Detail                           | Get File Chunks                      |
| ------------------ | ----------------------------------------- | ------------------------------------ |
| **Mục đích**       | Lấy thông tin tổng quan về file           | Lấy text content của từng chunk      |
| **Dữ liệu trả về** | File info, counts, contributor            | Danh sách chunks với content         |
| **Performance**    | Nhanh (không load content)                | Chậm hơn (load toàn bộ content)      |
| **Khi nào dùng**   | Hiển thị thông tin file, số lượng content | Hiển thị/đọc nội dung file, tìm kiếm |
| **Response size**  | Nhỏ (~1-2 KB)                             | Lớn (có thể vài MB nếu file lớn)     |

---

## Error Handling

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "User chưa đăng nhập.",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

### 400 Bad Request

```json
{
  "status": 400,
  "message": "Bạn chưa tham gia nhóm này",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

```json
{
  "status": 400,
  "message": "File không thuộc notebook này",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Notebook không tồn tại",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

```json
{
  "status": 404,
  "message": "File không tồn tại",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

### Ví dụ Error Handling

```typescript
async function getFileChunksWithErrorHandling(
  notebookId: string,
  fileId: string,
  token: string
): Promise<FileChunkResponse[]> {
  try {
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

      if (response.status === 401) {
        // Redirect to login
        window.location.href = "/login";
        throw new Error("Unauthorized");
      } else if (response.status === 404) {
        throw new Error("File or notebook not found");
      } else if (response.status === 400) {
        throw new Error(error.message || "Bad request");
      } else {
        throw new Error(`Server error: ${error.message}`);
      }
    }

    return response.json();
  } catch (error) {
    console.error("Error fetching chunks:", error);
    throw error;
  }
}
```

---

## Best Practices

1. **Sử dụng Get File Detail trước**: Kiểm tra `fileInfo.status === 'done'` trước khi gọi Get File Chunks để đảm bảo file đã được xử lý xong.

2. **Lazy loading**: Chỉ load chunks khi user thực sự cần xem nội dung (ví dụ: click vào button "View Content").

3. **Pagination cho chunks lớn**: Nếu file có nhiều chunks, có thể implement pagination hoặc virtual scrolling.

4. **Cache chunks**: Cache chunks trong memory hoặc localStorage để tránh gọi API nhiều lần.

5. **Error handling**: Luôn xử lý trường hợp file chưa có chunks (trả về empty array).

---

## Tóm tắt

- **Get File Detail**: Dùng để hiển thị thông tin tổng quan, số lượng content đã generate
- **Get File Chunks**: Dùng để hiển thị/đọc nội dung file, tìm kiếm trong text
- Cả 2 API đều yêu cầu user đã join notebook (status = 'approved')
- Get File Chunks chỉ trả về 3 field: `id`, `chunkIndex`, `content` để tối ưu performance
