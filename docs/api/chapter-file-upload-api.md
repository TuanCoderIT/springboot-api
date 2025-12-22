# API: Upload File vào Chương

## Endpoint

```
POST /lecturer/chapters/{chapterId}/files
```

## Headers

```
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data
```

---

## Request

### Path Parameters

| Param       | Type | Mô tả                         |
| ----------- | ---- | ----------------------------- |
| `chapterId` | UUID | ID của chương cần upload file |

### Form Data

| Field                      | Type   | Bắt buộc | Mô tả                     |
| -------------------------- | ------ | -------- | ------------------------- |
| `files`                    | File[] | ✅       | Danh sách file cần upload |
| `fileInfos[0].title`       | string | ❌       | Tiêu đề cho file thứ 1    |
| `fileInfos[0].description` | string | ❌       | Mô tả cho file thứ 1      |
| `fileInfos[1].title`       | string | ❌       | Tiêu đề cho file thứ 2    |
| `fileInfos[1].description` | string | ❌       | Mô tả cho file thứ 2      |
| ...                        |        |          | (theo index của files)    |

> **Lưu ý:** `fileInfos` theo thứ tự tương ứng với `files`. Nếu không truyền, title mặc định là tên file gốc.

### Supported File Types

- `.pdf` - PDF
- `.doc`, `.docx` - Microsoft Word
- `.ppt`, `.pptx` - Microsoft PowerPoint

---

## Response

### Success (201 Created)

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "itemType": "FILE",
    "refId": "660e8400-e29b-41d4-a716-446655440001",
    "title": "Bài giảng Chương 1",
    "sortOrder": 0,
    "metadata": {
      "mimeType": "application/pdf",
      "fileSize": 1024000,
      "storageUrl": "/uploads/abc123.pdf",
      "originalFilename": "week1.pdf",
      "description": "Tài liệu ôn tập giữa kỳ"
    },
    "createdAt": "2024-12-22T10:00:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "itemType": "FILE",
    "refId": "660e8400-e29b-41d4-a716-446655440003",
    "title": "Bài tập thực hành",
    "sortOrder": 1,
    "metadata": {
      "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "fileSize": 512000,
      "storageUrl": "/uploads/def456.docx",
      "originalFilename": "exercises.docx",
      "description": "10 bài tập cơ bản"
    },
    "createdAt": "2024-12-22T10:00:00Z"
  }
]
```

### Errors

| Status | Message                                    | Nguyên nhân                          |
| ------ | ------------------------------------------ | ------------------------------------ |
| 400    | `Chỉ hỗ trợ file PDF, Word, PowerPoint...` | File không đúng định dạng            |
| 400    | `Tên file không hợp lệ`                    | File không có tên                    |
| 403    | `Bạn không có quyền truy cập Notebook này` | Không phải giảng viên của assignment |
| 404    | `Chương không tồn tại`                     | `chapterId` không tồn tại            |
| 404    | `User không tồn tại`                       | Token không hợp lệ                   |

---

## TypeScript

### Interfaces

```typescript
interface FileMetadata {
  title?: string;
  description?: string;
}

interface ChapterItemResponse {
  id: string;
  itemType: "FILE" | "LECTURE" | "QUIZ" | "VIDEO" | "NOTE" | "FLASHCARD";
  refId: string | null;
  title: string;
  sortOrder: number;
  metadata: {
    mimeType?: string;
    fileSize?: number;
    storageUrl?: string;
    originalFilename?: string;
    description?: string;
  };
  createdAt: string;
}
```

### API Function

```typescript
async function uploadChapterFiles(
  chapterId: string,
  filesWithInfo: Array<{ file: File; title?: string; description?: string }>
): Promise<ChapterItemResponse[]> {
  const formData = new FormData();

  filesWithInfo.forEach((item, index) => {
    formData.append("files", item.file);
    if (item.title) {
      formData.append(`fileInfos[${index}].title`, item.title);
    }
    if (item.description) {
      formData.append(`fileInfos[${index}].description`, item.description);
    }
  });

  const res = await fetch(`/api/v1/lecturer/chapters/${chapterId}/files`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.message);
  }

  return res.json();
}
```

### React Example

```tsx
interface FileUploadItem {
  file: File;
  title: string;
  description: string;
}

const [files, setFiles] = useState<FileUploadItem[]>([]);
const [uploading, setUploading] = useState(false);

const handleAddFiles = (newFiles: FileList) => {
  const items = Array.from(newFiles).map((file) => ({
    file,
    title: file.name.replace(/\.[^/.]+$/, ""), // Bỏ extension làm default title
    description: "",
  }));
  setFiles((prev) => [...prev, ...items]);
};

const handleUpload = async () => {
  if (files.length === 0) return;

  setUploading(true);
  try {
    const items = await uploadChapterFiles(chapterId, files);
    console.log("Uploaded:", items);
    setFiles([]);
    // Refresh chapter items list
  } catch (error) {
    toast.error(error.message);
  } finally {
    setUploading(false);
  }
};

// UI cho phép user chỉnh title/description cho từng file trước khi upload
return (
  <div>
    <input
      type="file"
      multiple
      onChange={(e) => handleAddFiles(e.target.files!)}
    />

    {files.map((item, i) => (
      <div key={i}>
        <input
          value={item.title}
          onChange={(e) => {
            const updated = [...files];
            updated[i].title = e.target.value;
            setFiles(updated);
          }}
          placeholder="Tiêu đề"
        />
        <input
          value={item.description}
          onChange={(e) => {
            const updated = [...files];
            updated[i].description = e.target.value;
            setFiles(updated);
          }}
          placeholder="Mô tả"
        />
      </div>
    ))}

    <button onClick={handleUpload} disabled={uploading}>
      {uploading ? "Đang upload..." : "Upload"}
    </button>
  </div>
);
```

---

## Lưu ý

1. **Mỗi file có title/description riêng** - Truyền theo index tương ứng
2. **AI Processing tự động** - Sau upload, file được xử lý OCR + chunking + embedding (async)
3. **Chunk config cố định** - `chunkSize=2000`, `chunkOverlap=200`
4. **sortOrder tự tăng** - File mới thêm vào cuối danh sách
5. **Xóa file** - Dùng `DELETE /lecturer/chapter-items/{itemId}`
