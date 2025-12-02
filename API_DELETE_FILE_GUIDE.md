# API Hướng dẫn: Xóa File (Delete File)

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Xóa file](#2-xóa-file)
3. [Ví dụ code (JavaScript/TypeScript)](#3-ví-dụ-code-javascripttypescript)

---

## 1. Tổng quan

API này cho phép admin xóa một file cụ thể trong notebook. Khi xóa file, hệ thống sẽ:

1. **Xóa tất cả file chunks** liên quan đến file đó
2. **Xóa file từ storage** (physical file trên disk)
3. **Xóa record** từ database

**Lưu ý quan trọng**:

- Hành động xóa là **không thể hoàn tác** (irreversible)
- Tất cả dữ liệu liên quan (chunks, embeddings) sẽ bị xóa vĩnh viễn
- Nên có confirmation dialog trước khi xóa

---

## 2. Xóa file

### Endpoint

```
DELETE /admin/notebooks/{notebookId}/files/{fileId}
```

### Mô tả

Xóa một file cụ thể trong notebook. File sẽ bị xóa hoàn toàn khỏi hệ thống, bao gồm:

- File chunks
- File trong storage
- Record trong database

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description         |
| ------------ | ---- | -------- | ------------------- |
| `notebookId` | UUID | Yes      | ID của notebook     |
| `fileId`     | UUID | Yes      | ID của file cần xóa |

### Request Body

Không cần request body.

### Response Format

### Success Response (204 No Content)

Không có response body. Status code `204` cho biết xóa thành công.

### Error Response (400 Bad Request)

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

### Error Response (401 Unauthorized)

```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

### Ví dụ sử dụng

```bash
curl -X 'DELETE' \
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/file-uuid-here' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <token>'
```

**Response**: `204 No Content` (không có body)

---

## 3. Ví dụ code (JavaScript/TypeScript)

### 1. Fetch API

```javascript
async function deleteFile(notebookId, fileId, token) {
  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/${fileId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    if (response.status === 204) {
      return { success: true };
    }
    const error = await response.json();
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  return { success: true };
}

// Sử dụng
try {
  await deleteFile(
    "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    "file-uuid-here",
    token
  );
  console.log("File đã được xóa thành công");
} catch (error) {
  console.error("Lỗi khi xóa file:", error.message);
}
```

### 2. Axios

```javascript
import axios from "axios";

async function deleteFile(notebookId, fileId, token) {
  try {
    const response = await axios.delete(
      `/admin/notebooks/${notebookId}/files/${fileId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return { success: true };
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || "Lỗi khi xóa file");
    }
    throw error;
  }
}

// Sử dụng
await deleteFile(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34",
  "file-uuid-here",
  token
);
```

### 3. React Hook Example

```typescript
import { useState } from "react";

function useDeleteFile() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const deleteFile = async (
    notebookId: string,
    fileId: string,
    token: string
  ): Promise<void> => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(
        `/admin/notebooks/${notebookId}/files/${fileId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "application/json",
          },
        }
      );

      if (!response.ok && response.status !== 204) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Lỗi khi xóa file");
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error("Unknown error");
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  return {
    deleteFile,
    loading,
    error,
  };
}

// Sử dụng trong component
function FileDeleteButton({
  notebookId,
  fileId,
  fileName,
}: {
  notebookId: string;
  fileId: string;
  fileName: string;
}) {
  const { deleteFile, loading, error } = useDeleteFile();
  const token = "your-token-here";
  const [showConfirm, setShowConfirm] = useState(false);

  const handleDelete = async () => {
    if (!showConfirm) {
      setShowConfirm(true);
      return;
    }

    try {
      await deleteFile(notebookId, fileId, token);
      alert(`File "${fileName}" đã được xóa thành công!`);
      setShowConfirm(false);
      // Refresh danh sách files hoặc navigate away
    } catch (err) {
      alert(`Lỗi: ${err instanceof Error ? err.message : "Unknown error"}`);
      setShowConfirm(false);
    }
  };

  if (showConfirm) {
    return (
      <div className="delete-confirmation">
        <p>Bạn có chắc chắn muốn xóa file "{fileName}"?</p>
        <p className="warning">
          ⚠️ Hành động này không thể hoàn tác. Tất cả dữ liệu liên quan sẽ bị
          xóa vĩnh viễn.
        </p>
        <div className="button-group">
          <button onClick={handleDelete} disabled={loading} className="danger">
            {loading ? "Đang xóa..." : "Xác nhận xóa"}
          </button>
          <button
            onClick={() => setShowConfirm(false)}
            disabled={loading}
            className="cancel"
          >
            Hủy
          </button>
        </div>
        {error && <p style={{ color: "red" }}>Lỗi: {error.message}</p>}
      </div>
    );
  }

  return (
    <button onClick={handleDelete} disabled={loading} className="delete-btn">
      {loading ? "Đang xóa..." : "Xóa file"}
    </button>
  );
}
```

### 4. React Component với Confirmation Dialog

```typescript
import React, { useState } from "react";

interface DeleteFileProps {
  notebookId: string;
  fileId: string;
  fileName: string;
  onDeleted?: () => void;
}

function DeleteFileButton({
  notebookId,
  fileId,
  fileName,
  onDeleted,
}: DeleteFileProps) {
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const token = "your-token-here";

  const handleDelete = async () => {
    try {
      setLoading(true);
      const response = await fetch(
        `/admin/notebooks/${notebookId}/files/${fileId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 204 || response.ok) {
        alert(`File "${fileName}" đã được xóa thành công!`);
        setShowDialog(false);
        if (onDeleted) {
          onDeleted();
        }
      } else {
        const error = await response.json();
        throw new Error(error.message || "Lỗi khi xóa file");
      }
    } catch (error) {
      alert(
        `Lỗi khi xóa file: ${
          error instanceof Error ? error.message : "Unknown error"
        }`
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <button
        onClick={() => setShowDialog(true)}
        className="btn-delete"
        disabled={loading}
      >
        Xóa
      </button>

      {showDialog && (
        <div className="modal-overlay" onClick={() => setShowDialog(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>Xác nhận xóa file</h3>
            <p>Bạn có chắc chắn muốn xóa file:</p>
            <p className="file-name">{fileName}</p>
            <p className="warning-text">
              ⚠️ Cảnh báo: Hành động này không thể hoàn tác. Tất cả dữ liệu liên
              quan (chunks, embeddings) sẽ bị xóa vĩnh viễn.
            </p>
            <div className="modal-actions">
              <button
                onClick={handleDelete}
                disabled={loading}
                className="btn-confirm-delete"
              >
                {loading ? "Đang xóa..." : "Xác nhận xóa"}
              </button>
              <button
                onClick={() => setShowDialog(false)}
                disabled={loading}
                className="btn-cancel"
              >
                Hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default DeleteFileButton;
```

### 5. TypeScript Types

```typescript
// Không có response type vì API trả về 204 No Content
// Chỉ cần handle success/error

interface DeleteFileError {
  status: number;
  message: string;
  timestamp: string;
}
```

### 6. CSS Example cho Confirmation Dialog

```css
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 8px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-content h3 {
  margin: 0 0 16px 0;
  color: #d32f2f;
}

.file-name {
  font-weight: 600;
  margin: 8px 0;
  color: #333;
}

.warning-text {
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 4px;
  padding: 12px;
  margin: 16px 0;
  color: #856404;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-confirm-delete {
  background: #d32f2f;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.btn-confirm-delete:hover:not(:disabled) {
  background: #b71c1c;
}

.btn-confirm-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-cancel:hover:not(:disabled) {
  background: #e0e0e0;
}

.btn-delete {
  background: #f44336;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-delete:hover:not(:disabled) {
  background: #d32f2f;
}

.btn-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```

---

## Ghi chú quan trọng

1. **Hành động không thể hoàn tác**:

   - Khi xóa file, tất cả dữ liệu liên quan sẽ bị xóa vĩnh viễn
   - File chunks, embeddings, và file trong storage đều bị xóa
   - Không có cách nào khôi phục lại

2. **Validation**:

   - File phải thuộc notebook được chỉ định
   - File phải tồn tại
   - Admin phải đã đăng nhập

3. **Response Status**:

   - `204 No Content`: Xóa thành công (không có response body)
   - `400 Bad Request`: File không thuộc notebook
   - `404 Not Found`: File không tồn tại
   - `401 Unauthorized`: Chưa đăng nhập hoặc không có quyền

4. **UI Best Practices**:

   - **Luôn hiển thị confirmation dialog** trước khi xóa
   - Hiển thị tên file trong confirmation để user biết chính xác file nào sẽ bị xóa
   - Cảnh báo rõ ràng về việc không thể hoàn tác
   - Hiển thị loading state khi đang xóa
   - Refresh danh sách files sau khi xóa thành công
   - Hiển thị thông báo thành công/thất bại

5. **Error Handling**:

   - Luôn xử lý lỗi khi gọi API
   - Hiển thị thông báo lỗi rõ ràng cho user
   - Không tự động đóng dialog nếu có lỗi

6. **Security**:
   - Chỉ admin mới có quyền xóa file
   - File phải thuộc notebook được chỉ định (tránh xóa nhầm)

## Ví dụ flow hoàn chỉnh

```typescript
async function handleDeleteFile(
  notebookId: string,
  fileId: string,
  fileName: string
) {
  // 1. Hiển thị confirmation
  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn xóa file "${fileName}"?\n\n` +
      "⚠️ Cảnh báo: Hành động này không thể hoàn tác."
  );

  if (!confirmed) {
    return;
  }

  try {
    // 2. Gọi API xóa
    const response = await fetch(
      `/admin/notebooks/${notebookId}/files/${fileId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    // 3. Xử lý response
    if (response.status === 204 || response.ok) {
      // 4. Thông báo thành công
      showSuccessMessage(`File "${fileName}" đã được xóa thành công!`);

      // 5. Refresh danh sách files
      refreshFileList();
    } else {
      // 6. Xử lý lỗi
      const error = await response.json();
      showErrorMessage(error.message || "Lỗi khi xóa file");
    }
  } catch (error) {
    // 7. Xử lý network error
    showErrorMessage("Không thể kết nối đến server");
  }
}
```
