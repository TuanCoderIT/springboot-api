# API AI Generation - Hướng dẫn cho Frontend

## Tổng quan

API AI Generation xử lý các tính năng tạo nội dung AI như Quiz, Summary, Flashcards, TTS, Video...

**Base URL:** `/user/notebooks/{notebookId}/ai`

**Authentication:** Yêu cầu JWT token trong header `Authorization: Bearer <token>`

---

## 1. Tạo Quiz (Async)

### Endpoint

```
POST /user/notebooks/{notebookId}/ai/quiz/generate
```

### Query Parameters

| Parameter           | Type     | Required | Default      | Mô tả                                                 |
| ------------------- | -------- | -------- | ------------ | ----------------------------------------------------- |
| `fileIds`           | `UUID[]` | ✅ Yes   | -            | Danh sách file IDs để tạo quiz                        |
| `numberOfQuestions` | `string` | No       | `"standard"` | Số lượng câu hỏi: `"few"` \| `"standard"` \| `"many"` |
| `difficultyLevel`   | `string` | No       | `"medium"`   | Độ khó: `"easy"` \| `"medium"` \| `"hard"`            |

### Request Example

```bash
curl -X POST "https://api.example.com/user/notebooks/123e4567-e89b-12d3-a456-426614174000/ai/quiz/generate?fileIds=abc123,def456&numberOfQuestions=standard&difficultyLevel=medium" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Response - Success (200)

```json
{
  "taskId": "789e0123-e89b-12d3-a456-426614174000",
  "status": "queued",
  "message": "Quiz đang được tạo ở nền. Sử dụng taskId để theo dõi tiến trình.",
  "success": true
}
```

### Response - Error (400)

```json
{
  "error": "Danh sách file IDs không được để trống"
}
```

### Lưu ý

- Quiz được tạo **bất đồng bộ (async)** - API trả về ngay `taskId`
- Sử dụng `taskId` để theo dõi tiến trình qua API **Get AI Tasks**
- Status flow: `queued` → `processing` → `done` | `failed`

---

## 2. Lấy danh sách AI Tasks

### Endpoint

```
GET /user/notebooks/{notebookId}/ai/tasks
```

### Query Parameters

| Parameter  | Type     | Required | Default | Mô tả                                                                                              |
| ---------- | -------- | -------- | ------- | -------------------------------------------------------------------------------------------------- |
| `taskType` | `string` | No       | `null`  | Lọc theo loại task: `"quiz"` \| `"summary"` \| `"flashcards"` \| `"tts"` \| `"video"` \| `"other"` |

### Request Example

```bash
# Lấy tất cả tasks
curl "https://api.example.com/user/notebooks/123e4567-e89b-12d3-a456-426614174000/ai/tasks" \
  -H "Authorization: Bearer <your-jwt-token>"

# Lọc chỉ lấy quiz tasks
curl "https://api.example.com/user/notebooks/123e4567-e89b-12d3-a456-426614174000/ai/tasks?taskType=quiz" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Response - Success (200)

```json
[
  {
    "id": "789e0123-e89b-12d3-a456-426614174000",
    "notebookId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "user-uuid-here",
    "userFullName": "Nguyễn Văn A",
    "userAvatar": "https://example.com/avatar.jpg",
    "taskType": "quiz",
    "status": "done",
    "errorMessage": null,
    "createdAt": "2024-12-10T10:30:00+07:00",
    "updatedAt": "2024-12-10T10:32:15+07:00",
    "fileCount": 3,
    "isOwner": true
  },
  {
    "id": "456e0123-e89b-12d3-a456-426614174000",
    "notebookId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "another-user-uuid",
    "userFullName": "Trần Thị B",
    "userAvatar": "https://example.com/avatar2.jpg",
    "taskType": "quiz",
    "status": "done",
    "errorMessage": null,
    "createdAt": "2024-12-10T09:00:00+07:00",
    "updatedAt": "2024-12-10T09:02:30+07:00",
    "fileCount": 2,
    "isOwner": false
  }
]
```

### Response Fields

| Field          | Type      | Mô tả                                                               |
| -------------- | --------- | ------------------------------------------------------------------- |
| `id`           | `UUID`    | ID của task                                                         |
| `notebookId`   | `UUID`    | ID của notebook                                                     |
| `userId`       | `UUID`    | ID của user tạo task                                                |
| `userFullName` | `string`  | Tên đầy đủ của user tạo task                                        |
| `userAvatar`   | `string`  | URL avatar của user tạo task                                        |
| `taskType`     | `string`  | Loại task: `quiz`, `summary`, `flashcards`, `tts`, `video`, `other` |
| `status`       | `string`  | Trạng thái: `queued`, `processing`, `done`, `failed`                |
| `errorMessage` | `string?` | Thông báo lỗi (nếu `status = failed`)                               |
| `createdAt`    | `ISO8601` | Thời gian tạo                                                       |
| `updatedAt`    | `ISO8601` | Thời gian cập nhật                                                  |
| `fileCount`    | `number`  | Số lượng files được dùng để tạo task                                |
| `isOwner`      | `boolean` | `true` nếu task thuộc về user hiện tại                              |

### Quy tắc hiển thị Tasks

| Điều kiện                  | Hiển thị                                                 |
| -------------------------- | -------------------------------------------------------- |
| Task của **user hiện tại** | Tất cả status (`queued`, `processing`, `done`, `failed`) |
| Task của **người khác**    | Chỉ hiển thị `done`                                      |

---

## 3. Status Flow

```
┌─────────┐     ┌────────────┐     ┌──────┐
│ queued  │ ──▶ │ processing │ ──▶ │ done │
└─────────┘     └────────────┘     └──────┘
                      │
                      ▼
                 ┌────────┐
                 │ failed │
                 └────────┘
```

### Status Descriptions

| Status       | Mô tả                    | UI Suggestion                          |
| ------------ | ------------------------ | -------------------------------------- |
| `queued`     | Task đang trong hàng đợi | Hiện spinner + "Đang chờ..."           |
| `processing` | Task đang được xử lý     | Hiện spinner + "Đang tạo..."           |
| `done`       | Task hoàn thành          | Hiện icon ✅ + cho phép xem kết quả    |
| `failed`     | Task thất bại            | Hiện icon ❌ + hiển thị `errorMessage` |

---

## 4. TypeScript Interfaces

```typescript
// Request types
interface GenerateQuizParams {
  notebookId: string;
  fileIds: string[];
  numberOfQuestions?: "few" | "standard" | "many";
  difficultyLevel?: "easy" | "medium" | "hard";
}

interface GetAiTasksParams {
  notebookId: string;
  taskType?: "quiz" | "summary" | "flashcards" | "tts" | "video" | "other";
}

// Response types
interface GenerateQuizResponse {
  taskId: string;
  status: "queued";
  message: string;
  success: boolean;
}

interface AiTaskResponse {
  id: string;
  notebookId: string;
  userId: string;
  userFullName: string;
  userAvatar: string;
  taskType: "quiz" | "summary" | "flashcards" | "tts" | "video" | "other";
  status: "queued" | "processing" | "done" | "failed";
  errorMessage: string | null;
  createdAt: string; // ISO8601
  updatedAt: string; // ISO8601
  fileCount: number;
  isOwner: boolean;
}
```

---

## 5. Frontend Implementation Guide

### 5.1. Polling cho Task Status

Vì quiz được tạo async, FE cần poll để cập nhật status:

```typescript
async function pollTaskStatus(
  notebookId: string,
  taskId: string,
  onStatusChange: (task: AiTaskResponse) => void
) {
  const POLL_INTERVAL = 3000; // 3 giây
  const MAX_ATTEMPTS = 60; // 3 phút max

  let attempts = 0;

  const poll = async () => {
    attempts++;

    const tasks = await fetchAiTasks(notebookId, "quiz");
    const task = tasks.find((t) => t.id === taskId);

    if (!task) {
      console.error("Task not found");
      return;
    }

    onStatusChange(task);

    // Stop polling if done or failed
    if (task.status === "done" || task.status === "failed") {
      return;
    }

    // Continue polling if still processing
    if (attempts < MAX_ATTEMPTS) {
      setTimeout(poll, POLL_INTERVAL);
    }
  };

  poll();
}
```

### 5.2. Generate Quiz Flow

```typescript
async function handleGenerateQuiz(fileIds: string[]) {
  try {
    // 1. Call API to start quiz generation
    const response = await generateQuiz({
      notebookId,
      fileIds,
      numberOfQuestions: "standard",
      difficultyLevel: "medium",
    });

    // 2. Show immediate feedback
    toast.info("Quiz đang được tạo...");

    // 3. Start polling for status updates
    pollTaskStatus(notebookId, response.taskId, (task) => {
      if (task.status === "done") {
        toast.success("Quiz đã được tạo thành công!");
        refreshQuizList(); // Refresh UI
      } else if (task.status === "failed") {
        toast.error(`Tạo quiz thất bại: ${task.errorMessage}`);
      }
    });
  } catch (error) {
    toast.error("Không thể tạo quiz");
  }
}
```

### 5.3. UI Component Example

```tsx
function AiTaskCard({ task }: { task: AiTaskResponse }) {
  const statusConfig = {
    queued: { icon: "⏳", label: "Đang chờ", color: "gray" },
    processing: { icon: "🔄", label: "Đang xử lý", color: "blue" },
    done: { icon: "✅", label: "Hoàn thành", color: "green" },
    failed: { icon: "❌", label: "Thất bại", color: "red" },
  };

  const config = statusConfig[task.status];

  return (
    <div className="task-card">
      <div className="task-header">
        <img src={task.userAvatar} alt={task.userFullName} />
        <span>{task.userFullName}</span>
        {task.isOwner && <span className="badge">Của bạn</span>}
      </div>

      <div className="task-body">
        <span className="task-type">{task.taskType}</span>
        <span className={`status status-${config.color}`}>
          {config.icon} {config.label}
        </span>
      </div>

      <div className="task-meta">
        <span>{task.fileCount} files</span>
        <span>{formatDate(task.createdAt)}</span>
      </div>

      {task.status === "failed" && task.errorMessage && (
        <div className="error-message">{task.errorMessage}</div>
      )}
    </div>
  );
}
```

---

## 6. Error Handling

### HTTP Status Codes

| Code  | Mô tả                                        |
| ----- | -------------------------------------------- |
| `200` | Success                                      |
| `400` | Bad Request - validation error               |
| `401` | Unauthorized - chưa đăng nhập                |
| `403` | Forbidden - không có quyền truy cập notebook |
| `404` | Not Found - notebook không tồn tại           |
| `500` | Internal Server Error                        |

### Error Response Format

```json
{
  "error": "Mô tả lỗi chi tiết",
  "errorType": "VALIDATION_ERROR"
}
```

---

## 7. API Endpoints Summary

| Method | Endpoint                                        | Mô tả                  |
| ------ | ----------------------------------------------- | ---------------------- |
| `POST` | `/user/notebooks/{notebookId}/ai/quiz/generate` | Tạo quiz (async)       |
| `GET`  | `/user/notebooks/{notebookId}/ai/tasks`         | Lấy danh sách AI tasks |

### Coming Soon

| Method | Endpoint                                              | Mô tả              |
| ------ | ----------------------------------------------------- | ------------------ |
| `POST` | `/user/notebooks/{notebookId}/ai/summary/generate`    | Tạo summary        |
| `POST` | `/user/notebooks/{notebookId}/ai/flashcards/generate` | Tạo flashcards     |
| `POST` | `/user/notebooks/{notebookId}/ai/tts/generate`        | Tạo text-to-speech |
| `POST` | `/user/notebooks/{notebookId}/ai/video/generate`      | Tạo video          |
