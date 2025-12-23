# API: Thêm Video YouTube vào Chapter

## Tổng quan

Cho phép giảng viên thêm video YouTube vào chapter. **API response ngay lập tức** (~1 giây), tất cả xử lý nặng chạy nền:

```
API call → Tạo record → Response ngay ✅
                ↘ Async: Trích xuất phụ đề → Chunk → Embedding (30-60s)
```

---

## Endpoint

```
POST /lecturer/chapters/{chapterId}/items/youtube
```

---

## Request

### Headers

```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Path Parameters

| Tên         | Kiểu | Bắt buộc | Mô tả          |
| ----------- | ---- | -------- | -------------- |
| `chapterId` | UUID | ✅       | ID của chapter |

### Request Body

```json
{
  "youtubeUrl": "https://youtu.be/kOiZpMpBpAU",
  "title": "Bài giảng Thuật toán",
  "description": "Video giới thiệu về giải thuật"
}
```

| Field         | Kiểu   | Bắt buộc | Mô tả                                 |
| ------------- | ------ | -------- | ------------------------------------- |
| `youtubeUrl`  | string | ✅       | URL video YouTube                     |
| `title`       | string | ❌       | Tiêu đề hiển thị (mặc định: video ID) |
| `description` | string | ❌       | Mô tả ngắn                            |

---

## Response

### Success: 201 Created

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itemType": "VIDEO",
  "refId": "660e8400-e29b-41d4-a716-446655440000",
  "title": "Bài giảng Thuật toán",
  "sortOrder": 2,
  "metadata": {
    "youtubeUrl": "https://youtu.be/kOiZpMpBpAU",
    "videoId": "kOiZpMpBpAU",
    "description": "Video giới thiệu về giải thuật"
  },
  "createdAt": "2024-12-23T13:15:00+07:00"
}
```

| Field                 | Kiểu   | Mô tả                                          |
| --------------------- | ------ | ---------------------------------------------- |
| `id`                  | UUID   | ID của chapter item                            |
| `itemType`            | string | Luôn là `"VIDEO"`                              |
| `refId`               | UUID   | ID của NotebookFile (dùng để check trạng thái) |
| `title`               | string | Tiêu đề                                        |
| `sortOrder`           | number | Thứ tự trong chapter                           |
| `metadata.youtubeUrl` | string | URL gốc                                        |
| `metadata.videoId`    | string | YouTube video ID                               |
| `createdAt`           | string | Thời gian tạo (ISO 8601)                       |

### Error Responses

| Code | Khi nào                         |
| ---- | ------------------------------- |
| 400  | URL không hợp lệ hoặc trống     |
| 403  | Không có quyền truy cập chapter |
| 404  | Chapter không tồn tại           |

---

## TypeScript

### Interfaces

```typescript
// === REQUEST ===
interface ChapterYoutubeUploadRequest {
  youtubeUrl: string;
  title?: string;
  description?: string;
}

// === RESPONSE ===
interface ChapterItemResponse {
  id: string;
  itemType: "FILE" | "VIDEO" | "QUIZ" | "LECTURE" | "NOTE" | "FLASHCARD";
  refId: string | null;
  title: string;
  sortOrder: number;
  metadata: VideoMetadata | Record<string, unknown>;
  createdAt: string;
}

interface VideoMetadata {
  youtubeUrl: string;
  videoId: string;
  description?: string;
}
```

### React Hook

```typescript
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";

interface AddYoutubeParams {
  chapterId: string;
  youtubeUrl: string;
  title?: string;
  description?: string;
}

export function useAddYoutubeVideo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ chapterId, ...body }: AddYoutubeParams) => {
      const res = await apiClient.post<ChapterItemResponse>(
        `/lecturer/chapters/${chapterId}/items/youtube`,
        body
      );
      return res.data;
    },
    onSuccess: (_, { chapterId }) => {
      queryClient.invalidateQueries({ queryKey: ["chapter-items", chapterId] });
    },
  });
}
```

### Usage

```tsx
const { mutate, isPending } = useAddYoutubeVideo();

const handleAdd = () => {
  mutate(
    {
      chapterId: "...",
      youtubeUrl: "https://youtu.be/kOiZpMpBpAU",
      title: "Bài giảng #1",
    },
    {
      onSuccess: (data) => {
        toast.success("Đã thêm video!");
        // Video đã thêm, nhưng embedding đang chạy nền
      },
      onError: (err) => toast.error(err.message),
    }
  );
};
```

---

## Lưu ý quan trọng cho FE

### 1. API Response ngay lập tức

- Response trả về **trong ~1 giây**
- **KHÔNG cần** đợi trích xuất phụ đề

### 2. Embedding chạy nền (30-60 giây)

- Phụ đề được trích xuất async
- Chunks + embedding tạo async
- Có thể check trạng thái qua `GET /lecturer/notebooks/{notebookId}/files` nếu cần

### 3. Hiển thị video

Dùng `metadata.videoId` để embed YouTube player:

```tsx
<iframe
  src={`https://www.youtube.com/embed/${metadata.videoId}`}
  allowFullScreen
/>
```

### 4. Check trạng thái xử lý

Nếu cần hiển thị trạng thái embedding:

```typescript
// NotebookFile có các trạng thái:
// - "approved": Đang chờ xử lý
// - "processing": Đang xử lý
// - "done": Hoàn thành
// - "failed": Lỗi
```

---

## cURL Example

```bash
curl -X POST 'http://localhost:8386/lecturer/chapters/{chapterId}/items/youtube' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "youtubeUrl": "https://youtu.be/kOiZpMpBpAU",
    "title": "Bài giảng Thuật toán"
  }'
```
