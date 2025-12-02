# API Hướng dẫn: Duyệt và Từ chối File (Approve & Reject File)

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Duyệt file](#2-duyệt-file)
3. [Từ chối file](#3-từ-chối-file)
4. [Ví dụ code (JavaScript/TypeScript)](#4-ví-dụ-code-javascripttypescript)

---

## 1. Tổng quan

API này cho phép admin duyệt (approve) hoặc từ chối (reject) các files đang chờ duyệt (status = "pending") trong một notebook cụ thể.

**Lưu ý quan trọng**:

- Chỉ có thể duyệt/từ chối files có status = "pending"
- Sau khi duyệt, file sẽ tự động chuyển sang status = "approved" và hệ thống sẽ bắt đầu xử lý AI (OCR, Embedding)
- Sau khi từ chối, file sẽ chuyển sang status = "rejected" và không được xử lý AI

---

## 2. Duyệt file

### Endpoint

```
PUT /admin/notebooks/{notebookId}/files/{fileId}/approve
```

### Mô tả

Duyệt một file cụ thể trong notebook. File phải có status = "pending" mới có thể duyệt. Sau khi duyệt, file sẽ được xử lý AI tự động.

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description           |
| ------------ | ---- | -------- | --------------------- |
| `notebookId` | UUID | Yes      | ID của notebook       |
| `fileId`     | UUID | Yes      | ID của file cần duyệt |

### Request Body

Không cần request body.

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

**Response Fields**:

| Field              | Type           | Description                                      |
| ------------------ | -------------- | ------------------------------------------------ |
| `id`               | UUID           | ID của file                                      |
| `originalFilename` | String         | Tên file gốc                                     |
| `mimeType`         | String         | Loại MIME của file                               |
| `fileSize`         | Long           | Kích thước file (bytes)                          |
| `storageUrl`       | String         | Đường dẫn lưu trữ file                           |
| `status`           | String         | Trạng thái file (sẽ là "approved" sau khi duyệt) |
| `pagesCount`       | Integer        | Số trang (cho PDF)                               |
| `ocrDone`          | Boolean        | Đã hoàn thành OCR chưa                           |
| `embeddingDone`    | Boolean        | Đã tạo embedding chưa                            |
| `chunkSize`        | Integer        | Kích thước chunk                                 |
| `chunkOverlap`     | Integer        | Độ overlap giữa các chunk                        |
| `chunksCount`      | Long           | Tổng số chunks đã được tạo                       |
| `uploadedBy`       | UploaderInfo   | Thông tin người đóng góp file                    |
| `createdAt`        | OffsetDateTime | Thời gian upload (ISO 8601)                      |
| `updatedAt`        | OffsetDateTime | Thời gian cập nhật (ISO 8601)                    |

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

## 3. Từ chối file

### Endpoint

```
PUT /admin/notebooks/{notebookId}/files/{fileId}/reject
```

### Mô tả

Từ chối một file cụ thể trong notebook. File phải có status = "pending" mới có thể từ chối. Sau khi từ chối, file sẽ không được xử lý AI.

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description             |
| ------------ | ---- | -------- | ----------------------- |
| `notebookId` | UUID | Yes      | ID của notebook         |
| `fileId`     | UUID | Yes      | ID của file cần từ chối |

### Request Body

Không cần request body.

### Response Format

### Success Response (200 OK)

```json
{
  "id": "file-uuid",
  "originalFilename": "document.pdf",
  "mimeType": "application/pdf",
  "fileSize": 1024000,
  "storageUrl": "/uploads/filename.pdf",
  "status": "rejected",
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

**Lưu ý**: Sau khi từ chối, `status` sẽ là `"rejected"` và file sẽ không được xử lý AI.

### Error Response (400 Bad Request)

**File không có status = "pending"**:

```json
{
  "status": 400,
  "message": "Chỉ có thể từ chối file có trạng thái pending",
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
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/file-uuid-here/reject' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

---

## 4. Ví dụ code (JavaScript/TypeScript)

### 1. Duyệt file

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
  console.log("AI processing sẽ bắt đầu tự động");
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

### 2. Từ chối file

#### Fetch API

```javascript
async function rejectFile(notebookId, fileId, token) {
  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/${fileId}/reject`,
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
  const rejectedFile = await rejectFile(
    "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    "file-uuid-here",
    token
  );
  console.log("File đã bị từ chối:", rejectedFile.originalFilename);
  console.log("Status:", rejectedFile.status); // "rejected"
} catch (error) {
  console.error("Lỗi khi từ chối file:", error.message);
}
```

#### Axios

```javascript
import axios from "axios";

async function rejectFile(notebookId, fileId, token) {
  try {
    const response = await axios.put(
      `/admin/notebooks/${notebookId}/files/${fileId}/reject`,
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
      throw new Error(error.response.data.message || "Lỗi khi từ chối file");
    }
    throw error;
  }
}

// Sử dụng
const rejectedFile = await rejectFile(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  "file-uuid-here",
  token
);
```

### React Hook Example

```typescript
import { useState } from "react";

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
    | "failed"
    | "done";
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

function useApproveRejectFile() {
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

  const rejectFile = async (
    notebookId: string,
    fileId: string,
    token: string
  ): Promise<NotebookFileResponse> => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(
        `/admin/notebooks/${notebookId}/files/${fileId}/reject`,
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
        throw new Error(errorData.message || "Lỗi khi từ chối file");
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
    rejectFile,
    loading,
    error,
  };
}

// Sử dụng trong component
function FileActionButtons({
  notebookId,
  fileId,
  currentStatus,
}: {
  notebookId: string;
  fileId: string;
  currentStatus: string;
}) {
  const { approveFile, rejectFile, loading, error } = useApproveRejectFile();
  const token = "your-token-here";

  const handleApprove = async () => {
    try {
      const result = await approveFile(notebookId, fileId, token);
      alert(`File "${result.originalFilename}" đã được duyệt thành công!`);
      // Refresh danh sách files hoặc update UI
    } catch (err) {
      alert(`Lỗi: ${err instanceof Error ? err.message : "Unknown error"}`);
    }
  };

  const handleReject = async () => {
    if (!confirm("Bạn có chắc chắn muốn từ chối file này?")) {
      return;
    }

    try {
      const result = await rejectFile(notebookId, fileId, token);
      alert(`File "${result.originalFilename}" đã bị từ chối.`);
      // Refresh danh sách files hoặc update UI
    } catch (err) {
      alert(`Lỗi: ${err instanceof Error ? err.message : "Unknown error"}`);
    }
  };

  if (currentStatus !== "pending") {
    return null; // Chỉ hiển thị buttons khi file đang pending
  }

  return (
    <div>
      <button onClick={handleApprove} disabled={loading}>
        {loading ? "Đang xử lý..." : "Duyệt"}
      </button>
      <button
        onClick={handleReject}
        disabled={loading}
        style={{ marginLeft: "10px" }}
      >
        {loading ? "Đang xử lý..." : "Từ chối"}
      </button>
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
  status:
    | "pending"
    | "approved"
    | "rejected"
    | "processing"
    | "failed"
    | "done";
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
```

---

## Ghi chú quan trọng

1. **Chỉ duyệt/từ chối files pending**: Cả 2 API chỉ hoạt động với files có status = "pending". Nếu file có status khác, sẽ trả về lỗi 400.

2. **Sau khi duyệt**:

   - File status chuyển từ "pending" → "approved"
   - Hệ thống tự động bắt đầu xử lý AI (OCR, Embedding)
   - File sẽ được tạo chunks và có thể sử dụng trong RAG queries

3. **Sau khi từ chối**:

   - File status chuyển từ "pending" → "rejected"
   - File sẽ **không** được xử lý AI
   - File sẽ không xuất hiện trong danh sách files đã duyệt

4. **Validation**:

   - File phải thuộc notebook được chỉ định
   - File phải có status = "pending"
   - Admin phải đã đăng nhập

5. **Error Handling**: Luôn xử lý lỗi khi gọi API, đặc biệt là:

   - File không tồn tại (404)
   - File không có status pending (400)
   - File không thuộc notebook (400)
   - Lỗi authentication (401)

6. **UI Best Practices**:
   - Chỉ hiển thị nút "Duyệt" và "Từ chối" khi file có status = "pending"
   - Hiển thị loading state khi đang xử lý
   - Xác nhận trước khi từ chối file (confirmation dialog)
   - Refresh danh sách files sau khi duyệt/từ chối thành công
   - Hiển thị thông báo thành công/thất bại cho user
