# API Notebook Cá Nhân - Hướng Dẫn Cho Frontend

## Tổng quát

API này cho phép user tạo và quản lý notebook cá nhân (personal notebook).

- **Base URL**: `/user/notebooks`
- **Authentication**: Bearer Token (JWT)
- **Content-Type**: `multipart/form-data` (cho POST, PUT có upload file) hoặc `application/json`

---

## 1. Tạo Notebook Cá Nhân

API hỗ trợ **2 mode** tạo notebook:

### 🔹 MODE 1: Manual (Nhập thủ công)

Người dùng tự nhập title và upload thumbnail.

**Yêu cầu:**

- `title`: Bắt buộc
- `thumbnail`: Bắt buộc (file ảnh)
- `autoGenerate`: `false` hoặc không truyền

### 🔹 MODE 2: Auto-generate (Tự động tạo bằng AI)

Chỉ cần nhập mô tả về notebook (≥10 từ), hệ thống sẽ:

1. **Search web** để lấy thông tin context
2. **Call AI (Gemini)** để generate:
   - `title`: Tiêu đề ngắn gọn, hấp dẫn
   - `description`: Mô tả chi tiết bằng Markdown
   - `imageUrl`: URL hình ảnh liên quan

**Yêu cầu:**

- `description`: Bắt buộc (tối thiểu 10 từ)
- `autoGenerate`: `true`
- `title`: Không cần (AI tự tạo)
- `thumbnail`: Không cần (AI tự tìm)

---

### Endpoint

```
POST /user/personal-notebooks
```

### Content-Type

```
multipart/form-data
```

### Request Body

| Field       | Type          | Mode 1 (Manual) | Mode 2 (Auto) | Description                |
| ----------- | ------------- | --------------- | ------------- | -------------------------- |
| `data`      | JSON (string) | ✅              | ✅            | Object chứa các field dưới |
| `thumbnail` | File (image)  | ✅              | ❌            | Ảnh thumbnail cho notebook |

**Cấu trúc `data`:**

```json
// MODE 1: Manual
{
  "title": "Notebook của tôi",
  "description": "Mô tả notebook (optional)",
  "autoGenerate": false
}

// MODE 2: Auto-generate
{
  "description": "Tôi muốn học về machine learning và deep learning, bao gồm các khái niệm cơ bản, thuật toán phổ biến, và ứng dụng thực tế trong công việc.",
  "autoGenerate": true
}
```

### Validation Rules

| Field          | Mode 1 (Manual)                   | Mode 2 (Auto)             |
| -------------- | --------------------------------- | ------------------------- |
| `title`        | Bắt buộc, tối đa 255 ký tự        | Không cần (AI tự tạo)     |
| `description`  | Không bắt buộc, tối đa 5000 ký tự | Bắt buộc, tối thiểu 10 từ |
| `thumbnail`    | Bắt buộc                          | Không cần (AI tự tìm)     |
| `autoGenerate` | `false` hoặc không truyền         | `true`                    |

### Response (201 Created)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Machine Learning & Deep Learning Fundamentals",
  "description": "## Tổng quan\n\nNotebook này tập trung vào **Machine Learning** và **Deep Learning**...\n\n### Nội dung chính\n- Khái niệm cơ bản\n- Thuật toán phổ biến\n- Ứng dụng thực tế\n\n...",
  "type": "personal",
  "visibility": "private",
  "thumbnailUrl": "https://example.com/ml-image.jpg",
  "fileCount": 0,
  "createdAt": "2025-12-11T12:34:56+07:00",
  "updatedAt": "2025-12-11T12:34:56+07:00"
}
```

**Note cho Mode 2:**

- `title`: AI tự generate dựa trên mô tả
- `description`: AI tự viết bằng Markdown chi tiết
- `thumbnailUrl`: URL hình ảnh từ web (có thể `null` nếu không tìm được)

### Frontend Examples

#### MODE 1: Manual (React + fetch)

```typescript
const createNotebookManual = async (
  title: string,
  description: string,
  thumbnail: File
) => {
  const formData = new FormData();

  const data = { title, description, autoGenerate: false };
  formData.append(
    "data",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );
  formData.append("thumbnail", thumbnail);

  const response = await fetch("/user/personal-notebooks", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  return response.json();
};
```

#### MODE 2: Auto-generate (React + fetch)

```typescript
const createNotebookAuto = async (description: string) => {
  const formData = new FormData();

  // Chỉ cần description, không cần title và thumbnail
  const data = { description, autoGenerate: true };
  formData.append(
    "data",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );

  const response = await fetch("/user/personal-notebooks", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  // Response sẽ có title và description được AI generate
  return response.json();
};
```

---

## 2. Cập Nhật Notebook Cá Nhân

### Endpoint

```
PUT /user/personal-notebooks/{notebookId}
```

### Content-Type

```
multipart/form-data
```

### Request Body

| Field       | Type          | Required | Description                                     |
| ----------- | ------------- | -------- | ----------------------------------------------- |
| `data`      | JSON (string) | ✅       | Object chứa `title` và `description`            |
| `thumbnail` | File (image)  | ❌       | Ảnh thumbnail mới (giữ nguyên nếu không truyền) |

### Response (200 OK)

Giống như response tạo mới.

### Note

- Chỉ **owner** mới có thể cập nhật notebook
- Thumbnail mới sẽ thay thế thumbnail cũ (nếu có)
- `autoGenerate` không áp dụng cho cập nhật

---

## 3. Xóa Notebook Cá Nhân

### Endpoint

```
DELETE /user/personal-notebooks/{notebookId}
```

### Response

- **204 No Content**: Xóa thành công
- **403 Forbidden**: Không phải owner
- **404 Not Found**: Notebook không tồn tại

---

## 4. Lấy Chi Tiết Notebook

### Endpoint

```
GET /user/personal-notebooks/{notebookId}
```

### Response (200 OK)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Notebook của tôi",
  "description": "## Mô tả\n\nNội dung markdown...",
  "type": "personal",
  "visibility": "private",
  "thumbnailUrl": "http://localhost:8386/uploads/abc123.jpg",
  "fileCount": 5,
  "createdAt": "2025-12-11T12:34:56+07:00",
  "updatedAt": "2025-12-11T12:34:56+07:00"
}
```

---

## 5. Lấy Danh Sách Notebook Cá Nhân

### Endpoint

```
GET /user/personal-notebooks
```

### Query Parameters

| Param     | Type   | Default     | Description                          |
| --------- | ------ | ----------- | ------------------------------------ |
| `q`       | string | -           | Tìm kiếm theo title hoặc description |
| `sortBy`  | string | `createdAt` | Sắp xếp theo field                   |
| `sortDir` | string | `desc`      | Hướng sắp xếp: `asc` hoặc `desc`     |
| `page`    | int    | `0`         | Số trang (0-indexed)                 |
| `size`    | int    | `10`        | Số item mỗi trang                    |

### Response (200 OK)

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Notebook 1",
      "description": "Mô tả 1",
      "type": "personal",
      "visibility": "private",
      "thumbnailUrl": "http://localhost:8386/uploads/abc123.jpg",
      "fileCount": 5,
      "createdAt": "2025-12-11T12:34:56+07:00",
      "updatedAt": "2025-12-11T12:34:56+07:00"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## Error Responses

### 400 Bad Request

```json
// Manual mode thiếu title
{
  "status": 400,
  "message": "Tiêu đề là bắt buộc khi tạo notebook thủ công",
  "timestamp": "2025-12-11T12:34:56"
}

// Manual mode thiếu thumbnail
{
  "status": 400,
  "message": "Thumbnail là bắt buộc khi tạo notebook thủ công",
  "timestamp": "2025-12-11T12:34:56"
}

// Auto mode description quá ngắn
{
  "status": 400,
  "message": "Mô tả phải có ít nhất 10 từ để sử dụng chế độ tự động tạo (hiện tại: 5 từ)",
  "timestamp": "2025-12-11T12:34:56"
}

// Auto mode lỗi AI
{
  "status": 400,
  "message": "Có lỗi khi tạo nội dung tự động. Vui lòng thử lại hoặc tạo thủ công.",
  "timestamp": "2025-12-11T12:34:56"
}
```

### 403 Forbidden

```json
{
  "status": 403,
  "message": "Chỉ chủ sở hữu mới có thể chỉnh sửa notebook",
  "timestamp": "2025-12-11T12:34:56"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Notebook không tồn tại",
  "timestamp": "2025-12-11T12:34:56"
}
```

---

## Notes

1. **Notebook cá nhân vs Community notebook**:

   - Personal notebook: `type = "personal"`, `visibility = "private"`
   - Community notebook: `type = "community"`, `visibility = "public"` hoặc `"private"`

2. **Auto-generate mode (AI)**:

   - Sử dụng **Google Custom Search API** để tìm kiếm thông tin context
   - Sử dụng **Gemini AI** để generate title, description (markdown), và imageUrl
   - AI sẽ viết description bằng Markdown với heading, bullet points, định dạng đẹp
   - `thumbnailUrl` có thể là `null` nếu AI không tìm được hình phù hợp

3. **Thumbnail**:

   - MODE 1: Upload lên server, trả về URL local
   - MODE 2: AI trả về URL hình ảnh từ web (hoặc null)

4. **Authorization**:
   - Tất cả API đều yêu cầu Bearer Token
   - Chỉ owner mới có quyền UPDATE/DELETE notebook cá nhân
