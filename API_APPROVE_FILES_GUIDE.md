# API Hướng dẫn: Duyệt Files (Approve Files)

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Duyệt từng file](#2-duyệt-từng-file)
3. [Duyệt tất cả files trong notebook](#3-duyệt-tất-cả-files-trong-notebook)
4. [Duyệt tất cả files pending](#4-duyệt-tất-cả-files-pending)
5. [Ví dụ code (JavaScript/TypeScript)](#5-ví-dụ-code-javascripttypescript)

---

## 1. Tổng quan

API này cho phép admin duyệt (approve) các files đang chờ duyệt (status = "pending"). Có 3 cách để duyệt:

1. **Duyệt từng file**: Duyệt một file cụ thể trong notebook
2. **Duyệt tất cả files trong notebook**: Duyệt tất cả files pending trong một notebook cụ thể
3. **Duyệt tất cả files pending**: Duyệt tất cả files pending trong hệ thống (có thể filter theo notebook)

**Lưu ý quan trọng**:
- Chỉ có thể duyệt files có status = "pending"
- Sau khi duyệt, file sẽ tự động chuyển sang status = "approved"
- Hệ thống sẽ tự động bắt đầu xử lý AI (OCR, Embedding) cho file sau khi được duyệt

---

## 2. Duyệt từng file

### Endpoint

```
PUT /admin/notebooks/{notebookId}/files/{fileId}/approve
```

### Mô tả

Duyệt một file cụ thể trong notebook. File phải có status = "pending" mới có thể duyệt.

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description     |
| ------------ | ---- | -------- | --------------- |
| `notebookId` | UUID | Yes      | ID của notebook  |
| `fileId`     | UUID | Yes      | ID của file cần duyệt |

### Response Format

### Success Response (200 OK)

```json
{
  "id": "file-uuid",
  "originalFilename": "document.pdf",
  "mimeType": "application/pdf",
  "fileSize": 1024000,
  "storageUrl": "/uploads/filename.pdf",
  "status": "approved",
  "pagesCount": 10,
  "ocrDone": false,
  "embeddingDone": false,
  "chunkSize": 800,
  "chunkOverlap": 120,
  "chunksCount": 0,
  "uploadedBy": {
    "id": "user-uuid",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "avatarUrl": "/uploads/avatar.jpg"
  },
  "createdAt": "2025-12-01T10:00:00+07:00",
  "updatedAt": "2025-12-01T11:00:00+07:00"
}
```

### Error Response (400 Bad Request)

**File không có status = "pending"**:

```json
{
  "status": 400,
  "message": "Chỉ có thể duyệt file có trạng thái pending",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

**File không thuộc notebook**:

```json
{
  "status": 400,
  "message": "File không thuộc notebook này",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

### Error Response (404 Not Found)

```json
{
  "status": 404,
  "message": "File không tồn tại",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

### Ví dụ sử dụng

```bash
curl -X 'PUT' \
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/file-uuid-here/approve' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

---

## 3. Duyệt tất cả files trong notebook

### Endpoint

```
PUT /admin/notebooks/{notebookId}/files/approve-all
```

### Mô tả

Duyệt tất cả files có status = "pending" trong một notebook cụ thể. Chỉ duyệt các files pending, bỏ qua các files có status khác.

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description     |
| ------------ | ---- | -------- | --------------- |
| `notebookId` | UUID | Yes      | ID của notebook  |

### Response Format

### Success Response (200 OK)

```json
{
  "approvedCount": 5,
  "message": "Đã duyệt 5 file(s)"
}
```

**Response Fields**:

| Field          | Type    | Description                    |
| -------------- | ------- | ------------------------------ |
| `approvedCount` | Integer | Số lượng files đã được duyệt   |
| `message`      | String  | Thông báo kết quả               |

### Ví dụ sử dụng

```bash
curl -X 'PUT' \
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/approve-all' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

**Response**:

```json
{
  "approvedCount": 3,
  "message": "Đã duyệt 3 file(s)"
}
```

---

## 4. Duyệt tất cả files pending

### Endpoint

```
PUT /admin/files/pending/approve-all
```

### Mô tả

Duyệt tất cả files có status = "pending" trong hệ thống. Có thể filter theo notebook bằng query parameter `notebookId`.

**Lưu ý**:
- Nếu **không truyền** `notebookId`: Duyệt **tất cả files pending** trong toàn bộ hệ thống
- Nếu **có truyền** `notebookId`: Chỉ duyệt files pending trong notebook đó

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Query Parameters

| Parameter    | Type | Required | Description                                                      |
| ------------ | ---- | -------- | ----------------------------------------------------------------- |
| `notebookId` | UUID | No       | Lọc theo notebook (nếu không truyền = duyệt tất cả files pending) |

### Response Format

### Success Response (200 OK)

```json
{
  "approvedCount": 10,
  "message": "Đã duyệt 10 file(s)"
}
```

**Response Fields**:

| Field          | Type    | Description                    |
| -------------- | ------- | ------------------------------ |
| `approvedCount` | Integer | Số lượng files đã được duyệt   |
| `message`      | String  | Thông báo kết quả               |

### Ví dụ sử dụng

#### Duyệt tất cả files pending trong hệ thống

```bash
curl -X 'PUT' \
  'http://localhost:8386/admin/files/pending/approve-all' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

**Response**:

```json
{
  "approvedCount": 15,
  "message": "Đã duyệt 15 file(s)"
}
```

#### Duyệt tất cả files pending trong một notebook cụ thể

```bash
curl -X 'PUT' \
  'http://localhost:8386/admin/files/pending/approve-all?notebookId=c3a7f558-faa7-4218-ae41-4ef57f976f34' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

**Response**:

```json
{
  "approvedCount": 5,
  "message": "Đã duyệt 5 file(s)"
}
```

---

## 5. Ví dụ code (JavaScript/TypeScript)

### 1. Duyệt từng file

#### Fetch API

```javascript
async function approveFile(notebookId, fileId, token) {
  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/${fileId}/approve`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Sử dụng
try {
  const approvedFile = await approveFile(
    "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    "file-uuid-here",
    token
  );
  console.log("File đã được duyệt:", approvedFile.originalFilename);
  console.log("Status:", approvedFile.status); // "approved"
} catch (error) {
  console.error("Lỗi khi duyệt file:", error.message);
}
```

#### Axios

```javascript
import axios from "axios";

async function approveFile(notebookId, fileId, token) {
  try {
    const response = await axios.put(
      `/admin/notebooks/${notebookId}/files/${fileId}/approve`,
      {},
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data;
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || "Lỗi khi duyệt file");
    }
    throw error;
  }
}

// Sử dụng
const approvedFile = await approveFile(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  "file-uuid-here",
  token
);
```

### 2. Duyệt tất cả files trong notebook

#### Fetch API

```javascript
async function approveAllFilesInNotebook(notebookId, token) {
  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/approve-all`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Sử dụng
try {
  const result = await approveAllFilesInNotebook(
    "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    token
  );
  console.log(`Đã duyệt ${result.approvedCount} file(s)`);
  console.log(result.message);
} catch (error) {
  console.error("Lỗi khi duyệt files:", error.message);
}
```

#### Axios

```javascript
import axios from "axios";

async function approveAllFilesInNotebook(notebookId, token) {
  try {
    const response = await axios.put(
      `/admin/notebooks/${notebookId}/files/approve-all`,
      {},
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data;
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || "Lỗi khi duyệt files");
    }
    throw error;
  }
}

// Sử dụng
const result = await approveAllFilesInNotebook(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  token
);
console.log(`Đã duyệt ${result.approvedCount} file(s)`);
```

### 3. Duyệt tất cả files pending

#### Fetch API

```javascript
async function approveAllPendingFiles(notebookId = null, token) {
  const url = notebookId
    ? `/admin/files/pending/approve-all?notebookId=${notebookId}`
    : `/admin/files/pending/approve-all`;

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Sử dụng: Duyệt tất cả files pending trong hệ thống
const allResult = await approveAllPendingFiles(null, token);
console.log(`Đã duyệt ${allResult.approvedCount} file(s) trong toàn bộ hệ thống`);

// Sử dụng: Duyệt tất cả files pending trong một notebook
const notebookResult = await approveAllPendingFiles(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  token
);
console.log(`Đã duyệt ${notebookResult.approvedCount} file(s) trong notebook`);
```

#### Axios

```javascript
import axios from "axios";

async function approveAllPendingFiles(notebookId = null, token) {
  try {
    const response = await axios.put(
      "/admin/files/pending/approve-all",
      {},
      {
        params: notebookId ? { notebookId } : {},
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data;
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || "Lỗi khi duyệt files");
    }
    throw error;
  }
}

// Sử dụng
const result = await approveAllPendingFiles(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  token
);
console.log(`Đã duyệt ${result.approvedCount} file(s)`);
```

### React Hook Example

```typescript
import { useState } from "react";

interface ApproveResult {
  approvedCount: number;
  message: string;
}

function useApproveFiles() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const approveFile = async (
    notebookId: string,
    fileId: string,
    token: string
  ): Promise<NotebookFileResponse> => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(
        `/admin/notebooks/${notebookId}/files/${fileId}/approve`,
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "application/json",
          },
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Lỗi khi duyệt file");
      }

      return await response.json();
    } catch (err) {
      const error = err instanceof Error ? err : new Error("Unknown error");
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const approveAllInNotebook = async (
    notebookId: string,
    token: string
  ): Promise<ApproveResult> => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(
        `/admin/notebooks/${notebookId}/files/approve-all`,
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "application/json",
          },
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Lỗi khi duyệt files");
      }

      return await response.json();
    } catch (err) {
      const error = err instanceof Error ? err : new Error("Unknown error");
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const approveAllPending = async (
    token: string,
    notebookId?: string
  ): Promise<ApproveResult> => {
    try {
      setLoading(true);
      setError(null);
      const url = notebookId
        ? `/admin/files/pending/approve-all?notebookId=${notebookId}`
        : `/admin/files/pending/approve-all`;

      const response = await fetch(url, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/json",
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Lỗi khi duyệt files");
      }

      return await response.json();
    } catch (err) {
      const error = err instanceof Error ? err : new Error("Unknown error");
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  return {
    approveFile,
    approveAllInNotebook,
    approveAllPending,
    loading,
    error,
  };
}

// Sử dụng trong component
function ApproveFilesComponent() {
  const { approveFile, approveAllInNotebook, approveAllPending, loading, error } =
    useApproveFiles();
  const token = "your-token-here";

  const handleApproveFile = async () => {
    try {
      const result = await approveFile(
        "notebook-id",
        "file-id",
        token
      );
      console.log("File đã được duyệt:", result);
    } catch (err) {
      console.error("Lỗi:", err);
    }
  };

  const handleApproveAllInNotebook = async () => {
    try {
      const result = await approveAllInNotebook("notebook-id", token);
      alert(`Đã duyệt ${result.approvedCount} file(s)`);
    } catch (err) {
      console.error("Lỗi:", err);
    }
  };

  const handleApproveAllPending = async () => {
    try {
      const result = await approveAllPending(token);
      alert(`Đã duyệt ${result.approvedCount} file(s)`);
    } catch (err) {
      console.error("Lỗi:", err);
    }
  };

  return (
    <div>
      <button onClick={handleApproveFile} disabled={loading}>
        Duyệt file
      </button>
      <button onClick={handleApproveAllInNotebook} disabled={loading}>
        Duyệt tất cả trong notebook
      </button>
      <button onClick={handleApproveAllPending} disabled={loading}>
        Duyệt tất cả pending
      </button>
      {loading && <p>Đang xử lý...</p>}
      {error && <p style={{ color: "red" }}>Lỗi: {error.message}</p>}
    </div>
  );
}
```

### TypeScript Types

```typescript
interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  storageUrl: string;
  status: "pending" | "approved" | "rejected" | "processing" | "failed" | "done";
  pagesCount: number | null;
  ocrDone: boolean;
  embeddingDone: boolean;
  chunkSize: number;
  chunkOverlap: number;
  chunksCount: number;
  uploadedBy: UploaderInfo;
  createdAt: string;
  updatedAt: string;
}

interface UploaderInfo {
  id: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
}

interface ApproveAllResponse {
  approvedCount: number;
  message: string;
}
```

---

## Ghi chú quan trọng

1. **Chỉ duyệt files pending**: Tất cả các API duyệt chỉ hoạt động với files có status = "pending". Nếu file có status khác, sẽ trả về lỗi 400.

2. **Tự động xử lý AI**: Sau khi file được duyệt, hệ thống sẽ tự động:
   - Chuyển status từ "pending" → "approved"
   - Bắt đầu quá trình OCR (nếu chưa hoàn thành)
   - Bắt đầu quá trình Embedding (nếu chưa hoàn thành)
   - Tạo file chunks từ nội dung đã OCR

3. **Transaction**: Tất cả các thao tác duyệt đều được thực hiện trong transaction để đảm bảo tính nhất quán dữ liệu.

4. **Performance**: 
   - Duyệt từng file: Nhanh, phù hợp khi cần kiểm soát từng file
   - Duyệt tất cả: Có thể mất thời gian nếu có nhiều files, nên hiển thị loading state cho user

5. **Error Handling**: Luôn xử lý lỗi khi gọi API, đặc biệt là:
   - File không tồn tại (404)
   - File không có status pending (400)
   - File không thuộc notebook (400)
   - Lỗi authentication (401)

