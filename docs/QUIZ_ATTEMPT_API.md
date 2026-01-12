# Quiz Attempt & Analysis API - Hướng dẫn Frontend

## Base URL

```
/user/notebooks/{notebookId}/ai/quiz
```

---

## 1. Submit Quiz Attempt

**Endpoint:** `POST /{aiSetId}/attempts`

**Request:**

```typescript
interface SubmitAttemptRequest {
  startedAt?: string; // ISO 8601
  finishedAt?: string; // ISO 8601
  timeSpentSeconds?: number;
  answers: {
    quizId: string; // UUID của câu hỏi
    selectedOptionId: string; // UUID option được chọn
  }[];
}
```

**Response:**

```typescript
interface AttemptResponse {
  id: string;
  aiSetId: string;
  score: number; // 0-100
  totalQuestions: number;
  correctCount: number;
  timeSpentSeconds?: number;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  hasAnalysis: boolean; // true nếu đã có AI analysis
  answers?: AttemptAnswerDetail[];
}
```

---

## 2. Lấy lịch sử làm bài

**Endpoint:** `GET /{aiSetId}/attempts`

**Response:** `AttemptResponse[]`

---

## 3. Lấy chi tiết một attempt

**Endpoint:** `GET /attempts/{attemptId}`

**Response:** `AttemptResponse` với `answers` đầy đủ

---

## 4. AI Phân tích kết quả

**Endpoint:** `POST /attempts/{attemptId}/analyze`

**Response:**

```typescript
interface QuizAnalysisResponse {
  scoreText: string; // "7/10 (70%)"
  summary: string; // Tóm tắt tiến bộ
  strengths: TopicAnalysis[];
  weaknesses: TopicAnalysis[];
  improvements: TopicAnalysis[]; // Chủ đề đã cải thiện
  recommendations: string[];
}

interface TopicAnalysis {
  topic: string;
  analysis: string;
  suggestions: string[];
}
```

---

## 5. Lấy analysis đã lưu

**Endpoint:** `GET /attempts/{attemptId}/analysis`

**Response:** `QuizAnalysisResponse` hoặc `204 No Content`

---

## TypeScript Types

```typescript
// Request types
interface SubmitAttemptRequest {
  startedAt?: string;
  finishedAt?: string;
  timeSpentSeconds?: number;
  answers: AnswerItem[];
}

interface AnswerItem {
  quizId: string;
  selectedOptionId: string;
}

// Response types
interface AttemptResponse {
  id: string;
  aiSetId: string;
  score: number;
  totalQuestions: number;
  correctCount: number;
  timeSpentSeconds?: number;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  hasAnalysis: boolean;
  answers?: AttemptAnswerDetail[];
}

interface AttemptAnswerDetail {
  quizId: string;
  question: string;
  selectedOptionId?: string;
  selectedOptionText?: string;
  correctOptionId: string;
  correctOptionText: string;
  isCorrect: boolean;
}

interface QuizAnalysisResponse {
  scoreText: string;
  summary: string;
  strengths: TopicAnalysis[];
  weaknesses: TopicAnalysis[];
  improvements: TopicAnalysis[];
  recommendations: string[];
}

interface TopicAnalysis {
  topic: string;
  analysis: string;
  suggestions: string[];
}
```

---

## Flow UI gợi ý

```
┌─────────────────────────────────────┐
│         KẾT QUẢ QUIZ                │
│                                     │
│    ┌───────────────────────┐        │
│    │   7/10  (70%)         │        │
│    │   ████████░░          │        │
│    └───────────────────────┘        │
│                                     │
│  [Xem chi tiết]  [🧠 Phân tích AI]  │
└─────────────────────────────────────┘
          │
          ▼ Bấm "Phân tích AI"
┌─────────────────────────────────────┐
│      PHÂN TÍCH TỪ AI 🧠            │
│                                     │
│ 📊 Tóm tắt:                        │
│ "Bạn tăng 2 câu so với lần trước!" │
│                                     │
│ ✅ Đã cải thiện:                   │
│ • SSL/TLS - Trước sai, nay đúng    │
│                                     │
│ ⚠️ Cần ôn lại:                     │
│ • RSA - Sai 3 lần liên tiếp        │
│   → Xem lại chương 5               │
│                                     │
│ 💡 Gợi ý:                          │
│ • Tập trung ôn bảo mật mạng        │
└─────────────────────────────────────┘
```

---

## Example API Calls

```typescript
// 1. Submit quiz result
const submitAttempt = async (
  notebookId: string,
  aiSetId: string,
  answers: AnswerItem[]
) => {
  const res = await fetch(
    `/user/notebooks/${notebookId}/ai/quiz/${aiSetId}/attempts`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        startedAt: startTime.toISOString(),
        finishedAt: new Date().toISOString(),
        timeSpentSeconds: Math.floor((Date.now() - startTime) / 1000),
        answers,
      }),
    }
  );
  return res.json() as AttemptResponse;
};

// 2. Get AI analysis
const getAnalysis = async (notebookId: string, attemptId: string) => {
  const res = await fetch(
    `/user/notebooks/${notebookId}/ai/quiz/attempts/${attemptId}/analyze`,
    { method: "POST", headers: { Authorization: `Bearer ${token}` } }
  );
  return res.json() as QuizAnalysisResponse;
};
```
