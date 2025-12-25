# API Đổi Tên File Quy Chế

## Endpoint

```
PUT /admin/regulation/files/{id}/rename
```

## Headers

| Key           | Value            |
| ------------- | ---------------- |
| Authorization | Bearer {token}   |
| Content-Type  | application/json |

## Path Parameters

| Param | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| id    | UUID | ✅       | ID của file |

## Request Body

```json
{
  "newFilename": "ten-file-moi.pdf"
}
```

| Field       | Type   | Required | Description                                     |
| ----------- | ------ | -------- | ----------------------------------------------- |
| newFilename | string | ✅       | Tên file mới (phải giữ nguyên đuôi mở rộng gốc) |

## Response

### Success (200)

```json
{
  "success": true,
  "message": "File renamed successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "originalFilename": "ten-file-moi.pdf",
    "mimeType": "application/pdf",
    "fileSize": 1024000,
    "storageUrl": "/uploads/abc123_ten-file-moi.pdf",
    "status": "approved",
    "pagesCount": 10,
    "ocrDone": true,
    "embeddingDone": true,
    "createdAt": "2024-12-24T10:00:00Z",
    "updatedAt": "2024-12-24T11:30:00Z"
  }
}
```

## Errors

### 400 - Đổi đuôi file

```json
{
  "success": false,
  "message": "Không được đổi đuôi file (cũ: .pdf, mới: .docx)"
}
```

### 400 - Tên file rỗng

```json
{
  "success": false,
  "message": "New filename cannot be empty"
}
```

### 404 - File không tồn tại

```json
{
  "success": false,
  "message": "File not found"
}
```

### 500 - Lỗi hệ thống (không thể đổi tên file vật lý)

```json
{
  "success": false,
  "message": "Không thể đổi tên file: File not found: uploads/abc.pdf"
}
```

## Lưu ý

- **Bắt buộc giữ nguyên đuôi file**: Nếu file gốc là `.pdf` thì tên mới cũng phải kết thúc bằng `.pdf`
- API chỉ cập nhật `originalFilename` (tên hiển thị) trong database
- File vật lý trong storage giữ nguyên tên UUID
