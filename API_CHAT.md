# API: Gửi tin nhắn Chat với Bot

## Endpoint

```
POST /user/notebooks/{notebookId}/bot-chat/conversations/{conversationId}/chat
```

## Mô tả

Gửi tin nhắn chat với bot AI. Hỗ trợ nhiều chế độ: RAG (tài liệu nội bộ), WEB (tìm kiếm web), HYBRID (kết hợp), LLM_ONLY (chỉ LLM), và AUTO (tự động chọn mode).

## Authentication

- **Required**: Yes
- **Type**: Bearer Token (JWT)
- Headers: `Authorization: Bearer <token>`

## Path Parameters

| Tên             | Kiểu | Bắt buộc | Mô tả                    |
| --------------- | ---- | -------- | ------------------------- |
| `notebookId`    | UUID | ✅       | ID của notebook           |
| `conversationId` | UUID | ✅       | ID của conversation       |

## Request

### Content-Type

```
multipart/form-data
```

### Form Data

| Tên      | Kiểu              | Bắt buộc | Mô tả                                                      |
| -------- | ----------------- | -------- | ---------------------------------------------------------- |
| `request` | String (JSON)     | ✅       | JSON string chứa ChatRequest                               |
| `images`  | File[] (optional) | ❌       | Danh sách hình ảnh/tài liệu (jpg, jpeg, png, gif, pdf, doc, docx) |

### ChatRequest (JSON trong `request` field)

```typescript
interface ChatRequest {
  message: string;           // ✅ Bắt buộc: Nội dung câu hỏi
  modelId: string;           // ✅ Bắt buộc: UUID của LLM model
  mode: "RAG" | "WEB" | "HYBRID" | "LLM_ONLY" | "AUTO";  // ✅ Bắt buộc: Chế độ chat
  ragFileIds?: string[];     // ❌ Optional: Danh sách file IDs (chỉ dùng cho RAG và HYBRID mode)
}
```

**Lưu ý**: `conversationId` không cần gửi trong request body vì đã có trong URL path.

### ChatMode

| Mode      | Mô tả                                                                 |
| --------- | --------------------------------------------------------------------- |
| `RAG`     | Tìm kiếm trong tài liệu nội bộ (cần `ragFileIds`)                    |
| `WEB`     | Tìm kiếm trên web                                                      |
| `HYBRID`  | Kết hợp RAG + WEB (cần `ragFileIds`)                                  |
| `LLM_ONLY` | Chỉ dùng LLM, không tra cứu                                           |
| `AUTO`    | Tự động chọn mode phù hợp dựa trên nội dung câu hỏi và file đính kèm |

## Request Example

### cURL

```bash
curl -X 'POST' \
  'http://localhost:8386/user/notebooks/95f69db9-e3e4-45d9-83ed-fe8d0cda70ba/bot-chat/conversations/7c1fa5a9-7efa-42d4-a49f-9dc5a3cad5f2/chat' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: multipart/form-data' \
  -F 'request={"message":"Hãy giải thích về bài thực hành 6","modelId":"0762b5e2-3310-46ef-83fe-dbb18dc76421","mode":"HYBRID","ragFileIds":["16280415-d780-45e1-b1bc-7707d85eb2f7"]}' \
  -F 'images=@image1.jpg' \
  -F 'images=@document.pdf'
```

### JavaScript/TypeScript (FormData)

```typescript
async function sendChatMessage(
  notebookId: string,
  conversationId: string,
  message: string,
  modelId: string,
  mode: "RAG" | "WEB" | "HYBRID" | "LLM_ONLY" | "AUTO",
  ragFileIds?: string[],
  images?: File[]
): Promise<ChatResponse> {
  const formData = new FormData();

  // Tạo request object
  const request: ChatRequest = {
    message,
    modelId,
    mode,
    ragFileIds: ragFileIds || undefined,
  };

  // Thêm request JSON vào formData
  formData.append("request", JSON.stringify(request));

  // Thêm images nếu có
  if (images && images.length > 0) {
    images.forEach((image) => {
      formData.append("images", image);
    });
  }

  const response = await fetch(
    `${API_BASE_URL}/user/notebooks/${notebookId}/bot-chat/conversations/${conversationId}/chat`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${getAuthToken()}`,
        // KHÔNG set Content-Type header, browser sẽ tự động set với boundary
      },
      body: formData,
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      window.location.href = "/login";
      throw new Error("Unauthorized");
    }
    const error = await response.json();
    throw new Error(error.message || `API Error: ${response.status}`);
  }

  return response.json();
}
```

## Response

### Success Response (200 OK)

```json
{
  "id": "b37fef0b-522a-414c-8063-416484a4fc36",
  "content": "Có rất nhiều bộ sưu tập ảnh gái xinh mặc bikini quyến rũ và gợi cảm trên internet...",
  "mode": "HYBRID",
  "role": "assistant",
  "context": null,
  "createdAt": "2025-12-08T01:17:33.836461+07:00",
  "metadata": null,
  "model": {
    "id": "0762b5e2-3310-46ef-83fe-dbb18dc76421",
    "code": "gemini",
    "provider": "google"
  },
  "sources": [
    {
      "sourceType": "WEB",
      "webIndex": 1,
      "url": "https://giaitritivi.com/anh-gai-xinh-bikini-sieu-nho/",
      "title": "Tổng hợp 366+ ảnh gái xinh bikini siêu nhỏ sexy nóng bỏng",
      "snippet": "Chiêm ngưỡng trọn bộ ảnh gái xinh bikini siêu nhỏ cực gợi cảm...",
      "score": 1.00,
      "provider": "google",
      "imageUrl": "https://giaitritivi.com/wp-content/uploads/2025/09/Gai-xinh-mac-do-lot-goi-cam-nong-bong.webp",
      "favicon": null,
      "fileId": null,
      "chunkIndex": null,
      "content": null,
      "similarity": null,
      "distance": null
    },
    {
      "sourceType": "RAG",
      "fileId": "16280415-d780-45e1-b1bc-7707d85eb2f7",
      "chunkIndex": 0,
      "score": 0.95,
      "provider": "rag",
      "content": "BÀI THỰC HÀNH 6\nGV: Nguyễn Thị Minh Tâm...",
      "similarity": 0.92,
      "distance": 0.08,
      "webIndex": null,
      "url": null,
      "title": null,
      "snippet": null,
      "imageUrl": null,
      "favicon": null
    }
  ],
  "files": [
    {
      "id": "f1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "fileType": "image",
      "fileUrl": "/uploads/image.jpg",
      "mimeType": "image/jpeg",
      "fileName": "image.jpg",
      "ocrText": "Text extracted from image",
      "caption": null,
      "metadata": null
    }
  ]
}
```

## Response Fields

### ChatResponse

| Field      | Type                    | Mô tả                                                      |
| ---------- | ----------------------- | ---------------------------------------------------------- |
| `id`       | `string` (UUID)         | ID của message                                             |
| `content`  | `string`                | Nội dung trả lời (markdown format)                         |
| `mode`     | `string`                | Mode thực tế sử dụng: `"RAG"`, `"WEB"`, `"HYBRID"`, `"LLM_ONLY"` |
| `role`     | `string`                | `"assistant"` (luôn là assistant cho response này)         |
| `context`  | `object \| null`        | Context data (JSONB)                                       |
| `createdAt` | `string` (ISO 8601)     | Thời gian tạo message                                      |
| `metadata` | `object \| null`        | Metadata (JSONB)                                           |
| `model`    | `ModelResponse \| null` | Thông tin LLM model được sử dụng                           |
| `sources`  | `SourceResponse[]`      | Danh sách sources (RAG và WEB) đã được sử dụng, sort theo score giảm dần |
| `files`    | `FileResponse[]`        | Danh sách files đính kèm từ user message (nếu có)         |

### ModelResponse

| Field      | Type     | Mô tả                |
| ---------- | -------- | --------------------- |
| `id`       | `string` | ID của model          |
| `code`     | `string` | Code: `"gemini"`, `"groq"` |
| `provider` | `string` | Provider: `"google"`, `"groq"` |

### SourceResponse

| Field        | Type            | Mô tả                                                          |
| ------------ | --------------- | -------------------------------------------------------------- |
| `sourceType` | `string`        | `"RAG"` hoặc `"WEB"`                                            |
| `fileId`     | `string \| null` | UUID của file (chỉ có khi `sourceType = "RAG"`)                |
| `chunkIndex` | `number \| null` | Index của chunk (chỉ có khi `sourceType = "RAG"`)             |
| `content`    | `string \| null` | Nội dung chunk (chỉ có khi `sourceType = "RAG"`)               |
| `similarity` | `number \| null` | Similarity score (chỉ có khi `sourceType = "RAG"`)            |
| `distance`   | `number \| null` | Distance score (chỉ có khi `sourceType = "RAG"`)              |
| `webIndex`   | `number \| null` | Index trong webResults (chỉ có khi `sourceType = "WEB"`)      |
| `url`        | `string \| null` | URL của web result (chỉ có khi `sourceType = "WEB"`)           |
| `title`      | `string \| null` | Title của web result (chỉ có khi `sourceType = "WEB"`)         |
| `snippet`    | `string \| null` | Snippet của web result (chỉ có khi `sourceType = "WEB"`)      |
| `imageUrl`   | `string \| null` | URL hình ảnh từ web result (chỉ có khi `sourceType = "WEB"`)   |
| `favicon`    | `string \| null` | Favicon URL (chỉ có khi `sourceType = "WEB"`)                  |
| `score`      | `number`        | Mức độ đóng góp (0.00 - 1.00)                                 |
| `provider`   | `string`        | Provider: `"rag"`, `"web"`, `"google"`                         |

### FileResponse

| Field      | Type            | Mô tả                    |
| ---------- | --------------- | ------------------------ |
| `id`       | `string` (UUID) | ID của file              |
| `fileType` | `string`        | `"image"`, `"document"` |
| `fileUrl`  | `string`        | URL của file             |
| `mimeType` | `string`        | MIME type                |
| `fileName` | `string`        | Tên file                 |
| `ocrText`  | `string \| null` | Text từ OCR (nếu có)     |
| `caption`  | `string \| null` | Caption (nếu có)         |
| `metadata` | `object \| null` | Metadata (JSONB)         |

## Error Handling

### 401 Unauthorized

**Nguyên nhân**: Token không hợp lệ hoặc đã hết hạn.

```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Xử lý FE**:
- Redirect user đến trang login
- Clear token và refresh token trong localStorage/sessionStorage

### 400 Bad Request

**Nguyên nhân 1**: Dữ liệu request không hợp lệ (JSON parse error).

```json
{
  "status": 400,
  "message": "Dữ liệu request không hợp lệ: Unexpected token...",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Nguyên nhân 2**: File type không được hỗ trợ.

```json
{
  "status": 400,
  "message": "Chỉ chấp nhận file hình ảnh (jpg, jpeg, png, gif) và document (pdf, doc, docx). File không hợp lệ: image.bmp",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Nguyên nhân 3**: Câu hỏi là bắt buộc nhưng bị rỗng.

```json
{
  "status": 400,
  "message": "Câu hỏi là bắt buộc. Vui lòng nhập câu hỏi.",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Nguyên nhân 4**: Model ID là bắt buộc nhưng không có.

```json
{
  "status": 400,
  "message": "Model ID là bắt buộc. Vui lòng chọn model.",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Xử lý FE**:
- Validate dữ liệu trước khi gửi request
- Hiển thị thông báo lỗi cụ thể cho user
- Kiểm tra file type trước khi upload

### 404 Not Found

**Nguyên nhân**: Conversation hoặc Notebook không tồn tại.

```json
{
  "status": 404,
  "message": "Conversation not found: 7c1fa5a9-7efa-42d4-a49f-9dc5a3cad5f2",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Xử lý FE**:
- Hiển thị thông báo "Conversation không tồn tại"
- Redirect về trang danh sách conversations

### 500 Internal Server Error

**Nguyên nhân**: Lỗi server (LLM timeout, database error, etc.).

```json
{
  "status": 500,
  "message": "Internal server error",
  "timestamp": "2025-12-08T01:17:33.836461+07:00"
}
```

**Xử lý FE**:
- Hiển thị thông báo lỗi cho user
- Có thể retry sau vài giây
- Log error để debug

## Example Code (React)

```typescript
import { useState } from "react";

interface ChatRequest {
  message: string;
  modelId: string;
  mode: "RAG" | "WEB" | "HYBRID" | "LLM_ONLY" | "AUTO";
  ragFileIds?: string[];
}

interface ChatResponse {
  id: string;
  content: string;
  mode: string;
  role: string;
  context: any | null;
  createdAt: string;
  metadata: any | null;
  model: {
    id: string;
    code: string;
    provider: string;
  } | null;
  sources: SourceResponse[];
  files: FileResponse[];
}

function ChatInput({
  notebookId,
  conversationId,
  modelId,
  mode,
  ragFileIds,
}: Props) {
  const [message, setMessage] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!message.trim()) {
      setError("Vui lòng nhập câu hỏi");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const formData = new FormData();

      const request: ChatRequest = {
        message: message.trim(),
        modelId,
        mode,
        ragFileIds: ragFileIds || undefined,
      };

      formData.append("request", JSON.stringify(request));

      if (images.length > 0) {
        images.forEach((image) => {
          formData.append("images", image);
        });
      }

      const response = await fetch(
        `${API_BASE_URL}/user/notebooks/${notebookId}/bot-chat/conversations/${conversationId}/chat`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${getAuthToken()}`,
          },
          body: formData,
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          window.location.href = "/login";
          return;
        }
        const errorData = await response.json();
        throw new Error(errorData.message || `API Error: ${response.status}`);
      }

      const data: ChatResponse = await response.json();

      // Xử lý response (thêm vào danh sách messages, etc.)
      onMessageSent(data);

      // Reset form
      setMessage("");
      setImages([]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra");
      console.error("Error sending message:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      
      // Validate file types
      const validTypes = [".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx"];
      const invalidFiles = files.filter(
        (file) => !validTypes.some((type) => file.name.toLowerCase().endsWith(type))
      );

      if (invalidFiles.length > 0) {
        setError(
          `File không hợp lệ: ${invalidFiles.map((f) => f.name).join(", ")}. Chỉ chấp nhận: ${validTypes.join(", ")}`
        );
        return;
      }

      setImages(files);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Nhập câu hỏi của bạn..."
        disabled={loading}
      />

      <input
        type="file"
        multiple
        accept=".jpg,.jpeg,.png,.gif,.pdf,.doc,.docx"
        onChange={handleImageChange}
        disabled={loading}
      />

      {images.length > 0 && (
        <div>
          <p>Đã chọn {images.length} file:</p>
          <ul>
            {images.map((file, index) => (
              <li key={index}>{file.name}</li>
            ))}
          </ul>
        </div>
      )}

      {error && <div className="error">{error}</div>}

      <button type="submit" disabled={loading || !message.trim()}>
        {loading ? "Đang gửi..." : "Gửi"}
      </button>
    </form>
  );
}
```

## Lưu ý

1. **Content-Type**: Không set `Content-Type` header khi dùng `FormData`, browser sẽ tự động set với boundary
2. **File validation**: Validate file type ở frontend trước khi gửi để tránh lỗi 400
3. **Message bắt buộc**: `message` không được rỗng
4. **Model ID bắt buộc**: `modelId` phải là UUID hợp lệ của một LLM model
5. **Mode bắt buộc**: `mode` phải là một trong: `"RAG"`, `"WEB"`, `"HYBRID"`, `"LLM_ONLY"`, `"AUTO"`
6. **ragFileIds**: Chỉ cần khi `mode = "RAG"` hoặc `mode = "HYBRID"`
7. **AUTO mode**: Khi dùng `AUTO`, hệ thống sẽ tự động chọn mode phù hợp dựa trên nội dung câu hỏi và file đính kèm
8. **Response time**: API có thể mất vài giây để xử lý (OCR, RAG search, Web search, LLM call)
9. **Sources**: Chỉ trả về sources thực sự được sử dụng để tạo câu trả lời, đã được sort theo score giảm dần
10. **Content format**: `content` trong response là markdown, cần render bằng markdown renderer

