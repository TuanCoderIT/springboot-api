# API Hướng dẫn - Lấy Lịch sử Chat với Bot

Tài liệu này hướng dẫn cách sử dụng API để lấy lịch sử chat với bot sử dụng cursor pagination.

## Base URL

```
/user/notebooks/{notebookId}/bot-chat/history
```

## Authentication

API sử dụng **Cookie-based authentication**. Token được lưu trong cookie `AUTH-TOKEN` sau khi user đăng nhập.

**Lưu ý quan trọng:**
- Frontend không cần gửi token trong header
- Browser sẽ tự động gửi cookie `AUTH-TOKEN` trong mọi request
- Đảm bảo `credentials: 'include'` khi gọi API từ frontend

---

## Get Chat History (Lấy lịch sử chat với bot)

Lấy lịch sử các đoạn chat trước đó với bot. Sử dụng cursor pagination để load thêm các tin nhắn cũ hơn khi user lướt lên.

### Endpoint

```
GET /user/notebooks/{notebookId}/bot-chat/history
```

### Path Parameters

| Tên        | Kiểu | Mô tả           |
| ---------- | ---- | --------------- |
| notebookId | UUID | ID của notebook |

### Query Parameters

| Tên        | Kiểu   | Bắt buộc | Mô tả                                                                 |
| ---------- | ------ | -------- | --------------------------------------------------------------------- |
| cursorNext | String | No       | UUID của message cũ nhất từ lần load trước. Dùng để lấy các message cũ hơn |
| limit      | Number | No       | Số lượng message muốn lấy (mặc định: 20, tối đa: 50)                 |

### Request Headers

Không cần gửi header `Authorization`. Cookie sẽ được gửi tự động.

### Response (200 OK)

```typescript
interface ChatHistoryResponse {
  messages: RagQueryResponse[];
  cursorNext: string | null; // UUID của message cũ nhất trong response, dùng để load more
  hasMore: boolean; // Còn message cũ hơn không
}

interface RagQueryResponse {
  id: string;
  question: string;
  answer: string;
  sourceChunks: {
    // JSONB chứa thông tin về các chunks được sử dụng để trả lời
    // Có thể chứa:
    // - file_id: UUID
    // - file_name: string
    // - file_type: string
    // - chunk_index: number
    // - metadata: object (offset metadata)
    // - score: number (cosine similarity)
    // - bounding_box: object (nếu OCR ảnh)
    // - ocr_text: string (text OCR của image chunk)
    [key: string]: any;
  } | null;
  latencyMs: number | null;
  createdAt: string; // ISO 8601
}
```

### Ví dụ Response

#### Lần đầu load (không có cursor)

```json
{
  "messages": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "question": "Tóm tắt nội dung file này",
      "answer": "File này nói về...",
      "sourceChunks": {
        "file_id": "f4a552b4-17a4-40b4-a602-3d1d6a2b3c2b",
        "file_name": "document.pdf",
        "file_type": "application/pdf",
        "chunk_index": 0,
        "score": 0.85,
        "metadata": {
          "page": 1
        }
      },
      "latencyMs": 1250,
      "createdAt": "2025-12-05T10:30:00.000Z"
    },
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "question": "Có những điểm chính nào?",
      "answer": "Các điểm chính bao gồm...",
      "sourceChunks": {
        "file_id": "f4a552b4-17a4-40b4-a602-3d1d6a2b3c2b",
        "file_name": "document.pdf",
        "file_type": "application/pdf",
        "chunk_index": 1,
        "score": 0.78
      },
      "latencyMs": 980,
      "createdAt": "2025-12-05T10:25:00.000Z"
    }
  ],
  "cursorNext": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "hasMore": true
}
```

#### Load more (có cursor)

```json
{
  "messages": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "question": "Giải thích chi tiết hơn",
      "answer": "Chi tiết như sau...",
      "sourceChunks": {
        "file_id": "f4a552b4-17a4-40b4-a602-3d1d6a2b3c2b",
        "file_name": "document.pdf",
        "chunk_index": 2,
        "score": 0.72
      },
      "latencyMs": 1100,
      "createdAt": "2025-12-05T10:20:00.000Z"
    }
  ],
  "cursorNext": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "hasMore": false
}
```

---

## Cách hoạt động (Cursor Pagination)

### 1. Lần đầu load (Initial Load)

**Request:**
```
GET /user/notebooks/{notebookId}/bot-chat/history?limit=20
```

**Response:**
- Trả về 20 message **mới nhất** (sắp xếp theo `createdAt DESC`)
- `cursorNext` = ID của message **cũ nhất** trong response
- `hasMore` = `true` nếu còn message cũ hơn

**Ví dụ:**
```
Messages: [Mới nhất] -> [Cũ hơn] -> [Cũ nhất trong response]
         Message 1
         Message 2
         ...
         Message 20 (cursorNext = Message 20's ID)
```

### 2. Load more (Scroll up để xem tin nhắn cũ)

**Request:**
```
GET /user/notebooks/{notebookId}/bot-chat/history?cursor_next={uuid}&limit=20
```

**Response:**
- Trả về 20 message **cũ hơn** cursor
- `cursorNext` = ID của message **cũ nhất** trong response mới
- `hasMore` = `true` nếu còn message cũ hơn

**Ví dụ:**
```
Lần 1: Message 1-20 (cursorNext = Message 20's ID)
Lần 2: Message 21-40 (cursorNext = Message 40's ID) - cũ hơn Message 20
Lần 3: Message 41-60 (cursorNext = Message 60's ID) - cũ hơn Message 40
```

### 3. Khi không còn message

**Response:**
```json
{
  "messages": [...],
  "cursorNext": "last-message-id",
  "hasMore": false
}
```

Khi `hasMore = false`, không còn message cũ hơn để load.

---

## Ví dụ (TypeScript/React)

### Basic Usage

```typescript
interface RagQueryResponse {
  id: string;
  question: string;
  answer: string;
  sourceChunks: {
    file_id?: string;
    file_name?: string;
    file_type?: string;
    chunk_index?: number;
    metadata?: any;
    score?: number;
    bounding_box?: any;
    ocr_text?: string;
    [key: string]: any;
  } | null;
  latencyMs: number | null;
  createdAt: string;
}

interface ChatHistoryResponse {
  messages: RagQueryResponse[];
  cursorNext: string | null;
  hasMore: boolean;
}

async function getChatHistory(
  notebookId: string,
  cursorNext?: string,
  limit: number = 20
): Promise<ChatHistoryResponse> {
  const params = new URLSearchParams();
  if (cursorNext) {
    params.append("cursor_next", cursorNext);
  }
  params.append("limit", limit.toString());

  const response = await fetch(
    `/user/notebooks/${notebookId}/bot-chat/history?${params.toString()}`,
    {
      credentials: "include", // ⭐ QUAN TRỌNG: Gửi cookie tự động
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to fetch chat history");
  }

  return response.json();
}

// Sử dụng
// Lần đầu
const firstPage = await getChatHistory(notebookId);
console.log("Messages:", firstPage.messages);
console.log("Has more:", firstPage.hasMore);

// Load more
if (firstPage.hasMore && firstPage.cursorNext) {
  const nextPage = await getChatHistory(notebookId, firstPage.cursorNext);
  console.log("More messages:", nextPage.messages);
}
```

### React Component với Infinite Scroll

```typescript
import React, { useState, useEffect, useCallback, useRef } from "react";

interface BotChatHistoryProps {
  notebookId: string;
}

const BotChatHistory: React.FC<BotChatHistoryProps> = ({ notebookId }) => {
  const [messages, setMessages] = useState<RagQueryResponse[]>([]);
  const [cursorNext, setCursorNext] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Load initial messages
  useEffect(() => {
    const loadInitial = async () => {
      try {
        setInitialLoading(true);
        setError(null);
        const response = await getChatHistory(notebookId);
        setMessages(response.messages);
        setCursorNext(response.cursorNext);
        setHasMore(response.hasMore);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load chat history");
      } finally {
        setInitialLoading(false);
      }
    };

    loadInitial();
  }, [notebookId]);

  // Load more messages (khi scroll lên)
  const loadMore = useCallback(async () => {
    if (loading || !hasMore || !cursorNext) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getChatHistory(notebookId, cursorNext);

      // Thêm messages mới vào đầu danh sách (vì là tin nhắn cũ hơn)
      setMessages((prev) => [...response.messages, ...prev]);
      setCursorNext(response.cursorNext);
      setHasMore(response.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load more messages");
    } finally {
      setLoading(false);
    }
  }, [notebookId, cursorNext, hasMore, loading]);

  // Handle scroll để load more khi scroll lên đầu
  const handleScroll = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container) return;

    // Khi scroll gần đến đầu (top 100px)
    if (container.scrollTop < 100 && hasMore && !loading) {
      loadMore();
    }
  }, [hasMore, loading, loadMore]);

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (container) {
      container.addEventListener("scroll", handleScroll);
      return () => container.removeEventListener("scroll", handleScroll);
    }
  }, [handleScroll]);

  if (initialLoading) {
    return <div>Loading chat history...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div
      ref={scrollContainerRef}
      style={{
        height: "600px",
        overflowY: "auto",
        padding: "20px",
      }}
    >
      {loading && (
        <div style={{ textAlign: "center", padding: "10px", color: "#666" }}>
          Loading older messages...
        </div>
      )}

      {messages.map((message) => (
        <div
          key={message.id}
          style={{
            marginBottom: "20px",
            padding: "15px",
            border: "1px solid #ddd",
            borderRadius: "8px",
          }}
        >
          <div style={{ marginBottom: "10px" }}>
            <strong>Question:</strong>
            <p style={{ marginTop: "5px", color: "#333" }}>{message.question}</p>
          </div>

          <div style={{ marginBottom: "10px" }}>
            <strong>Answer:</strong>
            <p style={{ marginTop: "5px", color: "#555" }}>{message.answer}</p>
          </div>

          {message.sourceChunks && (
            <div
              style={{
                marginTop: "10px",
                padding: "10px",
                backgroundColor: "#f5f5f5",
                borderRadius: "4px",
                fontSize: "12px",
              }}
            >
              <strong>Source:</strong>
              <ul style={{ marginTop: "5px", paddingLeft: "20px" }}>
                {message.sourceChunks.file_name && (
                  <li>File: {message.sourceChunks.file_name}</li>
                )}
                {message.sourceChunks.chunk_index !== undefined && (
                  <li>Chunk Index: {message.sourceChunks.chunk_index}</li>
                )}
                {message.sourceChunks.score !== undefined && (
                  <li>Score: {message.sourceChunks.score.toFixed(2)}</li>
                )}
              </ul>
            </div>
          )}

          <div style={{ marginTop: "10px", fontSize: "12px", color: "#999" }}>
            {new Date(message.createdAt).toLocaleString()}
            {message.latencyMs && ` • ${message.latencyMs}ms`}
          </div>
        </div>
      ))}

      {!hasMore && messages.length > 0 && (
        <div style={{ textAlign: "center", padding: "20px", color: "#999" }}>
          No more messages
        </div>
      )}

      {messages.length === 0 && (
        <div style={{ textAlign: "center", padding: "20px", color: "#999" }}>
          No chat history yet
        </div>
      )}
    </div>
  );
};

export default BotChatHistory;
```

### React Hook cho Chat History

```typescript
import { useState, useEffect, useCallback } from "react";

interface UseChatHistoryReturn {
  messages: RagQueryResponse[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
  loadMore: () => Promise<void>;
  refresh: () => Promise<void>;
}

function useChatHistory(
  notebookId: string,
  limit: number = 20
): UseChatHistoryReturn {
  const [messages, setMessages] = useState<RagQueryResponse[]>([]);
  const [cursorNext, setCursorNext] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadInitial = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await getChatHistory(notebookId, undefined, limit);
      setMessages(response.messages);
      setCursorNext(response.cursorNext);
      setHasMore(response.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load chat history");
    } finally {
      setLoading(false);
    }
  }, [notebookId, limit]);

  const loadMore = useCallback(async () => {
    if (loading || !hasMore || !cursorNext) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getChatHistory(notebookId, cursorNext, limit);

      // Thêm messages mới vào đầu (tin nhắn cũ hơn)
      setMessages((prev) => [...response.messages, ...prev]);
      setCursorNext(response.cursorNext);
      setHasMore(response.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load more messages");
    } finally {
      setLoading(false);
    }
  }, [notebookId, cursorNext, hasMore, loading, limit]);

  useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  return {
    messages,
    loading,
    error,
    hasMore,
    loadMore,
    refresh: loadInitial,
  };
}

// Sử dụng
function ChatHistoryComponent({ notebookId }: { notebookId: string }) {
  const { messages, loading, error, hasMore, loadMore } = useChatHistory(notebookId);

  return (
    <div>
      {error && <div>Error: {error}</div>}
      {messages.map((msg) => (
        <div key={msg.id}>{msg.question}</div>
      ))}
      {hasMore && (
        <button onClick={loadMore} disabled={loading}>
          {loading ? "Loading..." : "Load More"}
        </button>
      )}
    </div>
  );
}
```

---

## Error Handling

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "User chưa đăng nhập.",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

**Nguyên nhân:**
- Cookie `AUTH-TOKEN` không tồn tại
- Cookie đã hết hạn
- Token không hợp lệ

**Xử lý:**
- Redirect user đến trang login
- Xóa cookie cũ nếu có

### 400 Bad Request

#### Case 1: Cursor không hợp lệ

```json
{
  "status": 400,
  "message": "Cursor không hợp lệ",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

**Nguyên nhân:**
- `cursorNext` không phải UUID hợp lệ
- `cursorNext` không tồn tại trong database

**Xử lý:**
- Reset về load initial (bỏ cursor)
- Hoặc hiển thị thông báo lỗi

#### Case 2: Chưa tham gia notebook

```json
{
  "status": 400,
  "message": "Bạn chưa tham gia nhóm này",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Notebook không tồn tại",
  "timestamp": "2025-12-05T02:14:27.573824"
}
```

### Ví dụ Error Handling đầy đủ

```typescript
async function getChatHistoryWithErrorHandling(
  notebookId: string,
  cursorNext?: string,
  limit: number = 20
): Promise<ChatHistoryResponse> {
  try {
    const params = new URLSearchParams();
    if (cursorNext) {
      params.append("cursor_next", cursorNext);
    }
    params.append("limit", limit.toString());

    const response = await fetch(
      `/user/notebooks/${notebookId}/bot-chat/history?${params.toString()}`,
      {
        credentials: "include", // ⭐
      }
    );

    if (!response.ok) {
      const error = await response.json();

      if (response.status === 401) {
        // Unauthorized - redirect to login
        window.location.href = "/login";
        throw new Error("Phiên đăng nhập đã hết hạn");
      } else if (response.status === 400) {
        // Bad Request
        if (error.message?.includes("Cursor")) {
          // Cursor không hợp lệ - reset về initial load
          return getChatHistoryWithErrorHandling(notebookId, undefined, limit);
        }
        throw new Error(error.message || "Bad request");
      } else if (response.status === 404) {
        throw new Error("Notebook không tồn tại");
      } else {
        throw new Error(`Server error: ${error.message}`);
      }
    }

    return response.json();
  } catch (error) {
    console.error("Error fetching chat history:", error);
    throw error;
  }
}
```

---

## Best Practices

### 1. Lưu cursor trong state

```typescript
const [cursorNext, setCursorNext] = useState<string | null>(null);

// Sau mỗi lần load
setCursorNext(response.cursorNext);
```

### 2. Kiểm tra hasMore trước khi load

```typescript
if (!hasMore || !cursorNext) {
  return; // Không load nữa
}
```

### 3. Prevent duplicate requests

```typescript
const [loading, setLoading] = useState(false);

const loadMore = async () => {
  if (loading) return; // Đang load thì không load thêm

  setLoading(true);
  try {
    // ... load logic
  } finally {
    setLoading(false);
  }
};
```

### 4. Handle edge cases

```typescript
// Nếu cursorNext là null nhưng hasMore = true (edge case)
if (hasMore && !cursorNext) {
  console.warn("hasMore is true but cursorNext is null");
  setHasMore(false);
}
```

### 5. Optimistic UI Update

```typescript
// Lưu state trước khi load
const previousMessages = [...messages];
const previousCursor = cursorNext;

// Load more
try {
  const response = await getChatHistory(notebookId, cursorNext);
  setMessages([...response.messages, ...messages]);
  setCursorNext(response.cursorNext);
  setHasMore(response.hasMore);
} catch (error) {
  // Rollback nếu có lỗi
  setMessages(previousMessages);
  setCursorNext(previousCursor);
  alert("Không thể tải thêm tin nhắn. Vui lòng thử lại.");
}
```

---

## Source Chunks Structure

`sourceChunks` là JSONB object có thể chứa các thông tin sau:

```typescript
interface SourceChunks {
  // Thông tin file
  file_id?: string; // UUID của file
  file_name?: string; // Tên file gốc
  file_type?: string; // MIME type (application/pdf, image/png, ...)

  // Thông tin chunk
  chunk_index?: number; // Index của chunk trong file

  // Metadata
  metadata?: {
    page?: number; // Trang (nếu là PDF)
    offset?: number; // Offset trong file
    [key: string]: any;
  };

  // Similarity score
  score?: number; // Cosine similarity score (0-1)

  // OCR (nếu là ảnh)
  bounding_box?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  ocr_text?: string; // Text được OCR từ ảnh

  // Các field khác
  [key: string]: any;
}
```

### Ví dụ sử dụng sourceChunks

```typescript
function renderSourceInfo(sourceChunks: any) {
  if (!sourceChunks) return null;

  return (
    <div className="source-info">
      {sourceChunks.file_name && (
        <div>📄 File: {sourceChunks.file_name}</div>
      )}
      {sourceChunks.chunk_index !== undefined && (
        <div>📍 Chunk: {sourceChunks.chunk_index}</div>
      )}
      {sourceChunks.score !== undefined && (
        <div>🎯 Relevance: {(sourceChunks.score * 100).toFixed(1)}%</div>
      )}
      {sourceChunks.metadata?.page && (
        <div>📄 Page: {sourceChunks.metadata.page}</div>
      )}
    </div>
  );
}
```

---

## Tóm tắt

- **Endpoint**: `GET /user/notebooks/{notebookId}/bot-chat/history`
- **Authentication**: Cookie `AUTH-TOKEN` (tự động gửi với `credentials: 'include'`)
- **Pagination**: Cursor-based (dùng `cursorNext` để load message cũ hơn)
- **Response**: Danh sách messages, `cursorNext`, và `hasMore`
- **Source Chunks**: JSONB chứa thông tin về file, chunk, score, OCR, etc.

---

## Flow Diagram

```
1. Initial Load
   Request: GET /history?limit=20
   Response: Messages 1-20 (mới nhất), cursorNext = Message 20's ID

2. User scrolls up
   Request: GET /history?cursor_next={Message 20's ID}&limit=20
   Response: Messages 21-40 (cũ hơn), cursorNext = Message 40's ID

3. User scrolls up again
   Request: GET /history?cursor_next={Message 40's ID}&limit=20
   Response: Messages 41-60 (cũ hơn), cursorNext = Message 60's ID, hasMore = false

4. No more messages
   hasMore = false → Không load thêm
```

---

## Lưu ý quan trọng

1. ⚠️ **Luôn dùng `credentials: 'include'`** khi gọi API
2. ⚠️ **Kiểm tra `hasMore`** trước khi gọi load more
3. ⚠️ **Prevent duplicate requests** bằng loading state
4. ⚠️ **Xử lý cursor không hợp lệ** bằng cách reset về initial load
5. ⚠️ **Messages được sắp xếp DESC** (mới nhất trước), nhưng khi load more sẽ lấy message cũ hơn

