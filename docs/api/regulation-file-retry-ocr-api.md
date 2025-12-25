# API Retry OCR File Quy Chế

## Endpoint

```
POST /admin/regulation/files/{id}/retry-ocr
```

## Headers

| Key           | Value          |
| ------------- | -------------- |
| Authorization | Bearer {token} |

## Path Parameters

| Param | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| id    | UUID | ✅       | ID của file |

## Request Body

Không cần body.

## Response

### Success (200)

```json
{
  "success": true,
  "message": "OCR retry started",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "originalFilename": "quy-che-dao-tao.pdf",
    "mimeType": "application/pdf",
    "fileSize": 1024000,
    "storageUrl": "/uploads/abc123_quy-che-dao-tao.pdf",
    "status": "pending",
    "pagesCount": null,
    "ocrDone": false,
    "embeddingDone": false,
    "createdAt": "2024-12-24T10:00:00Z",
    "updatedAt": "2024-12-24T11:45:00Z"
  }
}
```

> **Lưu ý**: `status` sẽ chuyển sang `"pending"` ngay lập tức. Sau đó hệ thống sẽ xử lý async và chuyển sang `"processing"` → `"done"` hoặc `"failed"`.

## Errors

### 404 - File không tồn tại

```json
{
  "success": false,
  "message": "File not found"
}
```

## Flow xử lý

1. API reset trạng thái file về `pending`, `ocrDone=false`, `embeddingDone=false`
2. Trigger lại AI processing (async)
3. Response trả về ngay với `status: "pending"`
4. FE có thể poll hoặc dùng WebSocket để theo dõi trạng thái

## Khi nào cần retry?

- File có `status: "failed"`
- File có `ocrDone: false` sau một thời gian dài
- Admin muốn cập nhật lại embedding với config mới
