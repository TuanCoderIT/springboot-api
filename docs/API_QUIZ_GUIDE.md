# API Quiz - Hướng dẫn sử dụng

## 1. Lấy danh sách bộ Quiz (AI Sets)

### Endpoint

```
GET /user/notebooks/{notebookId}/ai/sets
```

### Đầu vào

| Tham số         | Vị trí | Bắt buộc | Mô tả                                              |
| --------------- | ------ | -------- | -------------------------------------------------- |
| `notebookId`    | Path   | ✅       | UUID của notebook                                  |
| `setType`       | Query  | ❌       | Lọc theo loại: `quiz`, `flashcard`, `tts`, `video` |
| `Authorization` | Header | ✅       | `Bearer {token}`                                   |

### Ví dụ Request

```bash
curl -X GET 'http://localhost:8386/user/notebooks/{notebookId}/ai/sets?setType=quiz' \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

### Đầu ra (200 OK)

```json
[
  {
    "id": "uuid",
    "notebookId": "uuid",
    "userId": "uuid",
    "userFullName": "Tên user",
    "userAvatar": "http://localhost:8386/uploads/avatar.jpg",
    "setType": "quiz",
    "status": "done",
    "errorMessage": null,
    "title": "Quiz từ 12 tài liệu",
    "description": null,
    "createdAt": "2025-12-10T16:14:52Z",
    "startedAt": "2025-12-10T16:14:52Z",
    "finishedAt": "2025-12-10T16:16:14Z",
    "updatedAt": "2025-12-10T16:16:14Z",
    "fileCount": 12,
    "owner": true
  }
]
```

### Các trường quan trọng

- `status`: `queued` → `processing` → `done` | `failed`
- `owner`: `true` nếu user hiện tại tạo bộ quiz này

### Lỗi

| Code | Message                |
| ---- | ---------------------- |
| 401  | Chưa đăng nhập         |
| 404  | Notebook không tồn tại |

---

## 2. Lấy chi tiết Quiz (Câu hỏi + Câu trả lời)

### Endpoint

```
GET /user/notebooks/{notebookId}/ai/quiz/{aiSetId}
```

### Đầu vào

| Tham số         | Vị trí | Bắt buộc | Mô tả                             |
| --------------- | ------ | -------- | --------------------------------- |
| `notebookId`    | Path   | ✅       | UUID của notebook                 |
| `aiSetId`       | Path   | ✅       | UUID của AI Set (lấy từ API trên) |
| `Authorization` | Header | ✅       | `Bearer {token}`                  |

### Yêu cầu

- User phải là **thành viên đã được duyệt** (`approved`) của notebook

### Ví dụ Request

```bash
curl -X GET 'http://localhost:8386/user/notebooks/{notebookId}/ai/quiz/{aiSetId}' \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

### Đầu ra (200 OK)

```json
{
  "aiSetId": "uuid",
  "title": "Quiz từ 12 tài liệu",
  "description": null,
  "status": "done",
  "errorMessage": null,
  "createdAt": "2025-12-10T16:14:52Z",
  "finishedAt": "2025-12-10T16:16:14Z",
  "createdById": "uuid",
  "createdByName": "Tên user",
  "createdByAvatar": "http://localhost:8386/uploads/avatar.jpg",
  "notebookId": "uuid",
  "totalQuizzes": 10,
  "quizzes": [
    {
      "id": "uuid",
      "question": "Nội dung câu hỏi?",
      "explanation": "Giải thích đáp án đúng",
      "difficultyLevel": 2,
      "createdAt": "2025-12-10T16:15:00Z",
      "options": [
        {
          "id": "uuid",
          "text": "Đáp án A",
          "isCorrect": true,
          "feedback": "Chính xác!",
          "position": 0
        },
        {
          "id": "uuid",
          "text": "Đáp án B",
          "isCorrect": false,
          "feedback": "Sai rồi...",
          "position": 1
        }
      ]
    }
  ]
}
```

### Giải thích các trường

| Trường            | Mô tả                        |
| ----------------- | ---------------------------- |
| `quizzes`         | Mảng các câu hỏi             |
| `question`        | Nội dung câu hỏi             |
| `explanation`     | Giải thích sau khi trả lời   |
| `difficultyLevel` | Độ khó (1-5)                 |
| `options`         | Mảng câu trả lời             |
| `isCorrect`       | `true` = đáp án đúng         |
| `feedback`        | Phản hồi khi chọn đáp án này |
| `position`        | Thứ tự hiển thị (0, 1, 2...) |

### Lỗi

| Code | Message                                                           |
| ---- | ----------------------------------------------------------------- |
| 400  | Bạn chưa tham gia nhóm này                                        |
| 400  | Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt |
| 400  | AI Set không thuộc notebook này                                   |
| 401  | Chưa đăng nhập                                                    |
| 404  | Notebook không tồn tại                                            |
| 404  | Không tìm thấy AI Set với ID: {id}                                |

---

## 3. Tạo Quiz mới

### Endpoint

```
POST /user/notebooks/{notebookId}/ai/quiz/generate
```

### Đầu vào

| Tham số                  | Vị trí | Bắt buộc | Mô tả                                                |
| ------------------------ | ------ | -------- | ---------------------------------------------------- |
| `notebookId`             | Path   | ✅       | UUID của notebook                                    |
| `fileIds`                | Query  | ✅       | Danh sách UUID file nguồn (có thể nhiều)             |
| `numberOfQuestions`      | Query  | ❌       | `few` \| `standard` \| `many` (mặc định: `standard`) |
| `difficultyLevel`        | Query  | ❌       | `easy` \| `medium` \| `hard` (mặc định: `medium`)    |
| `additionalRequirements` | Query  | ❌       | Yêu cầu bổ sung (text tự do)                         |
| `Authorization`          | Header | ✅       | `Bearer {token}`                                     |

### Ví dụ Request

```bash
curl -X POST 'http://localhost:8386/user/notebooks/{notebookId}/ai/quiz/generate?fileIds=uuid1&fileIds=uuid2&numberOfQuestions=standard&difficultyLevel=medium' \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

### Đầu ra (200 OK)

```json
{
  "aiSetId": "uuid",
  "status": "queued",
  "message": "Quiz đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.",
  "success": true
}
```

### Cách sử dụng

1. Gọi API này → nhận `aiSetId`
2. Poll API `/ai/sets` để kiểm tra `status`
3. Khi `status = done` → gọi API `/ai/quiz/{aiSetId}` để lấy quiz

### Lỗi

| Code | Message                                |
| ---- | -------------------------------------- |
| 400  | Danh sách file IDs không được để trống |
| 400  | Không tìm thấy file hợp lệ nào         |
| 401  | Chưa đăng nhập                         |
| 404  | Notebook không tồn tại                 |
| 404  | User không tồn tại                     |

---

## 4. Xóa AI Set

### Endpoint

```
DELETE /user/notebooks/{notebookId}/ai/sets/{aiSetId}
```

### Đầu vào

| Tham số         | Vị trí | Bắt buộc | Mô tả                   |
| --------------- | ------ | -------- | ----------------------- |
| `notebookId`    | Path   | ✅       | UUID của notebook       |
| `aiSetId`       | Path   | ✅       | UUID của AI Set cần xóa |
| `Authorization` | Header | ✅       | `Bearer {token}`        |

### Yêu cầu

- User phải là **người tạo** AI Set (owner)

### Ví dụ Request

```bash
curl -X DELETE 'http://localhost:8386/user/notebooks/{notebookId}/ai/sets/{aiSetId}' \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

### Đầu ra (204 No Content)

Không có body response

### Lỗi

| Code | Message                                     |
| ---- | ------------------------------------------- |
| 400  | Bạn chỉ có thể xóa AI Set do chính mình tạo |
| 401  | Chưa đăng nhập                              |
| 404  | Không tìm thấy AI Set với ID: {id}          |

---

## TypeScript Types

```typescript
// Response types
interface AiSetResponse {
  id: string;
  notebookId: string;
  userId: string;
  userFullName: string;
  userAvatar: string;
  setType: "quiz" | "flashcard" | "tts" | "video";
  status: "queued" | "processing" | "done" | "failed";
  errorMessage: string | null;
  title: string;
  description: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  updatedAt: string;
  fileCount: number;
  owner: boolean;
}

interface QuizListResponse {
  aiSetId: string;
  title: string;
  description: string | null;
  status: string;
  errorMessage: string | null;
  createdAt: string;
  finishedAt: string | null;
  createdById: string;
  createdByName: string;
  createdByAvatar: string;
  notebookId: string;
  quizzes: QuizResponse[];
  totalQuizzes: number;
}

interface QuizResponse {
  id: string;
  question: string;
  explanation: string | null;
  difficultyLevel: number;
  createdAt: string;
  options: QuizOptionResponse[];
}

interface QuizOptionResponse {
  id: string;
  text: string;
  isCorrect: boolean;
  feedback: string | null;
  position: number;
}

interface GenerateQuizResponse {
  aiSetId: string;
  status: string;
  message: string;
  success: boolean;
}
```

---

## Lỗi chung

```json
{
  "status": 400,
  "message": "Thông báo lỗi",
  "timestamp": "2025-12-10T23:30:00Z"
}
```
