# API: Get Active Conversation

## URL
```
GET /user/notebooks/{notebookId}/bot-chat/conversations/active
```

## Đầu vào

### Path Parameters
- `notebookId` (UUID, required): ID của notebook

### Headers
- `Authorization: Bearer <token>` (required): JWT token

### Query Parameters
Không có query parameters.

## Đầu ra

### Success - Có active conversation (200 OK)
```json
{
  "id": "7c1fa5a9-7efa-42d4-a49f-9dc5a3cad5f2",
  "title": "Bài thực hành 6",
  "notebookId": "95f69db9-e3e4-45d9-83ed-fe8d0cda70ba",
  "createdAt": "2025-12-08T01:00:00+07:00",
  "updatedAt": "2025-12-08T01:30:00+07:00",
  "firstMessage": null,
  "totalMessages": null
}
```

### Success - Chưa có active conversation (204 No Content)
Body rỗng.

### Error (401 Unauthorized)
```json
{
  "message": "User chưa đăng nhập."
}
```
