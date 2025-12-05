# API Hướng dẫn - Xóa File

Tài liệu này hướng dẫn cách sử dụng API để xóa file trong notebook. User chỉ có thể xóa file của chính mình.

## Base URL

```
/user/notebooks/{notebookId}/files/{fileId}
```

## Authentication

API sử dụng **Cookie-based authentication**. Token được lưu trong cookie `AUTH-TOKEN` sau khi user đăng nhập.

**Lưu ý quan trọng:**

- Frontend không cần gửi token trong header
- Browser sẽ tự động gửi cookie `AUTH-TOKEN` trong mọi request
- Đảm bảo `credentials: 'include'` khi gọi API từ frontend

---

## Delete File (Xóa file)

Xóa một file khỏi notebook. **Chỉ cho phép xóa file của chính user đó**.

### Endpoint

```
DELETE /user/notebooks/{notebookId}/files/{fileId}
```

### Path Parameters

| Tên        | Kiểu | Mô tả               |
| ---------- | ---- | ------------------- |
| notebookId | UUID | ID của notebook     |
| fileId     | UUID | ID của file cần xóa |

### Request Headers

Không cần gửi header `Authorization`. Cookie sẽ được gửi tự động.

### Response (204 No Content)

API trả về status code `204 No Content` khi xóa thành công. Không có response body.

### Quyền truy cập

API chỉ cho phép xóa file nếu:

1. ✅ User đã đăng nhập (có cookie `AUTH-TOKEN` hợp lệ)
2. ✅ User đã join notebook (status = 'approved')
3. ✅ File thuộc notebook đó
4. ✅ **File phải được upload bởi chính user đó** (quan trọng nhất)

### Ví dụ (TypeScript/React)

```typescript
async function deleteFile(notebookId: string, fileId: string): Promise<void> {
  const response = await fetch(
    `/user/notebooks/${notebookId}/files/${fileId}`,
    {
      method: "DELETE",
      credentials: "include", // ⭐ QUAN TRỌNG: Gửi cookie tự động
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to delete file");
  }

  // 204 No Content - không có response body
  return;
}

// Sử dụng
try {
  await deleteFile(notebookId, fileId);
  console.log("File deleted successfully");
} catch (error) {
  console.error("Error deleting file:", error);
}
```

### React Component Example

```typescript
import React, { useState } from "react";

interface DeleteFileButtonProps {
  notebookId: string;
  fileId: string;
  fileName: string;
  onDeleted?: () => void;
}

const DeleteFileButton: React.FC<DeleteFileButtonProps> = ({
  notebookId,
  fileId,
  fileName,
  onDeleted,
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);

  const handleDelete = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(
        `/user/notebooks/${notebookId}/files/${fileId}`,
        {
          method: "DELETE",
          credentials: "include", // ⭐ Gửi cookie tự động
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to delete file");
      }

      // Xóa thành công
      if (onDeleted) {
        onDeleted();
      }
      setShowConfirm(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete file");
    } finally {
      setLoading(false);
    }
  };

  if (showConfirm) {
    return (
      <div
        style={{
          padding: "10px",
          border: "1px solid #ddd",
          borderRadius: "4px",
        }}
      >
        <p>Bạn có chắc chắn muốn xóa file "{fileName}"?</p>
        <div style={{ marginTop: "10px", display: "flex", gap: "10px" }}>
          <button
            onClick={handleDelete}
            disabled={loading}
            style={{
              padding: "8px 16px",
              backgroundColor: "#dc3545",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: loading ? "not-allowed" : "pointer",
            }}
          >
            {loading ? "Đang xóa..." : "Xóa"}
          </button>
          <button
            onClick={() => {
              setShowConfirm(false);
              setError(null);
            }}
            disabled={loading}
            style={{
              padding: "8px 16px",
              backgroundColor: "#6c757d",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: loading ? "not-allowed" : "pointer",
            }}
          >
            Hủy
          </button>
        </div>
        {error && (
          <div
            style={{ marginTop: "10px", color: "#dc3545", fontSize: "14px" }}
          >
            {error}
          </div>
        )}
      </div>
    );
  }

  return (
    <button
      onClick={() => setShowConfirm(true)}
      style={{
        padding: "8px 16px",
        backgroundColor: "#dc3545",
        color: "white",
        border: "none",
        borderRadius: "4px",
        cursor: "pointer",
      }}
    >
      Xóa File
    </button>
  );
};

export default DeleteFileButton;
```

### Axios Example

```typescript
import axios from "axios";

// Cấu hình axios để tự động gửi cookie
axios.defaults.withCredentials = true;

async function deleteFile(notebookId: string, fileId: string): Promise<void> {
  try {
    await axios.delete(`/user/notebooks/${notebookId}/files/${fileId}`);
    // 204 No Content - không có response data
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const message = error.response?.data?.message || "Failed to delete file";
      throw new Error(message);
    }
    throw error;
  }
}

// Sử dụng
await deleteFile(notebookId, fileId);
```

### Fetch với Error Handling đầy đủ

```typescript
async function deleteFileWithErrorHandling(
  notebookId: string,
  fileId: string
): Promise<void> {
  try {
    const response = await fetch(
      `/user/notebooks/${notebookId}/files/${fileId}`,
      {
        method: "DELETE",
        credentials: "include", // ⭐ QUAN TRỌNG
      }
    );

    if (!response.ok) {
      const error = await response.json();

      if (response.status === 401) {
        // Unauthorized - cookie không hợp lệ hoặc hết hạn
        window.location.href = "/login";
        throw new Error("Phiên đăng nhập đã hết hạn");
      } else if (response.status === 400) {
        // Bad Request - không có quyền xóa
        throw new Error(error.message || "Không có quyền xóa file này");
      } else if (response.status === 404) {
        // Not Found
        throw new Error("File hoặc notebook không tồn tại");
      } else {
        throw new Error(`Server error: ${error.message}`);
      }
    }

    // 204 No Content - xóa thành công
    return;
  } catch (error) {
    console.error("Error deleting file:", error);
    throw error;
  }
}
```

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

**Nguyên nhân:**

- Cookie `AUTH-TOKEN` không tồn tại
- Cookie đã hết hạn
- Token không hợp lệ

**Xử lý:**

- Redirect user đến trang login
- Xóa cookie cũ nếu có

### 400 Bad Request

#### Case 1: Không có quyền xóa file

```json
{
  "status": 400,
  "message": "Bạn chỉ có thể xóa file của chính mình",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

**Nguyên nhân:**

- File không được upload bởi user hiện tại
- User đang cố xóa file của người khác

**Xử lý:**

- Hiển thị thông báo lỗi cho user
- Ẩn nút xóa nếu file không phải của user

#### Case 2: Chưa tham gia notebook

```json
{
  "status": 400,
  "message": "Bạn chưa tham gia nhóm này",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

**Nguyên nhân:**

- User chưa join notebook
- User đã join nhưng chưa được approve

#### Case 3: File không thuộc notebook

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

---

## Best Practices

### 1. Kiểm tra quyền trước khi hiển thị nút xóa

```typescript
interface FileItem {
  id: string;
  originalFilename: string;
  uploadedBy: {
    id: string;
    fullName: string;
    email: string;
  } | null;
  // ... other fields
}

interface FileListProps {
  files: FileItem[];
  currentUserId: string;
}

const FileList: React.FC<FileListProps> = ({ files, currentUserId }) => {
  return (
    <div>
      {files.map((file) => {
        const canDelete = file.uploadedBy?.id === currentUserId;

        return (
          <div
            key={file.id}
            style={{ display: "flex", alignItems: "center", gap: "10px" }}
          >
            <span>{file.originalFilename}</span>
            {canDelete && (
              <DeleteFileButton
                notebookId={notebookId}
                fileId={file.id}
                fileName={file.originalFilename}
                onDeleted={() => {
                  // Refresh file list
                  window.location.reload();
                }}
              />
            )}
          </div>
        );
      })}
    </div>
  );
};
```

### 2. Xác nhận trước khi xóa

Luôn hiển thị dialog xác nhận trước khi xóa file để tránh xóa nhầm:

```typescript
const handleDeleteClick = () => {
  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn xóa file "${fileName}"? Hành động này không thể hoàn tác.`
  );

  if (confirmed) {
    deleteFile(notebookId, fileId);
  }
};
```

### 3. Optimistic UI Update

Cập nhật UI ngay lập tức, rollback nếu có lỗi:

```typescript
const [files, setFiles] = useState<FileItem[]>([]);

const handleDelete = async (fileId: string) => {
  // Lưu state trước khi xóa
  const previousFiles = [...files];

  // Optimistic update - xóa ngay trong UI
  setFiles(files.filter((f) => f.id !== fileId));

  try {
    await deleteFile(notebookId, fileId);
    // Xóa thành công - không cần làm gì thêm
  } catch (error) {
    // Rollback nếu có lỗi
    setFiles(previousFiles);
    alert("Không thể xóa file. Vui lòng thử lại.");
  }
};
```

### 4. Loading State

Hiển thị loading state khi đang xóa:

```typescript
const [deletingFileId, setDeletingFileId] = useState<string | null>(null);

const handleDelete = async (fileId: string) => {
  setDeletingFileId(fileId);
  try {
    await deleteFile(notebookId, fileId);
    // Xóa thành công
  } catch (error) {
    // Xử lý lỗi
  } finally {
    setDeletingFileId(null);
  }
};

// Trong render
{
  deletingFileId === file.id ? (
    <span>Đang xóa...</span>
  ) : (
    <button onClick={() => handleDelete(file.id)}>Xóa</button>
  );
}
```

---

## Cấu hình Cookie

### Cookie Name

```
AUTH-TOKEN
```

### Cookie Properties

- **httpOnly**: `true` (không thể truy cập từ JavaScript)
- **secure**: `false` (trong development, `true` trong production với HTTPS)
- **sameSite**: `Lax`
- **path**: `/`
- **maxAge**: `24 * 60 * 60` (24 giờ)

### Frontend Configuration

#### Fetch API

```typescript
// Mặc định cho tất cả fetch requests
fetch("/api/endpoint", {
  credentials: "include", // ⭐ Gửi cookie tự động
});
```

#### Axios

```typescript
// Cấu hình global
axios.defaults.withCredentials = true;

// Hoặc cho từng request
axios.get("/api/endpoint", {
  withCredentials: true,
});
```

#### React Query / TanStack Query

```typescript
import { QueryClient } from "@tanstack/react-query";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      credentials: "include", // ⭐
    },
    mutations: {
      credentials: "include", // ⭐
    },
  },
});
```

---

## Ví dụ hoàn chỉnh

### FileList Component với Delete

```typescript
import React, { useState, useEffect } from "react";

interface FileItem {
  id: string;
  originalFilename: string;
  fileSize: number;
  status: string;
  uploadedBy: {
    id: string;
    fullName: string;
    email: string;
  } | null;
  createdAt: string;
}

interface FileListProps {
  notebookId: string;
  currentUserId: string;
}

const FileList: React.FC<FileListProps> = ({ notebookId, currentUserId }) => {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingFileId, setDeletingFileId] = useState<string | null>(null);

  useEffect(() => {
    const fetchFiles = async () => {
      try {
        setLoading(true);
        const response = await fetch(`/user/notebooks/${notebookId}/files`, {
          credentials: "include", // ⭐
        });

        if (!response.ok) {
          throw new Error("Failed to fetch files");
        }

        const data = await response.json();
        setFiles(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load files");
      } finally {
        setLoading(false);
      }
    };

    fetchFiles();
  }, [notebookId]);

  const handleDelete = async (fileId: string, fileName: string) => {
    const confirmed = window.confirm(
      `Bạn có chắc chắn muốn xóa file "${fileName}"? Hành động này không thể hoàn tác.`
    );

    if (!confirmed) return;

    setDeletingFileId(fileId);
    try {
      const response = await fetch(
        `/user/notebooks/${notebookId}/files/${fileId}`,
        {
          method: "DELETE",
          credentials: "include", // ⭐
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Failed to delete file");
      }

      // Xóa thành công - cập nhật danh sách
      setFiles(files.filter((f) => f.id !== fileId));
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to delete file");
    } finally {
      setDeletingFileId(null);
    }
  };

  if (loading) return <div>Loading files...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Files ({files.length})</h2>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr>
            <th>File Name</th>
            <th>Size</th>
            <th>Status</th>
            <th>Uploaded By</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {files.map((file) => {
            const canDelete = file.uploadedBy?.id === currentUserId;
            const isDeleting = deletingFileId === file.id;

            return (
              <tr key={file.id}>
                <td>{file.originalFilename}</td>
                <td>{(file.fileSize / 1024).toFixed(2)} KB</td>
                <td>{file.status}</td>
                <td>{file.uploadedBy?.fullName || "Unknown"}</td>
                <td>
                  {canDelete ? (
                    <button
                      onClick={() =>
                        handleDelete(file.id, file.originalFilename)
                      }
                      disabled={isDeleting}
                      style={{
                        padding: "6px 12px",
                        backgroundColor: "#dc3545",
                        color: "white",
                        border: "none",
                        borderRadius: "4px",
                        cursor: isDeleting ? "not-allowed" : "pointer",
                        opacity: isDeleting ? 0.6 : 1,
                      }}
                    >
                      {isDeleting ? "Đang xóa..." : "Xóa"}
                    </button>
                  ) : (
                    <span style={{ color: "#999", fontSize: "12px" }}>
                      Không thể xóa
                    </span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default FileList;
```

---

## Tóm tắt

- **Endpoint**: `DELETE /user/notebooks/{notebookId}/files/{fileId}`
- **Authentication**: Cookie `AUTH-TOKEN` (tự động gửi với `credentials: 'include'`)
- **Response**: `204 No Content` (không có body)
- **Quyền**: Chỉ cho phép xóa file của chính user đó
- **Xóa gì**: File chunks, file storage, và file record

---

## Lưu ý quan trọng

1. ⚠️ **Luôn dùng `credentials: 'include'`** khi gọi API từ frontend
2. ⚠️ **Kiểm tra quyền trước khi hiển thị nút xóa** (chỉ hiển thị nếu `file.uploadedBy.id === currentUserId`)
3. ⚠️ **Luôn xác nhận trước khi xóa** để tránh xóa nhầm
4. ⚠️ **Xóa file là hành động không thể hoàn tác** - đảm bảo user hiểu rõ
