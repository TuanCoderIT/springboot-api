# Hướng dẫn: Sử dụng thông tin Notebook trong API Pending Files

## Tổng quan

API `GET /admin/files/pending` đã được cập nhật để bao gồm thông tin notebook cho mỗi file. Điều này giúp frontend hiển thị thông tin notebook mà file thuộc về mà không cần gọi thêm API.

## Thay đổi trong Response

### Trước đây

```json
{
  "content": [
    {
      "id": "file-uuid",
      "originalFilename": "document.pdf",
      "uploadedBy": { ... },
      // Không có thông tin notebook
    }
  ]
}
```

### Bây giờ

```json
{
  "content": [
    {
      "id": "file-uuid",
      "originalFilename": "document.pdf",
      "uploadedBy": {
        "id": "user-uuid",
        "fullName": "Nguyễn Văn A",
        "email": "nguyenvana@example.com",
        "avatarUrl": "/uploads/avatar.jpg"
      },
      "notebook": {
        "id": "notebook-uuid",
        "title": "Nhóm Cộng đồng Toán học",
        "description": "Chia sẻ tài liệu và thảo luận về toán học",
        "type": "community",
        "visibility": "public",
        "thumbnailUrl": "/uploads/notebook-thumbnail.jpg"
      }
    }
  ]
}
```

## Cấu trúc NotebookInfo

| Field          | Type   | Description                     | Ví dụ                                    |
| -------------- | ------ | ------------------------------- | ---------------------------------------- |
| `id`           | UUID   | ID của notebook                 | "c3a7f558-faa7-4218-ae41-..."            |
| `title`        | String | Tiêu đề notebook                | "Nhóm Cộng đồng Toán học"                |
| `description`  | String | Mô tả notebook (có thể null)    | "Chia sẻ tài liệu..."                    |
| `type`         | String | Loại notebook                   | "community", "private_group", "personal" |
| `visibility`   | String | Hiển thị                        | "public", "private"                      |
| `thumbnailUrl` | String | URL ảnh thumbnail (có thể null) | "/uploads/thumbnail.jpg"                 |

### Giá trị hợp lệ cho `type`:

- `"community"` - Nhóm cộng đồng
- `"private_group"` - Nhóm riêng tư
- `"personal"` - Notebook cá nhân

### Giá trị hợp lệ cho `visibility`:

- `"public"` - Công khai
- `"private"` - Riêng tư

## Ví dụ sử dụng trong Frontend

### 1. Hiển thị thông tin notebook trong danh sách files

```typescript
interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  // ... other fields
  uploadedBy: UploaderInfo;
  notebook: NotebookInfo; // ← Thông tin notebook mới
  createdAt: string;
  updatedAt: string;
}

interface NotebookInfo {
  id: string;
  title: string;
  description: string | null;
  type: "community" | "private_group" | "personal";
  visibility: "public" | "private";
  thumbnailUrl: string | null;
}

function PendingFilesList({ files }: { files: NotebookFileResponse[] }) {
  return (
    <div>
      {files.map((file) => (
        <div key={file.id} className="file-card">
          <h3>{file.originalFilename}</h3>

          {/* Hiển thị thông tin notebook */}
          <div className="notebook-info">
            <img
              src={file.notebook.thumbnailUrl || "/default-notebook.png"}
              alt={file.notebook.title}
              className="notebook-thumbnail"
            />
            <div>
              <h4>{file.notebook.title}</h4>
              {file.notebook.description && <p>{file.notebook.description}</p>}
              <span className={`badge badge-${file.notebook.type}`}>
                {file.notebook.type === "community"
                  ? "Cộng đồng"
                  : file.notebook.type === "private_group"
                  ? "Nhóm riêng"
                  : "Cá nhân"}
              </span>
            </div>
          </div>

          {/* Thông tin người upload */}
          <div className="uploader-info">
            <p>
              Đóng góp bởi: {file.uploadedBy.fullName} ({file.uploadedBy.email})
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}
```

### 2. Lọc files theo notebook type

```typescript
function filterFilesByNotebookType(
  files: NotebookFileResponse[],
  type: "community" | "private_group" | "personal"
): NotebookFileResponse[] {
  return files.filter((file) => file.notebook.type === type);
}

// Sử dụng
const communityFiles = filterFilesByNotebookType(files, "community");
const privateGroupFiles = filterFilesByNotebookType(files, "private_group");
```

### 3. Nhóm files theo notebook

```typescript
function groupFilesByNotebook(
  files: NotebookFileResponse[]
): Map<string, NotebookFileResponse[]> {
  const grouped = new Map<string, NotebookFileResponse[]>();

  files.forEach((file) => {
    const notebookId = file.notebook.id;
    if (!grouped.has(notebookId)) {
      grouped.set(notebookId, []);
    }
    grouped.get(notebookId)!.push(file);
  });

  return grouped;
}

// Sử dụng
const groupedFiles = groupFilesByNotebook(files);
groupedFiles.forEach((files, notebookId) => {
  const notebook = files[0].notebook;
  console.log(`Notebook: ${notebook.title} - ${files.length} files`);
});
```

### 4. Tạo link đến notebook

```typescript
function FileCard({ file }: { file: NotebookFileResponse }) {
  const notebookUrl = `/notebooks/${file.notebook.id}`;

  return (
    <div className="file-card">
      <h3>{file.originalFilename}</h3>

      <a href={notebookUrl} className="notebook-link">
        <img
          src={file.notebook.thumbnailUrl || "/default-notebook.png"}
          alt={file.notebook.title}
        />
        <div>
          <h4>{file.notebook.title}</h4>
          <p className="notebook-type">
            {file.notebook.type === "community"
              ? "Cộng đồng"
              : file.notebook.type === "private_group"
              ? "Nhóm riêng"
              : "Cá nhân"}
          </p>
        </div>
      </a>
    </div>
  );
}
```

### 5. Hiển thị badge cho notebook type

```typescript
function NotebookTypeBadge({ type }: { type: string }) {
  const badgeConfig = {
    community: { label: "Cộng đồng", color: "blue" },
    private_group: { label: "Nhóm riêng", color: "green" },
    personal: { label: "Cá nhân", color: "gray" },
  };

  const config = badgeConfig[type as keyof typeof badgeConfig] || {
    label: type,
    color: "gray",
  };

  return <span className={`badge badge-${config.color}`}>{config.label}</span>;
}

// Sử dụng
<NotebookTypeBadge type={file.notebook.type} />;
```

### 6. React Component hoàn chỉnh

```typescript
import React from "react";

interface NotebookFileResponse {
  id: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  status: string;
  uploadedBy: {
    id: string;
    fullName: string;
    email: string;
    avatarUrl: string | null;
  };
  notebook: {
    id: string;
    title: string;
    description: string | null;
    type: "community" | "private_group" | "personal";
    visibility: "public" | "private";
    thumbnailUrl: string | null;
  };
  createdAt: string;
  updatedAt: string;
}

function PendingFileCard({ file }: { file: NotebookFileResponse }) {
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const getNotebookTypeLabel = (type: string): string => {
    const labels: Record<string, string> = {
      community: "Cộng đồng",
      private_group: "Nhóm riêng",
      personal: "Cá nhân",
    };
    return labels[type] || type;
  };

  return (
    <div className="pending-file-card">
      <div className="file-header">
        <h3>{file.originalFilename}</h3>
        <span className="file-status status-pending">Chờ duyệt</span>
      </div>

      <div className="notebook-section">
        <a href={`/notebooks/${file.notebook.id}`} className="notebook-link">
          {file.notebook.thumbnailUrl && (
            <img
              src={file.notebook.thumbnailUrl}
              alt={file.notebook.title}
              className="notebook-thumbnail"
            />
          )}
          <div className="notebook-info">
            <h4>{file.notebook.title}</h4>
            {file.notebook.description && (
              <p className="notebook-description">
                {file.notebook.description}
              </p>
            )}
            <div className="notebook-meta">
              <span className={`badge badge-${file.notebook.type}`}>
                {getNotebookTypeLabel(file.notebook.type)}
              </span>
              <span className="badge badge-visibility">
                {file.notebook.visibility === "public"
                  ? "Công khai"
                  : "Riêng tư"}
              </span>
            </div>
          </div>
        </a>
      </div>

      <div className="file-meta">
        <div className="uploader-info">
          <img
            src={file.uploadedBy.avatarUrl || "/default-avatar.png"}
            alt={file.uploadedBy.fullName}
            className="avatar"
          />
          <div>
            <p className="uploader-name">{file.uploadedBy.fullName}</p>
            <p className="uploader-email">{file.uploadedBy.email}</p>
          </div>
        </div>
        <div className="file-details">
          <p>Kích thước: {formatFileSize(file.fileSize)}</p>
          <p>Loại: {file.mimeType}</p>
          <p>Upload: {new Date(file.createdAt).toLocaleString("vi-VN")}</p>
        </div>
      </div>
    </div>
  );
}

export default PendingFileCard;
```

## CSS Example

```css
.pending-file-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  background: white;
}

.notebook-section {
  margin: 16px 0;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
}

.notebook-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: inherit;
  gap: 12px;
}

.notebook-thumbnail {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
}

.notebook-info h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
}

.notebook-description {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}

.notebook-meta {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge-community {
  background: #e3f2fd;
  color: #1976d2;
}

.badge-private_group {
  background: #e8f5e9;
  color: #388e3c;
}

.badge-personal {
  background: #f5f5f5;
  color: #616161;
}

.badge-visibility {
  background: #fff3e0;
  color: #f57c00;
}
```

## Lợi ích

1. **Giảm số lượng API calls**: Không cần gọi thêm API để lấy thông tin notebook
2. **Hiển thị ngay lập tức**: Có thể hiển thị thông tin notebook ngay trong danh sách files
3. **Dễ dàng lọc và nhóm**: Có thể lọc hoặc nhóm files theo notebook mà không cần thêm logic phức tạp
4. **Cải thiện UX**: User có thể thấy ngay file thuộc notebook nào và click vào để xem chi tiết

## Lưu ý

- Thông tin notebook luôn có sẵn trong response, không cần kiểm tra null (trừ `description` và `thumbnailUrl`)
- Có thể sử dụng `notebook.id` để tạo link đến trang chi tiết notebook
- Có thể sử dụng `notebook.type` để hiển thị badge hoặc icon khác nhau
- `notebook.thumbnailUrl` có thể null, nên có fallback image
