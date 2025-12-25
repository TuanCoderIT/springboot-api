# API Xóa File Quy Chế

## Endpoint

```
DELETE /admin/regulation/files/{id}
```

## Headers

| Key           | Value          |
| ------------- | -------------- |
| Authorization | Bearer {token} |

## Path Parameters

| Param | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| id    | UUID | ✅       | ID của file |

## Response

### Success (200)

```json
{
  "success": true,
  "message": "File deleted successfully",
  "data": null
}
```

## Errors

### 404 - File không tồn tại

```json
{
  "success": false,
  "message": "File not found"
}
```

## Side Effects

Khi gọi API này, hệ thống sẽ thực hiện các hành động sau:

1. **Xóa file vật lý**: Xóa file gốc trong thư mục `uploads/`
2. **Xóa metadata**: Xóa bản ghi `notebook_files` trong database
3. **Cascade delete**:
   - Xóa tất cả các `file_chunks` liên quan (vector embeddings)
   - Xóa các liên kết trong `notebook_ai_set_files`

> **Lưu ý**: Hành động này không thể hoàn tác (cannot undo).
