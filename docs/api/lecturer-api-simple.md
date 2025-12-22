# 📚 API Reference - Lecturer

> **Base URL:** `/lecturer/`  
> **Auth:** Bearer Token (role TEACHER)

---

## 1. Học kỳ (Terms)

### GET /lecturer/terms

Lấy danh sách học kỳ **còn khả dụng** (endDate >= hôm nay).

**Query Params:**

| Param      | Type    | Default | Mô tả                   |
| ---------- | ------- | ------- | ----------------------- |
| `page`     | number  | 0       | Trang (bắt đầu từ 0)    |
| `size`     | number  | 10      | Số item mỗi trang       |
| `q`        | string  | -       | Tìm theo mã, tên học kỳ |
| `isActive` | boolean | -       | Filter theo trạng thái  |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "code": "2024_HK1",
      "name": "Học kỳ 1 - Năm học 2024-2025",
      "startDate": "2024-09-01",
      "endDate": "2025-01-15",
      "isActive": true,
      "totalAssignments": 150
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

### GET /lecturer/terms/{id}

Lấy chi tiết 1 học kỳ.

**Path Params:** `id` (UUID)

**Response:** Object `TermResponse` (như trên, không có wrapper `data`)

---

## 2. Ngành học (Majors)

### GET /lecturer/majors

Lấy danh sách ngành học.

**Query Params:**

| Param       | Type    | Default | Mô tả                     |
| ----------- | ------- | ------- | ------------------------- |
| `page`      | number  | 0       | Trang                     |
| `size`      | number  | 10      | Số item mỗi trang         |
| `q`         | string  | -       | Tìm theo mã, tên ngành    |
| `isActive`  | boolean | -       | Filter theo trạng thái    |
| `orgUnitId` | UUID    | -       | Filter theo đơn vị (khoa) |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "code": "CNTT",
      "name": "Công nghệ Thông tin",
      "isActive": true,
      "orgUnit": { "id": "uuid", "code": "KHOA_CNTT", "name": "Khoa CNTT" },
      "subjectCount": 45,
      "studentCount": 500
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

### GET /lecturer/majors/{id}

Lấy chi tiết 1 ngành học.

**Path Params:** `id` (UUID)

**Response:** Object `MajorResponse`

---

## 3. Môn học (Subjects)

### GET /lecturer/subjects

Lấy danh sách môn học.

**Query Params:**

| Param      | Type    | Default | Mô tả                  |
| ---------- | ------- | ------- | ---------------------- |
| `page`     | number  | 0       | Trang                  |
| `size`     | number  | 10      | Số item mỗi trang      |
| `q`        | string  | -       | Tìm theo mã, tên môn   |
| `isActive` | boolean | -       | Filter theo trạng thái |
| `majorId`  | UUID    | -       | **Filter theo ngành**  |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "code": "INF30087",
      "name": "Cấu trúc dữ liệu và giải thuật",
      "credit": 3,
      "isActive": true,
      "majorCount": 2,
      "assignmentCount": 5,
      "studentCount": 120
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

### GET /lecturer/subjects/{id}

Lấy chi tiết 1 môn học.

**Path Params:** `id` (UUID)

**Response:** Object `SubjectResponse`

---

## 4. Phân công giảng dạy

### GET /lecturer/teaching-assignments

Lấy danh sách phân công giảng dạy của giảng viên.

**Query Params:**

| Param        | Type   | Default | Mô tả                            |
| ------------ | ------ | ------- | -------------------------------- |
| `page`       | number | 0       | Trang                            |
| `size`       | number | 10      | Số item mỗi trang                |
| `termId`     | UUID   | -       | Filter theo học kỳ               |
| `status`     | string | -       | Filter theo status (APPROVED...) |
| `termStatus` | string | -       | Filter: ACTIVE, UPCOMING, PAST   |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "subjectId": "uuid",
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu",
      "subjectCredit": 3,
      "termId": "uuid",
      "termCode": "2024_HK1",
      "termName": "Học kỳ 1 - 2024-2025",
      "termStartDate": "2024-09-01",
      "termEndDate": "2025-01-15",
      "status": "ACTIVE",
      "approvalStatus": "APPROVED",
      "classCount": 3,
      "studentCount": 120,
      "fileCount": 25,
      "quizCount": 50,
      "flashcardCount": 100,
      "summaryCount": 10,
      "videoCount": 5,
      "note": "Ghi chú phân công",
      "notebookId": "uuid",
      "createdAt": "2024-09-01T00:00:00+07:00",
      "termStatus": "ACTIVE"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 5, "totalPages": 1 }
}
```

### GET /lecturer/teaching-assignments/{assignmentId}

Lấy **chi tiết đầy đủ** 1 phân công giảng dạy.

**Path Params:** `assignmentId` (UUID)

**Response:**

```json
{
  "id": "uuid",
  "subjectId": "uuid",
  "subjectCode": "INF30087",
  "subjectName": "Cấu trúc dữ liệu",
  "subjectCredit": 3,
  "termId": "uuid",
  "termCode": "2024_HK1",
  "termName": "Học kỳ 1 - 2024-2025",
  "termStartDate": "2024-09-01",
  "termEndDate": "2025-01-15",
  "termIsActive": true,
  "status": "ACTIVE",
  "approvalStatus": "APPROVED",
  "termStatus": "ACTIVE",
  "classCount": 3,
  "studentCount": 120,
  "fileCount": 25,
  "quizCount": 50,
  "flashcardCount": 100,
  "summaryCount": 10,
  "videoCount": 0,
  "notebookId": "uuid",
  "notebookTitle": "Cấu trúc dữ liệu - HK1 2024",
  "notebookDescription": "Tài liệu môn CTDL",
  "notebookThumbnailUrl": "https://...",
  "notebookCreatedAt": "2024-09-01T00:00:00+07:00",
  "notebookUpdatedAt": "2024-09-15T00:00:00+07:00",
  "note": "Ghi chú phân công",
  "createdBy": "ADMIN",
  "createdAt": "2024-09-01T00:00:00+07:00",
  "recentClasses": [
    {
      "id": "uuid",
      "classCode": "INF30087-01",
      "room": "A201",
      "dayOfWeek": 2,
      "periods": "1-3",
      "studentCount": 45,
      "isActive": true
    }
  ]
}
```

### POST /lecturer/teaching-assignments/request

Gửi yêu cầu xin dạy môn trong học kỳ.

**Request Body:**

```json
{
  "termId": "uuid",
  "subjectId": "uuid",
  "note": "string" // optional
}
```

**Response 200:**

```json
{
  "id": "uuid",
  "subjectCode": "INF30087",
  "subjectName": "Cấu trúc dữ liệu và giải thuật",
  "termName": "Học kỳ 1 - 2024-2025",
  "approvalStatus": "PENDING",
  "classCount": 0,
  "studentCount": 0
}
```

**Error 400:** `"Bạn đã đăng ký dạy môn này trong học kỳ này rồi"`

---

## 5. Lớp học phần của phân công

### GET /lecturer/teaching-assignments/{assignmentId}/classes

Lấy danh sách lớp học phần của một phân công.

**Path Params:** `assignmentId` (UUID)

**Query Params:**

| Param     | Type   | Default   | Mô tả                    |
| --------- | ------ | --------- | ------------------------ |
| `q`       | string | -         | Tìm theo mã lớp, tên môn |
| `page`    | number | 0         | Trang                    |
| `size`    | number | 10        | Số item mỗi trang        |
| `sortBy`  | string | classCode | Sắp xếp theo field       |
| `sortDir` | string | asc       | Hướng sắp xếp            |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "classCode": "INF30087-01",
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu",
      "termName": "Học kỳ 1 - 2024-2025",
      "room": "A201",
      "dayOfWeek": 2,
      "periods": "1-3",
      "startDate": "2024-09-01",
      "endDate": "2024-12-31",
      "note": "Lớp buổi sáng",
      "isActive": true,
      "studentCount": 45,
      "createdAt": "2024-09-01T00:00:00+07:00",
      "updatedAt": "2024-09-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 3, "totalPages": 1 }
}
```

---

## 6. Sinh viên của phân công

### GET /lecturer/teaching-assignments/{assignmentId}/students

Lấy sinh viên trong 1 phân công (có thể lọc theo lớp cụ thể).

**Path Params:** `assignmentId` (UUID)

**Query Params:**

| Param     | Type   | Default     | Mô tả                   |
| --------- | ------ | ----------- | ----------------------- |
| `classId` | UUID   | -           | **Lọc theo lớp cụ thể** |
| `q`       | string | -           | Tìm theo mã SV, họ tên  |
| `page`    | number | 0           | Trang                   |
| `size`    | number | 10          | Số item mỗi trang       |
| `sortBy`  | string | studentCode | Sắp xếp theo field      |
| `sortDir` | string | asc         | Hướng sắp xếp           |

**Response:** Giống API 7

---

## 7. Thành viên của 1 lớp cụ thể

### GET /lecturer/classes/{classId}/members

Lấy danh sách sinh viên trong **1 lớp cụ thể**.

**Path Params:** `classId` (UUID)

**Query Params:**

| Param     | Type   | Default     | Mô tả                  |
| --------- | ------ | ----------- | ---------------------- |
| `q`       | string | -           | Tìm theo mã SV, họ tên |
| `page`    | number | 0           | Trang                  |
| `size`    | number | 10          | Số item mỗi trang      |
| `sortBy`  | string | studentCode | Sắp xếp theo field     |
| `sortDir` | string | asc         | Hướng sắp xếp          |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "studentCode": "20110001",
      "fullName": "Nguyễn Văn A",
      "firstName": "A",
      "lastName": "Nguyễn Văn",
      "dob": "2002-05-15",
      "classCode": "INF30087-01",
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu",
      "termName": "Học kỳ 1 - 2024-2025",
      "createdAt": "2024-09-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 45, "totalPages": 5 }
}
```

---

## 7. Thành viên của 1 lớp cụ thể

### GET /lecturer/classes/{classId}/members

Lấy danh sách sinh viên trong **1 lớp cụ thể**.

**Path Params:** `classId` (UUID)

**Query Params:**

| Param     | Type   | Default     | Mô tả                  |
| --------- | ------ | ----------- | ---------------------- |
| `q`       | string | -           | Tìm theo mã SV, họ tên |
| `page`    | number | 0           | Trang                  |
| `size`    | number | 10          | Số item mỗi trang      |
| `sortBy`  | string | studentCode | Sắp xếp theo field     |
| `sortDir` | string | asc         | Hướng sắp xếp          |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "studentCode": "20110001",
      "fullName": "Nguyễn Văn A",
      "firstName": "A",
      "lastName": "Nguyễn Văn",
      "dob": "2002-05-15",
      "classCode": "INF30087-01",
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu",
      "termName": "Học kỳ 1 - 2024-2025",
      "createdAt": "2024-09-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 45, "totalPages": 5 }
}
```

---

## 8. Quản lý Chương (Chapters) - Cho dndkit

### GET /lecturer/notebooks/{notebookId}/chapters

Lấy danh sách chương của Notebook (đã sắp xếp theo `sortOrder`).

**Path Params:** `notebookId` (UUID)

**Response:**

```json
[
  {
    "id": "uuid",
    "title": "Chương 1: Giới thiệu",
    "description": "Mô tả chương",
    "sortOrder": 0,
    "createdAt": "2024-12-20T10:00:00Z",
    "updatedAt": "2024-12-20T10:00:00Z"
  },
  {
    "id": "uuid",
    "title": "Chương 2: Cài đặt môi trường",
    "description": null,
    "sortOrder": 1,
    "createdAt": "2024-12-20T10:05:00Z",
    "updatedAt": "2024-12-20T10:05:00Z"
  }
]
```

### POST /lecturer/notebooks/{notebookId}/chapters

Tạo chương mới (thêm vào cuối danh sách).

**Path Params:** `notebookId` (UUID)

**Request Body:**

```json
{
  "title": "Chương mới"
}
```

**Response:** Object `ChapterResponse` (như trên).

### PUT /lecturer/chapters/{chapterId}

Cập nhật thông tin chương.

**Path Params:** `chapterId` (UUID)

**Request Body:**

```json
{
  "title": "Tên mới",
  "description": "Mô tả mới"
}
```

**Response:** Object `ChapterResponse`.

### DELETE /lecturer/chapters/{chapterId}

Xóa chương.

**Path Params:** `chapterId` (UUID)

**Response:** 204 No Content.

### PUT /lecturer/notebooks/{notebookId}/chapters/reorder

Sắp xếp lại thứ tự chương (dùng cho DnD Kit).

**Path Params:** `notebookId` (UUID)

**Request Body:**

```json
{
  "orderedIds": ["chapter-uuid-1", "chapter-uuid-2", "chapter-uuid-3"]
}
```

**Response:** 200 OK.

---

## 9. Quản lý Nội dung Chương (Chapter Items)

### Item Types

| Type         | Mô tả                             | ref_id trỏ đến         |
| ------------ | --------------------------------- | ---------------------- |
| `FILE`       | File tài liệu (PDF, Word, PPT)    | `notebook_files.id`    |
| `LECTURE`    | Bài giảng                         | `lectures.id` (nếu có) |
| `QUIZ`       | Câu hỏi trắc nghiệm               | `notebook_quizzes.id`  |
| `ASSIGNMENT` | Bài tập                           | `assignments.id`       |
| `NOTE`       | Ghi chú (nội dung trong metadata) | null                   |
| `VIDEO`      | Video                             | `video_assets.id`      |
| `FLASHCARD`  | Bộ flashcard                      | `notebook_ai_sets.id`  |

### GET /lecturer/chapters/{chapterId}/items

Lấy danh sách item trong chương (đã sắp xếp theo `sortOrder`).

**Path Params:** `chapterId` (UUID)

**Response:**

```json
[
  {
    "id": "uuid",
    "itemType": "FILE",
    "refId": "notebook-file-uuid",
    "title": "Bài giảng Chương 1.pdf",
    "sortOrder": 0,
    "metadata": {
      "mimeType": "application/pdf",
      "fileSize": 1024000,
      "storageUrl": "/uploads/..."
    },
    "createdAt": "2024-12-22T10:00:00Z"
  },
  {
    "id": "uuid",
    "itemType": "NOTE",
    "refId": null,
    "title": "Ghi chú quan trọng",
    "sortOrder": 1,
    "metadata": {
      "content": "Nội dung ghi chú..."
    },
    "createdAt": "2024-12-22T10:05:00Z"
  }
]
```

### POST /lecturer/chapters/{chapterId}/items

Tạo item mới (trừ FILE - dùng API upload riêng).

**Path Params:** `chapterId` (UUID)

**Request Body:**

```json
{
  "itemType": "NOTE",
  "refId": null,
  "title": "Ghi chú quan trọng",
  "metadata": {
    "content": "Nội dung ghi chú..."
  }
}
```

**Response:** Object `ChapterItemResponse`.

### PUT /lecturer/chapter-items/{itemId}

Cập nhật item (title, metadata).

**Path Params:** `itemId` (UUID)

**Request Body:**

```json
{
  "title": "Tiêu đề mới",
  "metadata": {
    "content": "Nội dung mới..."
  }
}
```

**Response:** Object `ChapterItemResponse`.

### POST /lecturer/chapters/{chapterId}/files

Upload file vào chương (itemType=FILE). File sẽ được lưu vào `NotebookFile`, tạo `ChapterItem` tham chiếu, và tự động chạy AI processing.

**Path Params:** `chapterId` (UUID)

**Content-Type:** `multipart/form-data`

**Form Fields:**

| Field          | Type   | Default | Mô tả                            |
| -------------- | ------ | ------- | -------------------------------- |
| `files`        | File[] | -       | Danh sách file (PDF, Word, PPT)  |
| `chunkSize`    | number | 3000    | Kích thước chunk (3000-5000)     |
| `chunkOverlap` | number | 250     | Overlap giữa các chunk (200-500) |

**Response:** Array of `ChapterItemResponse`.

### DELETE /lecturer/chapter-items/{itemId}

Xóa item khỏi chương. Nếu là FILE, sẽ xóa cả `NotebookFile` liên quan.

**Path Params:** `itemId` (UUID)

**Response:** 204 No Content.

### PUT /lecturer/chapters/{chapterId}/items/reorder

Sắp xếp lại thứ tự item trong chương (dùng cho DnD Kit).

**Path Params:** `chapterId` (UUID)

**Request Body:**

```json
{
  "orderedIds": ["item-uuid-1", "item-uuid-2", "item-uuid-3"]
}
```

**Response:** 200 OK.

```

```
