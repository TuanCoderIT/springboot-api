# WebSocket AI Task Progress - Frontend Guide

## Kết nối WebSocket

### URL

```
ws://localhost:8386/ws?access_token={JWT_TOKEN}

# Hoặc với SockJS (fallback)
http://localhost:8386/ws?access_token={JWT_TOKEN}
```

---

## 2 Channels

### 1. Task Owner Channel (Progress chi tiết)

**Subscribe:** `/topic/ai-task/{aiSetId}`

Sau khi gọi API generate (quiz, flashcards, video...), subscribe ngay channel này với `aiSetId` nhận được.

**Message format:**

```json
{
  "aiSetId": "uuid",
  "type": "progress | done | failed",
  "step": "queued | processing | summarizing | generating | saving | completed | error",
  "progress": 10,
  "message": "Đang xử lý...",
  "setType": "quiz",
  "data": { ... }
}
```

**Xử lý:**

- `type: "progress"` → Cập nhật progress bar
- `type: "done"` → Refetch data, show success
- `type: "failed"` → Show error toast

---

### 2. Notebook Channel (Notify Members)

**Subscribe:** `/topic/notebook/{notebookId}/ai-tasks`

Subscribe khi vào trang notebook để nhận realtime updates.

**Message format:**

```json
{
  "aiSetId": "uuid",
  "notebookId": "uuid",
  "type": "created | done | deleted",
  "setType": "quiz | flashcards | video | audio | summary | mindmap | suggestions",
  "title": "Quiz từ 3 tài liệu",
  "status": "done",
  "createdBy": {
    "id": "uuid",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "/uploads/..."
  },
  "timestamp": "2026-01-12T00:10:00+07:00"
}
```

**Xử lý:**

- `type: "done"` → Refetch AI sets list, show toast "{user} vừa tạo xong {setType}"
- `type: "deleted"` → Remove item from list

---

## Code Example (React + SockJS + STOMP)

```typescript
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

// Kết nối
const client = new Client({
  webSocketFactory: () => new SockJS(`${API_URL}/ws?access_token=${token}`),
  onConnect: () => {
    // Subscribe Task Owner channel
    client.subscribe(`/topic/ai-task/${aiSetId}`, (msg) => {
      const data = JSON.parse(msg.body);
      handleTaskProgress(data);
    });

    // Subscribe Notebook channel
    client.subscribe(`/topic/notebook/${notebookId}/ai-tasks`, (msg) => {
      const data = JSON.parse(msg.body);
      handleNotebookNotification(data);
    });
  },
});

client.activate();

// Handlers
function handleTaskProgress(msg: AiTaskProgressMessage) {
  switch (msg.type) {
    case "progress":
      setProgress(msg.progress);
      setStatus(msg.message);
      break;
    case "done":
      setProgress(100);
      refetchData();
      toast.success("Hoàn thành!");
      break;
    case "failed":
      toast.error(msg.message);
      break;
  }
}

function handleNotebookNotification(msg: AiTaskNotification) {
  if (msg.type === "done") {
    refetchAiSets();
    toast.info(`${msg.createdBy.fullName} vừa tạo xong ${msg.setType}`);
  } else if (msg.type === "deleted") {
    removeFromList(msg.aiSetId);
  }
}
```

---

## TypeScript Types

```typescript
interface AiTaskProgressMessage {
  aiSetId: string;
  type: "progress" | "done" | "failed";
  step: string;
  progress: number;
  message: string;
  setType?: string;
  data?: Record<string, any>;
}

interface AiTaskNotification {
  aiSetId: string;
  notebookId: string;
  type: "created" | "done" | "deleted";
  setType: string;
  title: string;
  status: string;
  createdBy: {
    id: string;
    fullName: string;
    avatarUrl?: string;
  };
  timestamp: string;
}
```
