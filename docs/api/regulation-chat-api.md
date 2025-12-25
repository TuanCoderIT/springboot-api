# API Chat Regulation

Base path: `/user/regulation/chat`

## Headers (Áp dụng cho tất cả endpoints)

| Key           | Value          |
| ------------- | -------------- |
| Authorization | Bearer {token} |

---

## 1. Tạo Conversation Mới

### Endpoint

```
POST /user/regulation/chat/conversations
```

### Query Parameters

| Param | Type   | Required | Description                           |
| ----- | ------ | -------- | ------------------------------------- |
| title | string | ❌       | Tiêu đề conversation (mặc định: null) |

### Response (200)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Hỏi về quy chế đào tạo",
  "notebookId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "isActive": true,
  "createdAt": "2024-12-24T10:00:00Z",
  "updatedAt": "2024-12-24T10:00:00Z"
}
```

---

## 2. Lấy Danh Sách Conversations

### Endpoint

```
GET /user/regulation/chat/conversations
```

### Query Parameters

| Param      | Type    | Required | Description                                 |
| ---------- | ------- | -------- | ------------------------------------------- |
| cursorNext | UUID    | ❌       | ID của conversation cuối cùng từ page trước |
| limit      | integer | ❌       | Số lượng items (mặc định 10, max 100)       |

### Response (200)

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Hỏi về quy chế đào tạo",
      "notebookId": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "770e8400-e29b-41d4-a716-446655440002",
      "isActive": true,
      "createdAt": "2024-12-24T10:00:00Z",
      "updatedAt": "2024-12-24T10:00:00Z"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440003",
      "title": "Câu hỏi về học phí",
      "notebookId": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "770e8400-e29b-41d4-a716-446655440002",
      "isActive": false,
      "createdAt": "2024-12-23T14:30:00Z",
      "updatedAt": "2024-12-23T15:00:00Z"
    }
  ],
  "cursorNext": "550e8400-e29b-41d4-a716-446655440003",
  "hasMore": true
}
```

### Lưu ý

- Sử dụng cursor pagination (không phải offset pagination)
- `cursorNext` trả về ID của item cuối cùng để load page tiếp theo
- `hasMore = false` khi đã hết dữ liệu

---

## 3. Lấy Conversation Active

### Endpoint

```
GET /user/regulation/chat/conversations/active
```

### Response (200)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Hỏi về quy chế đào tạo",
  "notebookId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "isActive": true,
  "createdAt": "2024-12-24T10:00:00Z",
  "updatedAt": "2024-12-24T10:00:00Z"
}
```

### Response (204 No Content)

Khi user chưa có conversation nào active.

---

## 4. Set Conversation Active

### Endpoint

```
POST /user/regulation/chat/conversations/{conversationId}/active
```

### Path Parameters

| Param          | Type | Required | Description         |
| -------------- | ---- | -------- | ------------------- |
| conversationId | UUID | ✅       | ID của conversation |

### Response (200)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Hỏi về quy chế đào tạo",
  "notebookId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "isActive": true,
  "createdAt": "2024-12-24T10:00:00Z",
  "updatedAt": "2024-12-24T10:00:00Z"
}
```

### Errors

**404 - Conversation không tồn tại**

```json
{
  "success": false,
  "message": "Conversation not found"
}
```

---

## 5. Lấy Messages của Conversation

### Endpoint

```
GET /user/regulation/chat/conversations/{conversationId}/messages
```

### Path Parameters

| Param          | Type | Required | Description         |
| -------------- | ---- | -------- | ------------------- |
| conversationId | UUID | ✅       | ID của conversation |

### Query Parameters

| Param      | Type | Required | Description                              |
| ---------- | ---- | -------- | ---------------------------------------- |
| cursorNext | UUID | ❌       | ID của message cũ nhất từ lần load trước |

### Response (200)

```json
{
  "items": [
    {
      "id": "msg-uuid-1",
      "role": "user",
      "content": "Quy chế điểm thi như thế nào?",
      "createdAt": "2024-12-24T10:05:00Z",
      "files": [
        {
          "id": "file-uuid-1",
          "fileType": "image",
          "fileUrl": "http://localhost:8080/uploads/screenshot.png",
          "mimeType": "image/png",
          "fileName": "screenshot.png",
          "ocrText": "Điều 15. Quy định về điểm thi..."
        }
      ],
      "sources": []
    },
    {
      "id": "msg-uuid-2",
      "role": "assistant",
      "content": "Theo quy chế đào tạo, điểm thi được tính như sau...",
      "createdAt": "2024-12-24T10:05:10Z",
      "files": [],
      "model": {
        "id": "model-uuid",
        "code": "gemini",
        "provider": "google"
      },
      "sources": [
        {
          "sourceType": "RAG",
          "fileId": "file-uuid",
          "chunkIndex": 5,
          "score": 0.95,
          "provider": "rag"
        }
      ]
    }
  ],
  "cursorNext": "msg-uuid-2",
  "hasMore": true
}
```

### Lưu ý

- Messages được sắp xếp theo thời gian (mới nhất trước)
- Sử dụng cursor pagination
- Mỗi lần load 10 messages
- `cursorNext` là ID của message cuối cùng để load page tiếp theo

---

## 6. Xóa Conversation

### Endpoint

```
DELETE /user/regulation/chat/conversations/{conversationId}
```

### Path Parameters

| Param          | Type | Required | Description         |
| -------------- | ---- | -------- | ------------------- |
| conversationId | UUID | ✅       | ID của conversation |

### Response (200 - Có conversation tiếp theo)

Trả về conversation tiếp theo khi xóa conversation đang active:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440004",
  "title": "Conversation mới nhất",
  "notebookId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "isActive": true,
  "createdAt": "2024-12-23T12:00:00Z",
  "updatedAt": "2024-12-23T12:00:00Z"
}
```

### Response (204 No Content)

Xóa thành công, không có body.

### Errors

**404 - Conversation không tồn tại**

```json
{
  "success": false,
  "message": "Conversation not found"
}
```

**403 - Không có quyền xóa**

```json
{
  "success": false,
  "message": "You don't have permission to delete this conversation"
}
```

### Lưu ý

- Chỉ người tạo conversation mới được xóa
- Xóa conversation sẽ xóa tất cả messages bên trong (cascade delete)

---

## Flow sử dụng

1. **Lần đầu vào trang:** Gọi `GET /conversations/active`

   - Nếu 200: Hiển thị conversation active
   - Nếu 204: Gọi `POST /conversations` để tạo mới

2. **Chuyển conversation:** Gọi `POST /conversations/{id}/active`

3. **Load thêm conversations:** Gọi `GET /conversations?cursorNext={lastId}`
