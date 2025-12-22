# Lecturer Teaching Assignment API

Tài liệu hướng dẫn tích hợp API quản lý Phân công Giảng dạy (Teaching Assignment) dành cho Giảng viên.

## Overview

Bộ API giúp Giảng viên:

1.  Xem danh sách các môn học mình được phân công giảng dạy.
2.  Xem chi tiết môn học (kèm thống kê tài liệu, lớp học).
3.  Gửi yêu cầu xin giảng dạy một môn học cụ thể trong học kỳ.

**Base URL:** `/lecturer`

---

## 1. Lấy danh sách Phân công Giảng dạy (My Assignments)

API lấy danh sách các assignment của giảng viên, hỗ trợ lọc theo trạng thái và học kỳ.

- **Endpoint:** `GET /teaching-assignments`
- **Auth:** Required (Lecturer Role)

### Query Parameters

| Param        | Type   | Required | Description                                      | Default |
| :----------- | :----- | :------- | :----------------------------------------------- | :------ |
| `termId`     | UUID   | No       | ID của Học kỳ                                    | `null`  |
| `status`     | String | No       | Trạng thái (`ACTIVE`, `INACTIVE`)                | `null`  |
| `termStatus` | String | No       | Trạng thái học kỳ (`ACTIVE`, `PAST`, `UPCOMING`) | `null`  |
| `page`       | Number | No       | Trang hiện tại (0-indexed)                       | `0`     |
| `size`       | Number | No       | Kích thước trang                                 | `10`    |

### Response Example

```json
{
  "content": [
    {
      "id": "ta123-...",
      "subjectId": "s123-...",
      "subjectCode": "IT301",
      "subjectName": "Đồ án tổng hợp",
      "subjectCredit": 2,
      "termId": "t123-...",
      "termCode": "20241",
      "termName": "Học kỳ 1 2024-2025",
      "termStartDate": "2024-09-01",
      "termEndDate": "2025-01-15",
      "termStatus": "ACTIVE",
      "status": "ACTIVE",
      "approvalStatus": "APPROVED",
      "classCount": 3,
      "studentCount": 120,
      "fileCount": 15,
      "quizCount": 50,
      "flashcardCount": 100,
      "summaryCount": 5,
      "videoCount": 2,
      "createdAt": "2024-08-01T10:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

## 2. Chi tiết Phân công Giảng dạy (Assignment Detail)

Lấy thông tin chi tiết của một assignment, bao gồm cả thông tin Notebook và danh sách sơ lược các lớp (Top 5).

- **Endpoint:** `GET /teaching-assignments/{assignmentId}`
- **Auth:** Required (Lecturer Role)

### Path Parameters

| Param          | Type | Description                |
| :------------- | :--- | :------------------------- |
| `assignmentId` | UUID | ID của Teaching Assignment |

### Response Example

```json
{
  "id": "ta123-...",
  "subjectId": "s123-...",
  "subjectCode": "IT3040",
  "subjectName": "Kỹ thuật lập trình",
  "subjectCredit": 3,
  "termId": "t123-...",
  "termCode": "20241",
  "termName": "Học kỳ 1 2024-2025",
  "termIsActive": true,
  "status": "ACTIVE",
  "approvalStatus": "APPROVED",
  "termStatus": "ACTIVE",
  // Thống kê
  "classCount": 2,
  "studentCount": 80,
  "fileCount": 10,
  // Notebook
  "notebookId": "nb123-...",
  "notebookTitle": "Kỹ thuật lập trình (20241)",
  "notebookThumbnailUrl": "https://...",
  // Top 5 Class
  "recentClasses": [
    {
      "id": "c456-...",
      "classCode": "123456",
      "room": "B1-305",
      "dayOfWeek": 3,
      "periods": "7-9",
      "studentCount": 40,
      "isActive": true
    }
  ]
}
```

---

## 3. Gửi yêu cầu Giảng dạy (Request Teaching)

Giảng viên chủ động đăng ký dạy một môn trong một học kỳ.

- **Endpoint:** `POST /teaching-assignments/request`
- **Auth:** Required (Lecturer Role)

### Request Body

```json
{
  "termId": "uuid-term-id",
  "subjectId": "uuid-subject-id",
  "note": "Tôi muốn dạy môn này vì..." // Optional
}
```

### Response

Trả về object `LecturerAssignmentResponse` với `approvalStatus` = `PENDING`.

---

## Liên kết API khác

Để lấy danh sách **Lớp học** và **Sinh viên** chi tiết hơn của Assignment, vui lòng xem tài liệu:
👉 `lecturer-classes-students-api.md`

- `GET /teaching-assignments/{id}/classes`
- `GET /teaching-assignments/{id}/students`

---

## TypeScript Interface

```typescript
export interface LecturerAssignmentResponse {
  id: string;
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  subjectCredit: number;
  termId: string;
  termCode: string;
  termName: string;
  termStartDate: string;
  termEndDate: string;
  termStatus: "ACTIVE" | "PAST" | "UPCOMING";
  status: string;
  approvalStatus: string;
  classCount: number;
  studentCount: number;
  fileCount: number;
  quizCount: number;
  flashcardCount: number;
  videoCount: number;
  createdAt: string;
}

export interface RequestTeachingRequest {
  termId: string;
  subjectId: string;
  note?: string;
}
```
