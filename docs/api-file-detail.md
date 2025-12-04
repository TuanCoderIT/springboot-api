# API Hướng dẫn - Lấy chi tiết File

## Endpoint

```
GET /admin/notebooks/{notebookId}/files/{fileId}
```

## Headers

| Key           | Value            |
| ------------- | ---------------- |
| Authorization | Bearer {token}   |
| Content-Type  | application/json |

## Path Parameters

| Tên        | Kiểu | Mô tả               |
| ---------- | ---- | ------------------- |
| notebookId | UUID | ID của notebook     |
| fileId     | UUID | ID của file cần lấy |

## Response

### Success (200 OK)

```typescript
interface NotebookFileDetailResponse {
  fileInfo: {
    id: string;
    originalFilename: string;
    mimeType: string;
    fileSize: number;
    storageUrl: string;
    status: string;
    pagesCount: number | null;
    ocrDone: boolean;
    embeddingDone: boolean;
    chunkSize: number;
    chunkOverlap: number;
    chunksCount: number;
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
  };
  totalTextChunks: number;
  generatedContentCounts: {
    video: number;
    podcast: number;
    flashcard: number;
    quiz: number;
  };
}
```

### Ví dụ Response

```json
{
  "fileInfo": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "originalFilename": "document.pdf",
    "mimeType": "application/pdf",
    "fileSize": 1024000,
    "storageUrl": "http://localhost:8080/uploads/document.pdf",
    "status": "completed",
    "pagesCount": 10,
    "ocrDone": true,
    "embeddingDone": true,
    "chunkSize": 800,
    "chunkOverlap": 120,
    "chunksCount": 45,
    "uploadedBy": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "fullName": "Nguyễn Văn A",
      "email": "admin@example.com",
      "avatarUrl": null
    },
    "notebook": {
      "id": "987fcdeb-51a2-3e4f-8b9c-0d1e2f3a4b5c",
      "title": "Tài liệu học tập",
      "description": "Mô tả notebook",
      "type": "personal",
      "visibility": "private",
      "thumbnailUrl": null
    },
    "createdAt": "2024-12-04T10:30:00+07:00",
    "updatedAt": "2024-12-04T10:35:00+07:00"
  },
  "totalTextChunks": 45,
  "generatedContentCounts": {
    "video": 2,
    "podcast": 1,
    "flashcard": 15,
    "quiz": 5
  }
}
```

### Error Responses

| Status | Code         | Mô tả                               |
| ------ | ------------ | ----------------------------------- |
| 400    | BAD_REQUEST  | `notebookId` không được để trống    |
| 400    | BAD_REQUEST  | `fileId` không được để trống        |
| 400    | BAD_REQUEST  | File không thuộc notebook này       |
| 401    | UNAUTHORIZED | Admin chưa đăng nhập                |
| 404    | NOT_FOUND    | File không tồn tại với ID: {fileId} |
| 404    | NOT_FOUND    | Notebook của file không tồn tại     |

### Ví dụ Error Response

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "File không tồn tại với ID: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/admin/notebooks/{notebookId}/files/{fileId}"
}
```

## Ví dụ sử dụng (React/TypeScript)

```typescript
async function getFileDetail(notebookId: string, fileId: string) {
  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/${fileId}`,
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
```
