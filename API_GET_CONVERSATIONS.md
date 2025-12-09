# API: Lấy danh sách Conversations

## Endpoint

```
GET /user/notebooks/{notebookId}/bot-chat/conversations
```

## Mô tả

Lấy danh sách conversations của user trong một notebook, sử dụng cursor-based pagination. Conversations được sắp xếp theo `createdAt` DESC (mới nhất trước).

## Authentication

- **Required**: Yes
- **Type**: Bearer Token (JWT)
- Headers: `Authorization: Bearer <token>`

## Path Parameters

| Tên          | Kiểu | Bắt buộc | Mô tả           |
| ------------ | ---- | -------- | --------------- |
| `notebookId` | UUID | ✅       | ID của notebook |

## Query Parameters

| Tên          | Kiểu | Bắt buộc | Mô tả                                                                                 |
| ------------ | ---- | -------- | ------------------------------------------------------------------------------------- |
| `cursorNext` | UUID | ❌       | UUID của conversation cũ nhất từ lần load trước. Bỏ qua nếu đây là lần load đầu tiên. |

## Request Example

### Lần load đầu tiên (không có cursor)

```bash
GET /user/notebooks/95f69db9-e3e4-45d9-83ed-fe8d0cda70ba/bot-chat/conversations
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Load thêm (có cursor)

```bash
GET /user/notebooks/95f69db9-e3e4-45d9-83ed-fe8d0cda70ba/bot-chat/conversations?cursorNext=123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Response

### Success Response (200 OK)

```json
{
  "conversations": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "title": "Chat về Spring Boot",
      "notebookId": "95f69db9-e3e4-45d9-83ed-fe8d0cda70ba",
      "createdAt": "2025-12-07T10:30:00+00:00",
      "updatedAt": null,
      "firstMessage": "Xin chào, tôi muốn hỏi về Spring Boot",
      "totalMessages": 5
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "title": "Hỏi về RAG",
      "notebookId": "95f69db9-e3e4-45d9-83ed-fe8d0cda70ba",
      "createdAt": "2025-12-07T09:15:00+00:00",
      "updatedAt": null,
      "firstMessage": "RAG là gì?",
      "totalMessages": 3
    }
  ],
  "cursorNext": "223e4567-e89b-12d3-a456-426614174001",
  "hasMore": true
}
```

### Response Fields

| Tên             | Kiểu                      | Mô tả                                                                                                                                        |
| --------------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `conversations` | Array\<ConversationItem\> | Danh sách conversations (tối đa 10 items)                                                                                                    |
| `cursorNext`    | String (UUID) \| null     | UUID của conversation cũ nhất trong response. Dùng giá trị này cho `cursorNext` ở lần load tiếp theo. `null` nếu không còn conversation nào. |
| `hasMore`       | Boolean                   | `true` nếu còn conversation cũ hơn, `false` nếu đã hết.                                                                                      |

### ConversationItem Fields

| Tên             | Kiểu                      | Mô tả                                                                                        |
| --------------- | ------------------------- | -------------------------------------------------------------------------------------------- |
| `id`            | String (UUID)             | ID của conversation                                                                          |
| `title`         | String                    | Tiêu đề conversation                                                                         |
| `notebookId`    | String (UUID)             | ID của notebook                                                                              |
| `createdAt`     | String (ISO 8601)         | Thời gian tạo (format: `2025-12-07T10:30:00+00:00`)                                          |
| `updatedAt`     | String (ISO 8601) \| null | Thời gian cập nhật (hiện tại luôn `null`)                                                    |
| `firstMessage`  | String \| null            | Nội dung tin nhắn đầu tiên trong conversation. `null` nếu conversation chưa có tin nhắn nào. |
| `totalMessages` | Number (Long)             | Tổng số tin nhắn trong conversation. `0` nếu conversation chưa có tin nhắn nào.              |

## Error Responses

### 401 Unauthorized

**Nguyên nhân**: User chưa đăng nhập hoặc token không hợp lệ.

```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

**Xử lý FE**:

- Redirect về trang login
- Hoặc refresh token và thử lại

---

### 500 Internal Server Error

**Nguyên nhân**:

- Lỗi database
- Notebook không tồn tại
- Lỗi hệ thống khác

```json
{
  "status": 500,
  "message": "JDBC exception executing SQL [...]",
  "timestamp": "2025-12-07T10:30:00.000Z"
}
```

**Xử lý FE**:

- Hiển thị thông báo lỗi cho user
- Log lỗi để debug
- Có thể retry sau vài giây

---

### 400 Bad Request

**Nguyên nhân**:

- `notebookId` không đúng format UUID
- `cursorNext` không đúng format UUID (nếu có)

```json
{
  "status": 400,
  "message": "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'",
  "timestamp": "2025-12-07T10:30:00.000Z"
}
```

**Xử lý FE**:

- Validate UUID format trước khi gọi API
- Hiển thị lỗi validation cho user

---

## Cách sử dụng Cursor Pagination

### Flow hoàn chỉnh:

1. **Lần load đầu tiên**: Không gửi `cursorNext`

   ```javascript
   GET / user / notebooks / { notebookId } / bot - chat / conversations;
   ```

2. **Lưu `cursorNext` từ response**:

   ```javascript
   const response = await fetch(...);
   const data = await response.json();
   const nextCursor = data.cursorNext; // "223e4567-e89b-12d3-a456-426614174001"
   ```

3. **Load thêm**: Dùng `cursorNext` từ lần trước

   ```javascript
   GET /user/notebooks/{notebookId}/bot-chat/conversations?cursorNext={nextCursor}
   ```

4. **Kiểm tra `hasMore`**:
   - Nếu `hasMore === false`: Không còn conversation nào, ẩn nút "Load more"
   - Nếu `hasMore === true`: Còn conversation, hiển thị nút "Load more"

### Example Code (JavaScript/TypeScript)

```typescript
interface ConversationItem {
  id: string;
  title: string;
  notebookId: string;
  createdAt: string;
  updatedAt: string | null;
  firstMessage: string | null;
  totalMessages: number;
}

interface ListConversationsResponse {
  conversations: ConversationItem[];
  cursorNext: string | null;
  hasMore: boolean;
}

async function loadConversations(
  notebookId: string,
  cursorNext?: string
): Promise<ListConversationsResponse> {
  const url = new URL(
    `/user/notebooks/${notebookId}/bot-chat/conversations`,
    API_BASE_URL
  );

  if (cursorNext) {
    url.searchParams.set("cursorNext", cursorNext);
  }

  const response = await fetch(url.toString(), {
    method: "GET",
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      // Redirect to login
      window.location.href = "/login";
      throw new Error("Unauthorized");
    }
    throw new Error(`API Error: ${response.status}`);
  }

  return response.json();
}

// Sử dụng
let allConversations: ConversationItem[] = [];
let cursorNext: string | null = null;
let hasMore = true;

async function loadMore() {
  if (!hasMore) return;

  const response = await loadConversations(
    NOTEBOOK_ID,
    cursorNext || undefined
  );

  allConversations = [...allConversations, ...response.conversations];
  cursorNext = response.cursorNext;
  hasMore = response.hasMore;

  // Update UI
  renderConversations(allConversations);

  if (!hasMore) {
    hideLoadMoreButton();
  }
}

// Load lần đầu
loadMore();
```

## Lưu ý

1. **Limit cố định**: Mỗi lần load trả về tối đa **10 conversations**
2. **Order**: Conversations được sắp xếp theo `createdAt DESC` (mới nhất trước)
3. **Cursor**: `cursorNext` là UUID của conversation **cũ nhất** trong response hiện tại
4. **Empty list**: Nếu không có conversation nào, `conversations` sẽ là mảng rỗng `[]`, `cursorNext` là `null`, `hasMore` là `false`
5. **UpdatedAt**: Hiện tại `updatedAt` luôn là `null` trong response (có thể thay đổi trong tương lai)
6. **FirstMessage**: Tin nhắn đầu tiên được lấy theo `createdAt ASC` (tin nhắn cũ nhất). `null` nếu conversation chưa có tin nhắn nào
7. **TotalMessages**: Tổng số tin nhắn bao gồm cả tin nhắn của user và bot. `0` nếu conversation chưa có tin nhắn nào

## Testing với cURL

```bash
# Lần load đầu tiên
curl -X GET \
  'http://localhost:8386/user/notebooks/95f69db9-e3e4-45d9-83ed-fe8d0cda70ba/bot-chat/conversations' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'accept: */*'

# Load thêm (có cursor)
curl -X GET \
  'http://localhost:8386/user/notebooks/95f69db9-e3e4-45d9-83ed-fe8d0cda70ba/bot-chat/conversations?cursorNext=123e4567-e89b-12d3-a456-426614174000' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'accept: */*'
```
