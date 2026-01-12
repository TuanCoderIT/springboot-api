# Code Exercises API - Hướng dẫn Frontend

## Base URL

```
/user/notebooks/{notebookId}/ai/code-exercises
```

---

## 1. Sync Languages (chạy 1 lần)

```
POST /languages/sync
→ {"synced": 10, "message": "..."}
```

---

## 2. Generate Bài Tập

**Endpoint:** `POST /generate`

**Request:**

```typescript
interface CodeExerciseGenerateRequest {
  fileIds: string[]; // UUID các files tài liệu
  maxExercises?: number; // 1-5, default: 3
  language?: "vi" | "en";
  additionalRequirements?: string;
}
```

**Response:**

```json
{
  "aiSetId": "uuid",
  "status": "processing"
}
```

---

## 3. Lấy Danh Sách Bài Tập

**Endpoint:** `GET /{aiSetId}`

**Response:**

```typescript
interface CodeExerciseResponse {
  id: string;
  title: string;
  description: string;
  difficulty: "easy" | "medium" | "hard";
  timeLimit: number; // seconds
  memoryLimit: number; // bytes
  language: { id: string; name: string; version: string };
  starterFiles: CodeFile[];
  sampleTestcases: Testcase[];
}
```

---

## 4. Chạy Code User

**Endpoint:** `POST /exercise/{exerciseId}/run`

**Request:**

```typescript
interface RunCodeRequest {
  languageId: string;
  files: {
    filename: string;
    content: string;
    isMain: boolean;
  }[];
}
```

**Response:**

```typescript
interface RunCodeResponse {
  status: "passed" | "failed";
  passed: number;
  failed: number;
  total: number;
  saved: boolean; // true nếu pass all
  details: {
    id: string;
    input?: string;
    expected?: string;
    output?: string;
    result: "passed" | "failed" | "runtime_error" | "time_limit_exceeded";
    isHidden: boolean;
  }[];
}
```

---

## TypeScript Types

```typescript
interface CodeExercise {
  id: string;
  title: string;
  description: string;
  difficulty: "easy" | "medium" | "hard";
  timeLimit: number;
  memoryLimit: number;
  language: LanguageInfo;
  starterFiles: CodeFile[];
  sampleTestcases: Testcase[];
}

interface LanguageInfo {
  id: string;
  name: string; // "python", "javascript"
  version: string; // "3.10.0"
}

interface CodeFile {
  id: string;
  filename: string;
  content: string;
  isMain: boolean;
}

interface Testcase {
  id: string;
  input: string;
  expectedOutput: string;
  orderIndex: number;
  isHidden: boolean;
}

interface TestResult {
  id: string;
  input?: string;
  expected?: string;
  output?: string;
  stderr?: string;
  exitCode?: number;
  cpuTime?: number;
  memory?: number;
  result: "passed" | "failed" | "runtime_error" | "time_limit_exceeded";
  isHidden: boolean;
}
```

---

## UI Flow Gợi Ý

```
┌─────────────────────────────────────┐
│       BÀI TẬP: Tính tổng 2 số       │
│  Mức độ: 🟢 Easy  | Python 3.10     │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ # main.py                       │ │
│ │ a = int(input())                │ │
│ │ b = int(input())                │ │
│ │ # TODO: in tổng a + b           │ │
│ └─────────────────────────────────┘ │
│                                     │
│  TEST CASES:                        │
│  ┌───────┬────────┬─────────────┐   │
│  │ Input │ Output │ Status      │   │
│  ├───────┼────────┼─────────────┤   │
│  │ 1 2   │ 3      │ ✅ Passed   │   │
│  │ 5 7   │ 12     │ ✅ Passed   │   │
│  │ ???   │ ???    │ 🔒 Hidden   │   │
│  └───────┴────────┴─────────────┘   │
│                                     │
│     [▶ Chạy Code]   [📤 Nộp bài]    │
└─────────────────────────────────────┘
```

---

## Piston Config

```
Piston URL: http://localhost:2000
```

Đảm bảo Piston container đang chạy:

```bash
docker ps | grep piston
```
