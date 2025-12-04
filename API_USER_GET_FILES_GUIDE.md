# API Hướng Dẫn: Lấy Danh Sách File Theo Notebook ID

## Endpoint

```
GET /user/notebooks/{notebookId}/files
```

## Mô Tả

Lấy danh sách file theo notebook ID với các điều kiện:
- **File có status = 'done'**: Tất cả file đã được duyệt và xử lý xong (OCR, embedding)
- **File của user hiện tại**: File do user đang đăng nhập upload, bất kể status (pending, failed, rejected, processing)
- **Tìm kiếm theo tên file**: Tùy chọn, tìm kiếm theo tên file gốc

## Authentication

**Yêu cầu**: Phải đăng nhập (JWT token trong cookie hoặc header)

## Request Parameters

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `notebookId` | UUID | ✅ Yes | ID của notebook/group |

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `search` | String | ❌ No | Từ khóa tìm kiếm theo tên file (case-insensitive) |

## Response

### Success Response (200 OK)

**Response Body**: Array of `NotebookFileResponse`

```json
[
  {
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
    "chunkOverlap": 200,
    "chunksCount": null,
    "uploadedBy": {
      "id": "991c40a1-c2b1-4e62-972a-33deafd708ff",
      "fullName": "John Doe",
      "email": "john@example.com",
      "avatarUrl": "http://localhost:8386/uploads/2ba5bc04-8692-440d-8f9b-19e07bf37b82.jpg"
    },
    "notebook": {
      "id": "c3a7f558-faa7-4218-ae41-4ef57f976f34",
      "title": "My Notebook",
      "description": "Description here",
      "type": "community",
      "visibility": "public",
      "thumbnailUrl": "http://localhost:8386/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg"
    },
    "createdAt": "2025-12-03T14:48:59.628486Z",
    "updatedAt": "2025-12-03T15:07:45.887456Z"
  }
]
```

### Response Fields

#### NotebookFileResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID của file |
| `originalFilename` | String | Tên file gốc khi upload |
| `mimeType` | String | MIME type của file (application/pdf, application/vnd.openxmlformats-officedocument.wordprocessingml.document, ...) |
| `fileSize` | Long | Kích thước file (bytes) |
| `storageUrl` | String | **Full URL** để truy cập file (đã normalize: `baseUrl + path`) |
| `status` | String | Trạng thái file: `pending`, `approved`, `rejected`, `processing`, `done`, `failed` |
| `pagesCount` | Integer | Số trang (nếu là PDF) |
| `ocrDone` | Boolean | Đã hoàn thành OCR chưa |
| `embeddingDone` | Boolean | Đã hoàn thành embedding chưa |
| `chunkSize` | Integer | Kích thước chunk (3000-5000) |
| `chunkOverlap` | Integer | Độ overlap giữa các chunk (200-500) |
| `chunksCount` | Long | Số lượng chunks (luôn null trong response này) |
| `uploadedBy` | UploaderInfo | Thông tin người upload |
| `notebook` | NotebookInfo | Thông tin notebook |
| `createdAt` | OffsetDateTime | Thời gian tạo (ISO 8601) |
| `updatedAt` | OffsetDateTime | Thời gian cập nhật (ISO 8601) |

#### UploaderInfo

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID của user |
| `fullName` | String | Tên đầy đủ |
| `email` | String | Email |
| `avatarUrl` | String | **Full URL** avatar (đã normalize: `baseUrl + path`) |

#### NotebookInfo

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID của notebook |
| `title` | String | Tiêu đề notebook |
| `description` | String | Mô tả |
| `type` | String | Loại notebook (community, personal, ...) |
| `visibility` | String | Quyền truy cập (public, private, blocked) |
| `thumbnailUrl` | String | **Full URL** thumbnail (đã normalize: `baseUrl + path`) |

### Error Responses

#### 401 Unauthorized
```json
{
  "error": "User chưa đăng nhập."
}
```

#### 500 Internal Server Error
Có lỗi xảy ra khi xử lý request.

## Examples

### 1. Lấy tất cả file (không tìm kiếm)

**Request:**
```http
GET /user/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "6523c2d1-c4dc-4173-8b56-3353057096a8",
    "originalFilename": "document.pdf",
    "status": "done",
    ...
  },
  {
    "id": "f4a552b4-17a4-40b4-a602-3d1d6a2b3c2b",
    "originalFilename": "my-file.docx",
    "status": "pending",
    ...
  }
]
```

### 2. Tìm kiếm file theo tên

**Request:**
```http
GET /user/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?search=document
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "6523c2d1-c4dc-4173-8b56-3353057096a8",
    "originalFilename": "document.pdf",
    "status": "done",
    ...
  }
]
```

### 3. Tìm kiếm không có kết quả

**Request:**
```http
GET /user/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?search=xyz123
Authorization: Bearer <token>
```

**Response:**
```json
[]
```

## Frontend Implementation Examples

### JavaScript/TypeScript (Fetch API)

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
    avatarUrl: string;
  } | null;
  notebook: {
    id: string;
    title: string;
    description: string;
    type: string;
    visibility: string;
    thumbnailUrl: string;
  } | null;
  createdAt: string;
  updatedAt: string;
}

async function getNotebookFiles(
  notebookId: string,
  search?: string
): Promise<NotebookFileResponse[]> {
  const baseUrl = 'http://localhost:8386';
  const url = new URL(`${baseUrl}/user/notebooks/${notebookId}/files`);
  
  if (search) {
    url.searchParams.append('search', search);
  }

  const response = await fetch(url.toString(), {
    method: 'GET',
    credentials: 'include', // Include cookies for JWT
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Unauthorized: Please login');
    }
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Usage
try {
  const files = await getNotebookFiles('c3a7f558-faa7-4218-ae41-4ef57f976f34', 'document');
  console.log('Files:', files);
  
  // Hiển thị file
  files.forEach(file => {
    console.log(`File: ${file.originalFilename}`);
    console.log(`Status: ${file.status}`);
    console.log(`Storage URL: ${file.storageUrl}`);
    console.log(`Uploaded by: ${file.uploadedBy?.fullName}`);
  });
} catch (error) {
  console.error('Error fetching files:', error);
}
```

### React Hook Example

```typescript
import { useState, useEffect } from 'react';

function useNotebookFiles(notebookId: string, search?: string) {
  const [files, setFiles] = useState<NotebookFileResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchFiles() {
      setLoading(true);
      setError(null);
      
      try {
        const baseUrl = 'http://localhost:8386';
        const url = new URL(`${baseUrl}/user/notebooks/${notebookId}/files`);
        
        if (search) {
          url.searchParams.append('search', search);
        }

        const response = await fetch(url.toString(), {
          method: 'GET',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (!response.ok) {
          if (response.status === 401) {
            throw new Error('Unauthorized: Please login');
          }
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        setFiles(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    }

    if (notebookId) {
      fetchFiles();
    }
  }, [notebookId, search]);

  return { files, loading, error };
}

// Component usage
function NotebookFilesList({ notebookId }: { notebookId: string }) {
  const [searchTerm, setSearchTerm] = useState('');
  const { files, loading, error } = useNotebookFiles(notebookId, searchTerm);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <input
        type="text"
        placeholder="Search files..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
      />
      
      <ul>
        {files.map((file) => (
          <li key={file.id}>
            <h3>{file.originalFilename}</h3>
            <p>Status: {file.status}</p>
            <p>Size: {(file.fileSize / 1024).toFixed(2)} KB</p>
            <a href={file.storageUrl} target="_blank" rel="noopener noreferrer">
              Download
            </a>
            {file.uploadedBy && (
              <div>
                <img src={file.uploadedBy.avatarUrl} alt={file.uploadedBy.fullName} />
                <span>{file.uploadedBy.fullName}</span>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### Axios Example

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8386',
  withCredentials: true, // Include cookies
});

interface GetFilesParams {
  notebookId: string;
  search?: string;
}

async function getNotebookFiles({ notebookId, search }: GetFilesParams) {
  try {
    const response = await api.get<NotebookFileResponse[]>(
      `/user/notebooks/${notebookId}/files`,
      {
        params: search ? { search } : {},
      }
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        throw new Error('Unauthorized: Please login');
      }
      throw new Error(`HTTP error! status: ${error.response?.status}`);
    }
    throw error;
  }
}

// Usage
getNotebookFiles({
  notebookId: 'c3a7f558-faa7-4218-ae41-4ef57f976f34',
  search: 'document',
})
  .then((files) => {
    console.log('Files:', files);
  })
  .catch((error) => {
    console.error('Error:', error);
  });
```

## Lưu Ý Quan Trọng

### 1. URL Normalization

Tất cả các URL trong response đã được normalize về **full URL**:
- `storageUrl`: `http://localhost:8386/uploads/{filename}`
- `avatarUrl`: `http://localhost:8386/uploads/{filename}`
- `thumbnailUrl`: `http://localhost:8386/uploads/{filename}`

**Frontend có thể sử dụng trực tiếp các URL này** mà không cần xử lý thêm.

### 2. File Status

- **`done`**: File đã được duyệt và xử lý xong (OCR, embedding hoàn tất)
- **`pending`**: File đang chờ duyệt
- **`processing`**: File đang được xử lý (OCR, embedding)
- **`failed`**: File xử lý thất bại
- **`rejected`**: File bị từ chối
- **`approved`**: File đã được duyệt (chưa xử lý xong)

### 3. Filtering Logic

- User sẽ thấy **TẤT CẢ** file có `status = 'done'` (bất kể ai upload)
- User sẽ thấy **TẤT CẢ** file do chính mình upload (bất kể status)

### 4. Search

- Tìm kiếm theo `originalFilename` (tên file gốc)
- Case-insensitive (không phân biệt hoa thường)
- Partial match (tìm trong tên file)

### 5. Authentication

- Phải gửi JWT token trong cookie hoặc Authorization header
- Nếu không có token → 401 Unauthorized

## Testing với cURL

```bash
# Lấy tất cả file
curl -X 'GET' \
  'http://localhost:8386/user/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files' \
  -H 'accept: */*' \
  -H 'Cookie: AUTH_COOKIE=<your-jwt-token>'

# Tìm kiếm file
curl -X 'GET' \
  'http://localhost:8386/user/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?search=document' \
  -H 'accept: */*' \
  -H 'Cookie: AUTH_COOKIE=<your-jwt-token>'
```

## Response Order

Files được sắp xếp theo `createdAt DESC` (mới nhất trước).

