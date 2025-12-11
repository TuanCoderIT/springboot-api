# API Upload Files - Hướng dẫn cho Frontend

## Endpoint

```
POST /user/notebooks/{notebookId}/files
```

## Content-Type

```
multipart/form-data
```

## Input

| Tên          | Kiểu        | Bắt buộc | Mô tả                              |
| ------------ | ----------- | -------- | ---------------------------------- |
| `notebookId` | UUID (path) | ✅       | ID của notebook                    |
| `files`      | File[]      | ✅       | Danh sách file (hỗ trợ nhiều file) |

### File được hỗ trợ

- `.pdf`
- `.doc`
- `.docx`

---

## Logic Status ban đầu

| Loại Notebook   | Role của User | Status     | Xử lý AI     |
| --------------- | ------------- | ---------- | ------------ |
| Notebook thường | member        | `approved` | ✅ Ngay      |
| Community       | `owner`       | `approved` | ✅ Ngay      |
| Community       | `admin`       | `approved` | ✅ Ngay      |
| Community       | `member`      | `pending`  | ❌ Chờ duyệt |

---

## Output (201 Created)

```json
[
  {
    "id": "uuid",
    "originalFilename": "document.pdf",
    "mimeType": "application/pdf",
    "fileSize": 1024000,
    "storageUrl": "https://...",
    "status": "approved | pending",
    "pagesCount": null,
    "ocrDone": false,
    "embeddingDone": false,
    "chunkSize": 3000,
    "chunkOverlap": 250,
    "chunksCount": null,
    "uploadedBy": {
      "id": "uuid",
      "fullName": "Nguyễn Văn A",
      "email": "a@example.com",
      "avatarUrl": "https://..."
    },
    "notebook": {
      "id": "uuid",
      "title": "Notebook Title",
      "description": "...",
      "type": "personal | community",
      "visibility": "public | private | members_only",
      "thumbnailUrl": "https://..."
    },
    "createdAt": "2025-12-11T10:00:00+07:00",
    "updatedAt": "2025-12-11T10:00:00+07:00"
  }
]
```

---

## Lỗi

| Status | Message                                                                |
| ------ | ---------------------------------------------------------------------- |
| 400    | "Vui lòng chọn ít nhất một file để upload."                            |
| 400    | "File không được để trống."                                            |
| 400    | "Chỉ chấp nhận file PDF và Word (.doc, .docx). File không hợp lệ: xxx" |
| 400    | "Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt"    |
| 400    | "Bạn chưa tham gia nhóm này"                                           |
| 401    | Token hết hạn/thiếu                                                    |
| 404    | "Notebook không tồn tại"                                               |

---

## Ví dụ Frontend

```typescript
const formData = new FormData();
files.forEach((file) => formData.append("files", file));

const res = await fetch(`/user/notebooks/${notebookId}/files`, {
  method: "POST",
  headers: { Authorization: `Bearer ${token}` },
  body: formData,
});
```
