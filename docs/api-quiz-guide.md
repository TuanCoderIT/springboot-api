# API Quiz - Hướng dẫn cho Frontend

## Tổng quan

Hệ thống Quiz cho phép tạo câu hỏi trắc nghiệm tự động từ nội dung tài liệu trong notebook.

**Base URL:** `/user/notebooks/{notebookId}/ai`

**Authentication:** Yêu cầu JWT token trong header `Authorization: Bearer <token>`

---

## 📋 Mục lục

1. [Tạo Quiz (Async)](#1-tạo-quiz-async)
2. [Theo dõi tiến trình tạo Quiz](#2-theo-dõi-tiến-trình-tạo-quiz)
3. [Flow hoàn chỉnh](#3-flow-hoàn-chỉnh)
4. [TypeScript Types](#4-typescript-types)
5. [React Hooks Example](#5-react-hooks-example)
6. [UI Components](#6-ui-components)

---

## 1. Tạo Quiz (Async)

### Endpoint

```
POST /user/notebooks/{notebookId}/ai/quiz/generate
```

### Mô tả

Tạo quiz từ các file trong notebook. Quá trình tạo quiz được thực hiện **bất đồng bộ** (async) vì cần:

1. Tóm tắt nội dung các file đã chọn
2. Gọi AI (Gemini/Groq) để tạo câu hỏi
3. Parse và lưu quiz vào database

### Query Parameters

| Parameter                | Type     | Required | Default      | Mô tả                                      |
| ------------------------ | -------- | -------- | ------------ | ------------------------------------------ |
| `fileIds`                | `UUID[]` | ✅ Yes   | -            | Danh sách file IDs để tạo quiz             |
| `numberOfQuestions`      | `string` | No       | `"standard"` | Số lượng câu hỏi                           |
| `difficultyLevel`        | `string` | No       | `"medium"`   | Độ khó câu hỏi                             |
| `additionalRequirements` | `string` | No       | `null`       | Yêu cầu bổ sung từ người dùng (text tự do) |

### Giá trị `numberOfQuestions`

| Value        | Mô tả         | Số câu ~ước tính |
| ------------ | ------------- | ---------------- |
| `"few"`      | Ít câu hỏi    | 3-5 câu          |
| `"standard"` | Tiêu chuẩn    | 5-10 câu         |
| `"many"`     | Nhiều câu hỏi | 10-15 câu        |

### Giá trị `difficultyLevel`

| Value      | Mô tả                  | difficulty_level trong DB |
| ---------- | ---------------------- | ------------------------- |
| `"easy"`   | Dễ - câu hỏi cơ bản    | 1                         |
| `"medium"` | Trung bình             | 2                         |
| `"hard"`   | Khó - yêu cầu hiểu sâu | 3                         |

### Request Example

```bash
# Tạo quiz với 2 files, số lượng tiêu chuẩn, độ khó trung bình
curl -X POST "https://api.example.com/user/notebooks/123e4567-e89b-12d3-a456-426614174000/ai/quiz/generate?fileIds=abc123-uuid&fileIds=def456-uuid&numberOfQuestions=standard&difficultyLevel=medium" \
  -H "Authorization: Bearer <your-jwt-token>"

# Tạo quiz với yêu cầu bổ sung
curl -X POST "https://api.example.com/user/notebooks/123e4567-e89b-12d3-a456-426614174000/ai/quiz/generate?fileIds=abc123-uuid&numberOfQuestions=many&difficultyLevel=hard&additionalRequirements=Tập%20trung%20vào%20chương%203%20về%20Spring%20Security" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Ví dụ `additionalRequirements`

| Yêu cầu mẫu                             | Mô tả                                |
| --------------------------------------- | ------------------------------------ |
| "Tập trung vào chương 3"                | Chỉ tạo câu hỏi từ nội dung chương 3 |
| "Câu hỏi về lập trình Python"           | Nhấn mạnh chủ đề Python              |
| "Bao gồm code examples"                 | Yêu cầu câu hỏi kèm đoạn code        |
| "Dành cho người mới học"                | Tạo câu hỏi dễ hiểu                  |
| "Không hỏi về lý thuyết, chỉ thực hành" | Loại trừ câu hỏi lý thuyết           |

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

```json
{
  "error": "Không tìm thấy file hợp lệ nào"
}
```

---

## 2. Theo dõi tiến trình tạo Quiz

### Endpoint

```
GET /user/notebooks/{notebookId}/ai/tasks?taskType=quiz
```

### Query Parameters

| Parameter  | Type     | Required | Default | Mô tả                                  |
| ---------- | -------- | -------- | ------- | -------------------------------------- |
| `taskType` | `string` | No       | `null`  | Filter: `"quiz"` để chỉ lấy quiz tasks |

### Request Example

```bash
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
  }
]
```

### Task Status Flow

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

| Status       | Mô tả               | Thời gian ước tính |
| ------------ | ------------------- | ------------------ |
| `queued`     | Đang trong hàng đợi | 0-5 giây           |
| `processing` | AI đang tạo quiz    | 10-60 giây         |
| `done`       | Hoàn thành          | -                  |
| `failed`     | Thất bại            | -                  |

---

## 3. Flow hoàn chỉnh

### Sequence Diagram

```
┌──────────┐          ┌──────────┐          ┌─────────┐
│  Frontend│          │  Backend │          │   AI    │
└────┬─────┘          └────┬─────┘          └────┬────┘
     │                     │                     │
     │  POST /quiz/generate│                     │
     │────────────────────>│                     │
     │                     │                     │
     │  {taskId, status}   │                     │
     │<────────────────────│                     │
     │                     │                     │
     │                     │  Summarize files    │
     │                     │────────────────────>│
     │                     │                     │
     │  GET /tasks (poll)  │                     │
     │────────────────────>│                     │
     │                     │  Generate quiz      │
     │  status: processing │────────────────────>│
     │<────────────────────│                     │
     │                     │                     │
     │  GET /tasks (poll)  │  Quiz JSON          │
     │────────────────────>│<────────────────────│
     │                     │                     │
     │  status: done       │  Save to DB         │
     │<────────────────────│                     │
     │                     │                     │
```

### Step-by-Step Implementation

```typescript
// 1. User chọn files và nhấn "Tạo Quiz"
async function handleCreateQuiz(selectedFileIds: string[]) {
  try {
    // 2. Gọi API tạo quiz
    const response = await api.post(
      `/user/notebooks/${notebookId}/ai/quiz/generate`,
      null,
      {
        params: {
          fileIds: selectedFileIds,
          numberOfQuestions: "standard",
          difficultyLevel: "medium",
        },
      }
    );

    const { taskId, status } = response.data;

    // 3. Thông báo user
    toast.info("Quiz đang được tạo...");

    // 4. Bắt đầu polling
    startPolling(taskId);
  } catch (error) {
    toast.error("Không thể tạo quiz");
  }
}

// 5. Polling để theo dõi tiến trình
function startPolling(taskId: string) {
  const POLL_INTERVAL = 3000; // 3 giây
  const MAX_DURATION = 180000; // 3 phút

  const startTime = Date.now();

  const poll = async () => {
    // Check timeout
    if (Date.now() - startTime > MAX_DURATION) {
      toast.error("Tạo quiz timeout. Vui lòng thử lại.");
      return;
    }

    // Fetch tasks
    const tasks = await api.get(`/user/notebooks/${notebookId}/ai/tasks`, {
      params: { taskType: "quiz" },
    });

    const task = tasks.data.find((t: AiTask) => t.id === taskId);

    if (!task) {
      setTimeout(poll, POLL_INTERVAL);
      return;
    }

    // Update UI based on status
    switch (task.status) {
      case "queued":
        setQuizStatus("Đang chờ xử lý...");
        setTimeout(poll, POLL_INTERVAL);
        break;

      case "processing":
        setQuizStatus("AI đang tạo quiz...");
        setTimeout(poll, POLL_INTERVAL);
        break;

      case "done":
        toast.success("Quiz đã được tạo thành công!");
        refreshQuizList(); // Load quizzes mới
        break;

      case "failed":
        toast.error(`Tạo quiz thất bại: ${task.errorMessage}`);
        break;
    }
  };

  poll();
}
```

---

## 4. TypeScript Types

```typescript
// ============================================
// REQUEST TYPES
// ============================================

interface GenerateQuizParams {
  notebookId: string;
  fileIds: string[];
  numberOfQuestions?: "few" | "standard" | "many";
  difficultyLevel?: "easy" | "medium" | "hard";
  additionalRequirements?: string; // Yêu cầu bổ sung từ người dùng
}

// ============================================
// RESPONSE TYPES
// ============================================

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
  createdAt: string;
  updatedAt: string;
  fileCount: number;
  isOwner: boolean;
}

// ============================================
// QUIZ DATA TYPES (saved in DB)
// ============================================

interface Quiz {
  id: string;
  notebookId: string;
  question: string;
  explanation: string;
  difficultyLevel: 1 | 2 | 3; // 1=easy, 2=medium, 3=hard
  createdBy: {
    id: string;
    fullName: string;
    avatarUrl: string;
  };
  createdAt: string;
  options: QuizOption[];
}

interface QuizOption {
  id: string;
  text: string;
  isCorrect: boolean;
  feedback: string;
  position: number;
}
```

---

## 5. React Hooks Example

### useGenerateQuiz Hook

```typescript
import { useState, useCallback } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { toast } from "sonner";

interface UseGenerateQuizOptions {
  notebookId: string;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}

export function useGenerateQuiz({
  notebookId,
  onSuccess,
  onError,
}: UseGenerateQuizOptions) {
  const [taskId, setTaskId] = useState<string | null>(null);
  const [status, setStatus] = useState<
    "idle" | "queued" | "processing" | "done" | "failed"
  >("idle");
  const queryClient = useQueryClient();

  const generateMutation = useMutation({
    mutationFn: async (params: {
      fileIds: string[];
      numberOfQuestions?: "few" | "standard" | "many";
      difficultyLevel?: "easy" | "medium" | "hard";
    }) => {
      const response = await api.post(
        `/user/notebooks/${notebookId}/ai/quiz/generate`,
        null,
        { params }
      );
      return response.data;
    },
    onSuccess: (data) => {
      setTaskId(data.taskId);
      setStatus("queued");
      startPolling(data.taskId);
    },
    onError: (error: Error) => {
      toast.error("Không thể tạo quiz");
      onError?.(error);
    },
  });

  const startPolling = useCallback(
    (id: string) => {
      const POLL_INTERVAL = 3000;
      const MAX_DURATION = 180000;
      const startTime = Date.now();

      const poll = async () => {
        if (Date.now() - startTime > MAX_DURATION) {
          setStatus("failed");
          toast.error("Tạo quiz timeout");
          return;
        }

        try {
          const response = await api.get(
            `/user/notebooks/${notebookId}/ai/tasks`,
            { params: { taskType: "quiz" } }
          );

          const task = response.data.find((t: AiTaskResponse) => t.id === id);

          if (!task) {
            setTimeout(poll, POLL_INTERVAL);
            return;
          }

          setStatus(task.status);

          if (task.status === "done") {
            toast.success("Quiz đã được tạo thành công!");
            queryClient.invalidateQueries({
              queryKey: ["quizzes", notebookId],
            });
            onSuccess?.();
          } else if (task.status === "failed") {
            toast.error(`Tạo quiz thất bại: ${task.errorMessage}`);
            onError?.(new Error(task.errorMessage || "Unknown error"));
          } else {
            setTimeout(poll, POLL_INTERVAL);
          }
        } catch (error) {
          setTimeout(poll, POLL_INTERVAL);
        }
      };

      poll();
    },
    [notebookId, queryClient, onSuccess, onError]
  );

  const reset = useCallback(() => {
    setTaskId(null);
    setStatus("idle");
  }, []);

  return {
    generate: generateMutation.mutate,
    isGenerating:
      generateMutation.isPending ||
      status === "queued" ||
      status === "processing",
    taskId,
    status,
    reset,
  };
}
```

### Usage Example

```tsx
function QuizGeneratorButton({ selectedFiles }: { selectedFiles: string[] }) {
  const { generate, isGenerating, status } = useGenerateQuiz({
    notebookId,
    onSuccess: () => {
      // Refresh quiz list
    },
  });

  const handleClick = () => {
    generate({
      fileIds: selectedFiles,
      numberOfQuestions: "standard",
      difficultyLevel: "medium",
    });
  };

  return (
    <Button
      onClick={handleClick}
      disabled={isGenerating || selectedFiles.length === 0}
    >
      {isGenerating ? (
        <>
          <Spinner className="mr-2" />
          {status === "queued" && "Đang chờ..."}
          {status === "processing" && "Đang tạo quiz..."}
        </>
      ) : (
        "Tạo Quiz"
      )}
    </Button>
  );
}
```

---

## 6. UI Components

### Quiz Generation Form

```tsx
import { useState } from "react";
import { Select, Button, Checkbox } from "@/components/ui";

interface QuizGeneratorFormProps {
  files: { id: string; name: string }[];
  onGenerate: (params: GenerateQuizParams) => void;
  isGenerating: boolean;
}

export function QuizGeneratorForm({
  files,
  onGenerate,
  isGenerating,
}: QuizGeneratorFormProps) {
  const [selectedFiles, setSelectedFiles] = useState<string[]>([]);
  const [numberOfQuestions, setNumberOfQuestions] = useState<
    "few" | "standard" | "many"
  >("standard");
  const [difficultyLevel, setDifficultyLevel] = useState<
    "easy" | "medium" | "hard"
  >("medium");

  const handleSubmit = () => {
    if (selectedFiles.length === 0) {
      toast.error("Vui lòng chọn ít nhất 1 file");
      return;
    }

    onGenerate({
      fileIds: selectedFiles,
      numberOfQuestions,
      difficultyLevel,
    });
  };

  return (
    <div className="space-y-4 p-4 border rounded-lg">
      <h3 className="text-lg font-semibold">Tạo Quiz từ tài liệu</h3>

      {/* File Selection */}
      <div>
        <label className="text-sm font-medium">Chọn tài liệu</label>
        <div className="mt-2 space-y-2 max-h-48 overflow-y-auto">
          {files.map((file) => (
            <label key={file.id} className="flex items-center gap-2">
              <Checkbox
                checked={selectedFiles.includes(file.id)}
                onCheckedChange={(checked) => {
                  if (checked) {
                    setSelectedFiles([...selectedFiles, file.id]);
                  } else {
                    setSelectedFiles(
                      selectedFiles.filter((id) => id !== file.id)
                    );
                  }
                }}
              />
              <span className="text-sm">{file.name}</span>
            </label>
          ))}
        </div>
        <p className="text-xs text-muted-foreground mt-1">
          Đã chọn {selectedFiles.length} file
        </p>
      </div>

      {/* Number of Questions */}
      <div>
        <label className="text-sm font-medium">Số lượng câu hỏi</label>
        <Select value={numberOfQuestions} onValueChange={setNumberOfQuestions}>
          <option value="few">Ít (3-5 câu)</option>
          <option value="standard">Tiêu chuẩn (5-10 câu)</option>
          <option value="many">Nhiều (10-15 câu)</option>
        </Select>
      </div>

      {/* Difficulty Level */}
      <div>
        <label className="text-sm font-medium">Độ khó</label>
        <Select value={difficultyLevel} onValueChange={setDifficultyLevel}>
          <option value="easy">Dễ</option>
          <option value="medium">Trung bình</option>
          <option value="hard">Khó</option>
        </Select>
      </div>

      {/* Submit Button */}
      <Button
        onClick={handleSubmit}
        disabled={isGenerating || selectedFiles.length === 0}
        className="w-full"
      >
        {isGenerating ? (
          <>
            <Spinner className="mr-2 h-4 w-4" />
            Đang tạo quiz...
          </>
        ) : (
          "✨ Tạo Quiz với AI"
        )}
      </Button>
    </div>
  );
}
```

### Task Progress Indicator

```tsx
interface TaskProgressProps {
  status: "idle" | "queued" | "processing" | "done" | "failed";
  errorMessage?: string;
}

export function TaskProgress({ status, errorMessage }: TaskProgressProps) {
  if (status === "idle") return null;

  const config = {
    queued: {
      icon: "⏳",
      text: "Đang chờ xử lý...",
      color: "text-gray-500",
      bgColor: "bg-gray-50",
    },
    processing: {
      icon: "🔄",
      text: "AI đang tạo quiz...",
      color: "text-blue-500",
      bgColor: "bg-blue-50",
    },
    done: {
      icon: "✅",
      text: "Quiz đã được tạo!",
      color: "text-green-500",
      bgColor: "bg-green-50",
    },
    failed: {
      icon: "❌",
      text: errorMessage || "Tạo quiz thất bại",
      color: "text-red-500",
      bgColor: "bg-red-50",
    },
  };

  const { icon, text, color, bgColor } = config[status];

  return (
    <div className={`flex items-center gap-2 p-3 rounded-lg ${bgColor}`}>
      <span className="text-xl animate-pulse">{icon}</span>
      <span className={`text-sm font-medium ${color}`}>{text}</span>
    </div>
  );
}
```

---

## 7. Best Practices

### ✅ Nên làm

1. **Disable button** khi đang tạo quiz để tránh spam
2. **Hiển thị progress** rõ ràng cho user biết đang ở bước nào
3. **Timeout handling** - thông báo nếu quá lâu
4. **Error handling** - hiển thị thông báo lỗi cụ thể
5. **Cache invalidation** - refresh quiz list sau khi tạo xong

### ❌ Không nên làm

1. **Poll quá nhanh** - 3 giây là optimal, không nên dưới 2 giây
2. **Poll vô hạn** - set max duration (3 phút)
3. **Gọi API khi không có files** - validate trước khi gọi
4. **Ignore errors** - luôn handle và hiển thị lỗi

---

## 8. Troubleshooting

| Lỗi                          | Nguyên nhân                                      | Giải pháp                           |
| ---------------------------- | ------------------------------------------------ | ----------------------------------- |
| "Không tìm thấy file hợp lệ" | File IDs không tồn tại hoặc không thuộc notebook | Kiểm tra lại danh sách files        |
| "Không thể tóm tắt tài liệu" | Files không có chunks/text content               | Đảm bảo files đã được xử lý OCR     |
| "LLM trả về response rỗng"   | Token limit hoặc API error                       | Giảm số files hoặc thử lại          |
| Timeout                      | Quiz generation quá lâu                          | Giảm số files, thử với ít files hơn |
