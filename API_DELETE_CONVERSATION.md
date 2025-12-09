# API: Delete Conversation

## URL
```
DELETE /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}
```

## Đầu vào

### Path Parameters
- `notebookId` (UUID, required): ID của notebook
- `conversationId` (UUID, required): ID của conversation cần xóa

### Headers
- `Authorization: Bearer <token>` (required): JWT token

### Request Body
Không có request body.

## Đầu ra

### Success (204 No Content)
Body rỗng. Conversation đã được xóa thành công.

### Error (400 Bad Request)
```json
{
  "message": "Bạn chỉ có thể xóa conversation của chính mình."
}
```

**Các trường hợp lỗi 400:**
- `"Conversation không thuộc về notebook này."` - Conversation không thuộc notebook được chỉ định
- `"Không thể xóa conversation này. Conversation không có thông tin người tạo."` - Conversation không có createdBy
- `"Bạn chỉ có thể xóa conversation của chính mình."` - User không phải người tạo conversation

### Error (404 Not Found)
```json
{
  "message": "Notebook không tồn tại."
}
```

```json
{
  "message": "Conversation không tồn tại."
}
```

### Error (401 Unauthorized)
```json
{
  "message": "User chưa đăng nhập."
}
```

