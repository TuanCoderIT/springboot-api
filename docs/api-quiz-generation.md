# API Hướng Dẫn: AI Generation

## Tổng quan

Backend cung cấp các API cho AI Generation tại base URL:

```
/user/notebooks/{notebookId}/ai
```

**Các tính năng:**

- Quiz Generation
- AI Tasks Management
- (Coming soon) Summary, Flashcards, TTS, Video

---

## 1. Quiz Generation

### Tạo Quiz (Background Processing)

```
POST /user/notebooks/{notebookId}/ai/quiz/generate
```

### Headers

```
Authorization: Bearer <token>
```

### Query Parameters

| Param               | Type   | Required | Default    | Mô tả                                                                |
| ------------------- | ------ | -------- | ---------- | -------------------------------------------------------------------- |
| `fileIds`           | UUID[] | ✅       | -          | Danh sách file IDs. Truyền nhiều lần: `?fileIds=uuid1&fileIds=uuid2` |
| `numberOfQuestions` | String | ❌       | `standard` | Số lượng: `few`, `standard`, `many`                                  |
| `difficultyLevel`   | String | ❌       | `medium`   | Độ khó: `easy`, `medium`, `hard`                                     |

### Request Example

```bash
curl -X POST \
  'http://localhost:8386/user/notebooks/{notebookId}/ai/quiz/generate?fileIds=uuid1&fileIds=uuid2&numberOfQuestions=standard&difficultyLevel=medium' \
  -H 'Authorization: Bearer <token>'
```

### Response Success (200 OK)

```json
{
  "taskId": "6bf57d41-d948-414a-b130-a3568353d0f8",
  "status": "queued",
  "message": "Quiz đang được tạo ở nền. Sử dụng taskId để theo dõi tiến trình.",
  "success": true
}
```

---

## 2. AI Tasks Management

### Lấy Danh Sách AI Tasks

```
GET /user/notebooks/{notebookId}/ai/tasks
```

### Headers

```
Authorization: Bearer <token>
```

### Query Parameters

| Param      | Type   | Required | Mô tả                                                                      |
| ---------- | ------ | -------- | -------------------------------------------------------------------------- |
| `taskType` | String | ❌       | Filter theo loại: `quiz`, `summary`, `flashcards`, `tts`, `video`, `other` |

### Logic hiển thị

| Ownership               | Status hiển thị                                              |
| ----------------------- | ------------------------------------------------------------ |
| **Task của tôi**        | Tất cả: `queued`, `processing`, `done`, `failed`, `canceled` |
| **Task của người khác** | Chỉ `done`                                                   |

### Request Example

```bash
# Lấy tất cả tasks
curl -X GET \
  'http://localhost:8386/user/notebooks/{notebookId}/ai/tasks' \
  -H 'Authorization: Bearer <token>'

# Lấy chỉ quiz tasks
curl -X GET \
  'http://localhost:8386/user/notebooks/{notebookId}/ai/tasks?taskType=quiz' \
  -H 'Authorization: Bearer <token>'
```

### Response Success (200 OK)

```json
[
  {
    "id": "6bf57d41-d948-414a-b130-a3568353d0f8",
    "notebookId": "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userFullName": "Nguyễn Văn A",
    "userAvatar": "https://example.com/avatars/user1.jpg",
    "taskType": "quiz",
    "status": "done",
    "errorMessage": null,
    "createdAt": "2025-12-10T16:30:00+07:00",
    "updatedAt": "2025-12-10T16:32:00+07:00",
    "fileCount": 4,
    "isOwner": true
  }
]
```

### Response Fields

| Field          | Type     | Nullable | Mô tả                             |
| -------------- | -------- | -------- | --------------------------------- |
| `id`           | UUID     | ❌       | ID của task                       |
| `notebookId`   | UUID     | ❌       | ID của notebook                   |
| `userId`       | UUID     | ✅       | ID của người tạo task             |
| `userFullName` | String   | ✅       | Tên đầy đủ của người tạo          |
| `userAvatar`   | String   | ✅       | URL avatar của người tạo          |
| `taskType`     | String   | ❌       | Loại task                         |
| `status`       | String   | ❌       | Trạng thái task                   |
| `errorMessage` | String   | ✅       | Thông báo lỗi                     |
| `createdAt`    | DateTime | ❌       | Thời gian tạo                     |
| `updatedAt`    | DateTime | ❌       | Thời gian cập nhật                |
| `fileCount`    | Integer  | ❌       | Số lượng files được sử dụng       |
| `isOwner`      | Boolean  | ❌       | `true` nếu task của user hiện tại |

---

## 3. Task Status Flow

```
┌─────────┐    ┌────────────┐    ┌──────┐
│ queued  │ ─> │ processing │ ─> │ done │
└─────────┘    └────────────┘    └──────┘
                     │
                     v
               ┌──────────┐
               │  failed  │
               └──────────┘
```

| Status       | Mô tả      | UI Suggestion                 |
| ------------ | ---------- | ----------------------------- |
| `queued`     | Đang chờ   | 🟡 Spinner + "Đang chờ..."    |
| `processing` | Đang xử lý | 🔵 Progress + "Đang xử lý..." |
| `done`       | Hoàn thành | 🟢 Hiển thị kết quả           |
| `failed`     | Thất bại   | 🔴 Hiển thị `errorMessage`    |
| `canceled`   | Đã hủy     | ⚪ Trạng thái đã hủy          |

---

## 4. Deprecated Endpoints

Các endpoint cũ vẫn hoạt động nhưng được khuyến khích chuyển sang API mới:

| Old Endpoint                                      | New Endpoint                                 |
| ------------------------------------------------- | -------------------------------------------- |
| `GET /user/notebooks/{id}/bot-chat/generate-quiz` | `POST /user/notebooks/{id}/ai/quiz/generate` |
| `GET /user/notebooks/{id}/bot-chat/ai-tasks`      | `GET /user/notebooks/{id}/ai/tasks`          |

---

## 5. Frontend Usage Example

```javascript
// Tạo quiz
async function createQuiz(notebookId, fileIds, options = {}) {
  const params = new URLSearchParams({
    numberOfQuestions: options.numberOfQuestions || "standard",
    difficultyLevel: options.difficultyLevel || "medium",
  });

  fileIds.forEach((id) => params.append("fileIds", id));

  const response = await fetch(
    `/user/notebooks/${notebookId}/ai/quiz/generate?${params}`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );

  return response.json();
}

// Polling để check status
async function pollTaskStatus(notebookId, taskId) {
  const pollInterval = 3000; // 3 giây
  const maxAttempts = 60;

  for (let i = 0; i < maxAttempts; i++) {
    await sleep(pollInterval);

    const tasks = await fetch(
      `/user/notebooks/${notebookId}/ai/tasks?taskType=quiz`,
      { headers: { Authorization: `Bearer ${token}` } }
    ).then((r) => r.json());

    const task = tasks.find((t) => t.id === taskId);

    if (!task) continue;

    switch (task.status) {
      case "done":
        return { success: true, task };
      case "failed":
        return { success: false, error: task.errorMessage };
      default:
      // Continue polling
    }
  }

  return { success: false, error: "Timeout" };
}
```

---

## 6. Error Responses

### 400 Bad Request

```json
{
  "status": 400,
  "message": "Danh sách file IDs không được để trống",
  "timestamp": "2025-12-10T16:10:34.710639"
}
```

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "User chưa đăng nhập.",
  "timestamp": "2025-12-10T16:10:34.710639"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Notebook không tồn tại",
  "timestamp": "2025-12-10T16:10:34.710639"
}
```
