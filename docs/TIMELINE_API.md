# Timeline API - Frontend Guide

## Endpoints

### 1. Tạo Timeline (Async)

```
POST /user/notebooks/{notebookId}/ai/timeline/generate
```

**Query Params:**
| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|-------|
| `fileIds` | UUID[] | ✅ | - | List file IDs |
| `mode` | string | ❌ | `logic` | `time` hoặc `logic` |
| `maxEvents` | number | ❌ | 25 | Số events tối đa |
| `additionalRequirements` | string | ❌ | - | Yêu cầu bổ sung |

**Response:**

```json
{
  "aiSetId": "uuid",
  "status": "queued",
  "success": true,
  "message": "Timeline đang được tạo. Dùng aiSetId để theo dõi."
}
```

---

### 2. Lấy Timeline

```
GET /user/notebooks/{notebookId}/ai/timeline/{aiSetId}
```

**Response:**

```json
{
  "aiSetId": "uuid",
  "title": "Tiến trình phát triển Internet",
  "mode": "logic",
  "totalEvents": 10,
  "status": "done",
  "createdAt": "2026-01-12T00:00:00Z",
  "events": [
    {
      "id": "uuid",
      "order": 1,
      "date": "1969",
      "datePrecision": "year",
      "title": "ARPANET ra đời",
      "description": "Mạng máy tính đầu tiên...",
      "importance": "critical",
      "icon": "network"
    }
  ],
  "createdBy": {
    "id": "uuid",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "/uploads/..."
  }
}
```

---

## TypeScript Types

```typescript
interface TimelineGenerateParams {
  fileIds: string[];
  mode?: "time" | "logic";
  maxEvents?: number;
  additionalRequirements?: string;
}

interface TimelineEvent {
  id: string;
  order: number;
  date: string;
  datePrecision: "year" | "month" | "day" | "unknown";
  title: string;
  description: string;
  importance: "minor" | "normal" | "major" | "critical";
  icon?: string;
}

interface TimelineResponse {
  aiSetId: string;
  title: string;
  mode: string;
  totalEvents: number;
  status: "queued" | "processing" | "done" | "failed";
  createdAt: string;
  events: TimelineEvent[];
  createdBy: {
    id: string;
    fullName: string;
    avatarUrl?: string;
  };
}
```

---

## Workflow

```
1. POST /timeline/generate → nhận aiSetId
2. Subscribe WebSocket: /topic/ai-task/{aiSetId}
3. Khi nhận status=done → GET /timeline/{aiSetId}
4. Render timeline events
```

---

## Icon Options

| Icon        | Mô tả          |
| ----------- | -------------- |
| `history`   | Lịch sử        |
| `network`   | Mạng/Kết nối   |
| `protocol`  | Giao thức      |
| `release`   | Phát hành      |
| `concept`   | Khái niệm      |
| `law`       | Luật/Quy định  |
| `event`     | Sự kiện chung  |
| `warning`   | Cảnh báo       |
| `milestone` | Mốc quan trọng |
| `process`   | Quy trình      |

---

## Importance Levels

| Level      | Mô tả          | Gợi ý UI                  |
| ---------- | -------------- | ------------------------- |
| `minor`    | Sự kiện phụ    | Size nhỏ, màu xám         |
| `normal`   | Bình thường    | Size trung bình, màu xanh |
| `major`    | Quan trọng     | Size lớn, màu cam         |
| `critical` | Rất quan trọng | Size lớn, highlight đỏ    |
