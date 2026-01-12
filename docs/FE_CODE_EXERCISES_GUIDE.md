# Hướng dẫn API Code Exercises (AI Auto-Gen) cho Frontend

Tài liệu hướng dẫn tích hợp tính năng "Bài tập lập trình tự động" - sinh ra từ tài liệu bằng AI, có kiểm thử code mẫu trước khi lưu.

**Base URL**: `/user/notebooks/{notebookId}/ai/code-exercises`

---

## 🏗️ 1. Quy trình tích hợp (Workflow)

1.  **Sinh bài tập**: User chọn tài liệu → Gọi API `Generate`.
    - Backend tự động phân tích tài liệu để chọn ngôn ngữ (VD: tài liệu C++ → sinh bài C++).
    - Backend tự động chạy thử code mẫu (solution). Chỉ bài nào Pass 100% testcase mới được lưu.
2.  **Làm bài**: Hiển thị danh sách bài tập.
    - User viết code → Gọi API `Run Code` (không cần gửi `languageId`).
    - Hệ thống chấm điểm dựa trên Testcases ẩn/hiện.
3.  **Gợi ý**: Nếu User bế tắc, gọi API `Get Solution` để lấy code mẫu tham khảo.

---

## 🔌 2. API Endpoints

### 2.1. Sync Languages (Admin/Init)

Đồng bộ danh sách ngôn ngữ từ Execution Engine (Piston).

- **POST** `/languages/sync`
- **Body**: `{}`
- **Response**: `{ "synced": 15, "message": "..." }`

### 2.2. Get Supported Languages

Lấy danh sách ngôn ngữ (để hiển thị filter hoặc dropdown nếu cần).

- **GET** `/languages`
- **Response**: `[ { "id": "...", "name": "python", "version": "3.10" }, ... ]`

### 2.3. Generate Exercises (Sinh bài tập)

- **POST** `/generate`
- **Body**:
  ```json
  {
    "fileIds": ["uuid-file-1", "uuid-file-2"],
    "prompt": "Tạo bài tập về quy hoạch động", // Optional
    "difficulty": "MEDIUM",
    "count": 3
    // Không cần gửi "language". AI tự detect từ tài liệu.
  }
  ```
- **Response**: `{ "aiSetId": "uuid...", "status": "processing" }`

### 2.4. Get Exercises List (Danh sách câu hỏi)

- **GET** `/{aiSetId}`
- **Response**:
  ```json
  [
    {
      "id": "uuid-bai-tap",
      "title": "Fibonacci",
      "language": { "name": "python", "version": "3.10" }, // Ngôn ngữ của bài
      "files": [{ "role": "starter", "content": "def fib(n):\n  pass" }],
      "sampleTestcases": [{ "input": "5", "expectedOutput": "5" }]
    }
  ]
  ```

### 2.5. Run Code (Chấm bài)

user submit code của họ.

- **POST** `/exercise/{exerciseId}/run`
- **Body**:
  ```json
  {
    // "languageId": BỎ QUA - Backend tự lấy theo bài tập
    "files": [
      {
        "filename": "main.py",
        "content": "def fib(n): return n if n<2 else fib(n-1)+fib(n-2)",
        "isMain": true
      }
    ]
  }
  ```
- **Response**:
  ```json
  {
    "status": "passed", // passed | failed | runtime_error
    "passed": 5,        // Số testcase đúng
    "total": 5,         // Tổng testcase
    "details": [ ... ]
  }
  ```

### 2.6. Get Solution (Lấy code mẫu)

Gọi khi user muốn xem đáp án (Gợi ý).

- **GET** `/exercise/{exerciseId}/solution`
- **Response**:
  ```json
  [
    {
      "filename": "main.py",
      "content": "Full solution code here...",
      "role": "solution"
    }
  ]
  ```

---

💡 **Note**:

- Các logic phức tạp (chọn ngôn ngữ, validate solution) đã được xử lý ngầm ở Backend. FE chỉ cần gọi API đơn giản.
- Nếu `Generate` trả về lỗi 500, vui lòng báo lại Backend check log (có thể do DB config).
