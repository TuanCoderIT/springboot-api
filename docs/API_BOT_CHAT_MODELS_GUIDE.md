# API: Lấy Danh Sách LLM Models

## URL

**GET** `/user/notebooks/{notebookId}/bot-chat/models`

---

## Đầu Vào (Input)

### URL Parameters

| Parameter    | Type          | Required | Description     |
| ------------ | ------------- | -------- | --------------- |
| `notebookId` | UUID (String) | ✅ Yes   | ID của notebook |

### Headers

| Header          | Type   | Required | Description                        |
| --------------- | ------ | -------- | ---------------------------------- |
| `Authorization` | String | ✅ Yes   | Bearer token: `Bearer {jwt_token}` |

### Request Body

Không có.

---

## Đầu Ra (Output)

### Success Response (200 OK)

**Response Body:** Array of LlmModel

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "gemini-2.5-flash",
    "provider": "google",
    "displayName": "Gemini 2.5 Flash",
    "isActive": true,
    "isDefault": true,
    "metadata": {
      "maxTokens": 8192,
      "temperature": 0.7
    },
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

**Response Structure:**

```typescript
interface LlmModel {
  id: string; // UUID
  code: string; // Mã định danh model
  provider: string; // Nhà cung cấp (google, openai, ...)
  displayName: string; // Tên hiển thị
  isActive: boolean; // Luôn là true (chỉ trả về models active)
  isDefault: boolean; // Có phải model mặc định không
  metadata: {
    // Metadata bổ sung (có thể null)
    [key: string]: any;
  } | null;
  createdAt: string; // ISO 8601 format
}
```

**Sắp xếp:**

- Model mặc định (`isDefault: true`) ở đầu tiên
- Các model khác sắp xếp theo `displayName` A-Z

**Empty Response:**
Nếu không có model nào active, trả về mảng rỗng: `[]`

---

## Các Lỗi Có Thể Xảy Ra

### 1. 401 Unauthorized

**Status Code:** `401`

**Response:**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "User chưa đăng nhập.",
  "path": "/user/notebooks/{notebookId}/bot-chat/models"
}
```

**Nguyên nhân:**

- Chưa đăng nhập
- Token không hợp lệ hoặc đã hết hạn

---

### 2. 400 Bad Request

**Status Code:** `400`

**Response:**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid UUID format",
  "path": "/user/notebooks/{notebookId}/bot-chat/models"
}
```

**Nguyên nhân:**

- `notebookId` không đúng format UUID

---

### 3. 500 Internal Server Error

**Status Code:** `500`

**Response:**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/user/notebooks/{notebookId}/bot-chat/models"
}
```

**Nguyên nhân:**

- Lỗi database
- Lỗi server

---

## Lưu Ý

- Chỉ trả về models có `isActive: true`
- Model mặc định (`isDefault: true`) luôn ở đầu danh sách
- `metadata` có thể là `null` hoặc object
