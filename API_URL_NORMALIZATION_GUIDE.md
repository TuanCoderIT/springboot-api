# Hướng dẫn: URL Normalization trong API Response

## Tổng quan

Tất cả các API trả về file URLs (storageUrl, thumbnailUrl, avatarUrl) đã được normalize để bao gồm **domain đầy đủ**. Frontend không cần xử lý thêm, có thể sử dụng trực tiếp URLs từ response.

## Cấu hình

Base URL được cấu hình trong `application.yml`:

```yaml
file:
  base-url: http://localhost:8386
```

**Lưu ý**: Trong production, cần cập nhật `file.base-url` thành domain thực tế (ví dụ: `https://api.example.com`).

## URL Format

### Trước khi normalize (trong database)

- `storageUrl`: `/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf`
- `thumbnailUrl`: `/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg`
- `avatarUrl`: `/uploads/avatar.jpg`

### Sau khi normalize (trong API response)

- `storageUrl`: `http://localhost:8386/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf`
- `thumbnailUrl`: `http://localhost:8386/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg`
- `avatarUrl`: `http://localhost:8386/uploads/avatar.jpg`

## Các API được normalize

### 1. GET /admin/files/pending

**Response Example**:

```json
{
  "content": [
    {
      "id": "file-uuid",
      "originalFilename": "document.pdf",
      "storageUrl": "http://localhost:8386/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf",
      "uploadedBy": {
        "id": "user-uuid",
        "fullName": "Nguyễn Văn A",
        "email": "nguyenvana@example.com",
        "avatarUrl": "http://localhost:8386/uploads/avatar.jpg"
      },
      "notebook": {
        "id": "notebook-uuid",
        "title": "Nhóm Cộng đồng",
        "thumbnailUrl": "http://localhost:8386/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg"
      }
    }
  ]
}
```

### 2. GET /admin/notebooks/{notebookId}/files

Tương tự, tất cả URLs đã được normalize.

### 3. PUT /admin/notebooks/{notebookId}/files/{fileId}/approve

**Response Example**:

```json
{
  "id": "file-uuid",
  "storageUrl": "http://localhost:8386/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf",
  "notebook": {
    "thumbnailUrl": "http://localhost:8386/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg"
  }
}
```

## Sử dụng trong Frontend

### 1. Sử dụng trực tiếp URL từ response

```typescript
interface NotebookFileResponse {
  storageUrl: string; // Đã có domain: "http://localhost:8386/uploads/..."
  notebook: {
    thumbnailUrl: string | null; // Đã có domain hoặc null
  };
  uploadedBy: {
    avatarUrl: string | null; // Đã có domain hoặc null
  };
}

function FileCard({ file }: { file: NotebookFileResponse }) {
  return (
    <div>
      {/* Sử dụng trực tiếp, không cần thêm domain */}
      <img src={file.notebook.thumbnailUrl || "/default-notebook.png"} />
      <a href={file.storageUrl} download>
        Download
      </a>
      <img src={file.uploadedBy.avatarUrl || "/default-avatar.png"} />
    </div>
  );
}
```

### 2. Kiểm tra URL đã có domain

```typescript
function isFullUrl(url: string | null): boolean {
  if (!url) return false;
  return url.startsWith("http://") || url.startsWith("https://");
}

function FileCard({ file }: { file: NotebookFileResponse }) {
  const thumbnailUrl = file.notebook.thumbnailUrl;

  // URL đã được normalize, luôn có domain
  if (isFullUrl(thumbnailUrl)) {
    console.log("URL đã có domain:", thumbnailUrl);
  }

  return <img src={thumbnailUrl || "/default.png"} />;
}
```

### 3. Xử lý null URLs

```typescript
function FileCard({ file }: { file: NotebookFileResponse }) {
  // thumbnailUrl có thể null, cần fallback
  const thumbnailUrl =
    file.notebook.thumbnailUrl || "http://localhost:8386/default-notebook.png";

  const avatarUrl =
    file.uploadedBy.avatarUrl || "http://localhost:8386/default-avatar.png";

  return (
    <div>
      <img src={thumbnailUrl} alt={file.notebook.title} />
      <img src={avatarUrl} alt={file.uploadedBy.fullName} />
    </div>
  );
}
```

### 4. React Component Example

```typescript
import React from "react";

interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  storageUrl: string; // Full URL với domain
  notebook: {
    id: string;
    title: string;
    thumbnailUrl: string | null; // Full URL với domain hoặc null
  };
  uploadedBy: {
    id: string;
    fullName: string;
    avatarUrl: string | null; // Full URL với domain hoặc null
  };
}

function PendingFileCard({ file }: { file: NotebookFileResponse }) {
  return (
    <div className="file-card">
      <h3>{file.originalFilename}</h3>

      {/* Notebook thumbnail - URL đã có domain */}
      {file.notebook.thumbnailUrl && (
        <img
          src={file.notebook.thumbnailUrl}
          alt={file.notebook.title}
          className="notebook-thumbnail"
        />
      )}

      {/* Download link - URL đã có domain */}
      <a
        href={file.storageUrl}
        download={file.originalFilename}
        className="download-link"
      >
        Download File
      </a>

      {/* User avatar - URL đã có domain */}
      {file.uploadedBy.avatarUrl && (
        <img
          src={file.uploadedBy.avatarUrl}
          alt={file.uploadedBy.fullName}
          className="user-avatar"
        />
      )}
    </div>
  );
}
```

## Lưu ý quan trọng

1. **URLs đã có domain**: Tất cả URLs trong response đã được normalize với domain đầy đủ, không cần thêm domain phía frontend.

2. **Null handling**:

   - `thumbnailUrl` có thể `null` → cần fallback image
   - `avatarUrl` có thể `null` → cần fallback image
   - `storageUrl` không bao giờ `null` (file luôn có storageUrl)

3. **Production**:

   - Cần cập nhật `file.base-url` trong `application.yml` cho production
   - Hoặc sử dụng environment variable: `file.base-url: ${FILE_BASE_URL:http://localhost:8386}`

4. **CORS**: Nếu frontend và backend ở domain khác nhau, cần cấu hình CORS để cho phép load images từ backend domain.

5. **HTTPS**: Trong production, đảm bảo `file.base-url` sử dụng HTTPS.

## Ví dụ cấu hình production

```yaml
# application-prod.yml
file:
  base-url: https://api.example.com
```

Hoặc sử dụng environment variable:

```yaml
file:
  base-url: ${FILE_BASE_URL:http://localhost:8386}
```

```bash
# Set environment variable
export FILE_BASE_URL=https://api.example.com
```

## So sánh

### ❌ Không cần làm (URL đã được normalize)

```typescript
// KHÔNG CẦN làm thế này
const baseUrl = "http://localhost:8386";
const fullUrl = baseUrl + file.storageUrl; // ❌ Không cần
```

### ✅ Sử dụng trực tiếp

```typescript
// Sử dụng trực tiếp URL từ response
const fullUrl = file.storageUrl; // ✅ Đã có domain đầy đủ
```

## Testing

Khi test API, bạn sẽ thấy URLs có dạng:

```json
{
  "storageUrl": "http://localhost:8386/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf",
  "notebook": {
    "thumbnailUrl": "http://localhost:8386/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg"
  }
}
```

Thay vì:

```json
{
  "storageUrl": "/uploads/b8e5482f-8851-4873-b1fc-cd8830ce73ae.pdf",
  "notebook": {
    "thumbnailUrl": "/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg"
  }
}
```
