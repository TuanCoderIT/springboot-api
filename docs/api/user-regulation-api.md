# API User Regulation

## 1. Get Regulation Notebook

### Endpoint

```
GET /user/regulation/notebook
```

### Headers

| Key           | Value          |
| ------------- | -------------- |
| Authorization | Bearer {token} |

### Response (200)

```json
{
  "success": true,
  "message": "Regulation notebook",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Quy chế đào tạo",
    "description": "Các văn bản quy chế liên quan đến đào tạo",
    "createdAt": "2024-12-24T10:00:00Z",
    "updatedAt": "2024-12-24T11:00:00Z",
    "totalFiles": 25
  }
}
```

### Errors

**404 - Notebook không tồn tại**

```json
{
  "success": false,
  "message": "Regulation notebook not found"
}
```

---

## 2. Get Regulation Files

### Endpoint

```
GET /user/regulation/files
```

### Headers

| Key           | Value          |
| ------------- | -------------- |
| Authorization | Bearer {token} |

### Query Parameters

| Param         | Type    | Required | Default   | Description              |
| ------------- | ------- | -------- | --------- | ------------------------ |
| page          | integer | ❌       | 0         | Số trang (0-indexed)     |
| size          | integer | ❌       | 10        | Số item mỗi trang        |
| search        | string  | ❌       | null      | Tìm kiếm theo tên file   |
| sortBy        | string  | ❌       | createdAt | Sắp xếp theo field       |
| sortDirection | string  | ❌       | desc      | Hướng sắp xếp (asc/desc) |

### Response (200)

```json
{
  "success": true,
  "message": "Regulation files",
  "data": {
    "items": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "originalFilename": "quy-che-dao-tao.pdf",
        "mimeType": "application/pdf",
        "fileSize": 1024000,
        "storageUrl": "http://localhost:8080/uploads/abc123.pdf",
        "pagesCount": 15,
        "uploadedByName": "Nguyễn Văn A",
        "createdAt": "2024-12-24T10:00:00Z",
        "updatedAt": "2024-12-24T11:00:00Z"
      },
      {
        "id": "223e4567-e89b-12d3-a456-426614174001",
        "originalFilename": "quy-dinh-hoc-vu.pdf",
        "mimeType": "application/pdf",
        "fileSize": 2048000,
        "storageUrl": "http://localhost:8080/uploads/def456.pdf",
        "pagesCount": 8,
        "uploadedByName": "Trần Thị B",
        "createdAt": "2024-12-23T14:30:00Z",
        "updatedAt": "2024-12-23T15:00:00Z"
      }
    ],
    "meta": {
      "page": 0,
      "size": 10,
      "totalElements": 25,
      "totalPages": 3
    }
  }
}
```

### Errors

**404 - Notebook không tồn tại**

```json
{
  "success": false,
  "message": "Regulation notebook not found"
}
```

---

## Lưu ý

- API chỉ hiển thị file có `status = "done"` hoặc `"approved"`
- Không cần role admin, chỉ cần authenticated
- File được sắp xếp theo thời gian tạo mặc định (mới nhất trước)

---

# API Chat Regulation

Base path: `/user/regulation/chat`

## 1. Tạo Conversation Mới

```
POST /user/regulation/chat/conversations?title={title}
```

Response: `ConversationItem`

---

## 2. Lấy Danh Sách Conversations

```
GET /user/regulation/chat/conversations?cursorNext={uuid}
```

Response: `ListConversationsResponse` với cursor pagination

---

## 3. Lấy Conversation Active

```
GET /user/regulation/chat/conversations/active
```

Response: `ConversationItem` hoặc `204 No Content`

---

## 4. Set Conversation Active

```
POST /user/regulation/chat/conversations/{conversationId}/active
```

---

## 5. Lấy Messages của Conversation

```
GET /user/regulation/chat/conversations/{conversationId}/messages?cursorNext={uuid}
```

Response: `ListMessagesResponse` với cursor pagination

---

## 6. Xóa Conversation

```
DELETE /user/regulation/chat/conversations/{conversationId}
```

Response: `204 No Content`
