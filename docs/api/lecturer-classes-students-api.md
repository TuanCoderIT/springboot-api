# Lecturer Classes & Students API

Tài liệu hướng dẫn tích hợp API quản lý Lớp học phần và Sinh viên dành cho Giảng viên.

## Overview

Bộ API này giúp Giảng viên:

1.  Xem danh sách các lớp học phần mình đang dạy (có filter theo học kỳ, môn học).
2.  Xem danh sách sinh viên trong từng lớp.
3.  Tìm kiếm sinh viên.

**Base URL:** `/lecturer`

---

## 1. Lấy danh sách Lớp học phần (All Classes)

API này lấy tất cả các lớp mà giảng viên phụ trách, hỗ trợ bộ lọc linh hoạt.

- **Endpoint:** `GET /classes`
- **Auth:** Required (Lecturer Role)

### Query Parameters

| Param          | Type   | Required | Description                                | Default     |
| :------------- | :----- | :------- | :----------------------------------------- | :---------- |
| `termId`       | UUID   | No       | Lọc theo Học kỳ                            | `null`      |
| `assignmentId` | UUID   | No       | Lọc theo Phân công giảng dạy               | `null`      |
| `q`            | String | No       | Từ khóa tìm kiếm (Mã lớp, Tên môn)         | `null`      |
| `page`         | Number | No       | Số trang (0-indexed)                       | `0`         |
| `size`         | Number | No       | Kích thước trang                           | `10`        |
| `sortBy`       | String | No       | Trường sắp xếp (`classCode`, `created_at`) | `classCode` |
| `sortDir`      | String | No       | Hướng sắp xếp (`asc`, `desc`)              | `asc`       |

### Response Example

```json
{
  "content": [
    {
      "id": "c1b2a3e4-...",
      "classCode": "DATH_01",
      "subjectCode": "IT301",
      "subjectName": "Đồ án tổng hợp",
      "termName": "Học kỳ 1 2024-2025",
      "room": "B1-201",
      "dayOfWeek": 2,
      "periods": "1-3",
      "startDate": "2024-09-05",
      "endDate": "2025-01-15",
      "note": "Lớp chất lượng cao",
      "isActive": true,
      "studentCount": 45,
      "createdAt": "2024-08-20T10:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

---

}
}

---

## 2. Lấy chi tiết Lớp học phần (Class Detail)

API này trả về thông tin chi tiết của một lớp học phần.

- **Endpoint:** `GET /classes/{classId}`
- **Auth:** Required (Lecturer Role)

### Path Parameters

| Param     | Type | Description         |
| :-------- | :--- | :------------------ |
| `classId` | UUID | ID của lớp học phần |

### Response Example

```json
{
  "id": "c1b2a3e4-...",
  "classCode": "DATH_01",
  "subjectCode": "IT301",
  "subjectName": "Đồ án tổng hợp",
  "termName": "Học kỳ 1 2024-2025",
  "room": "B1-201",
  "dayOfWeek": 2,
  "periods": "1-3",
  "startDate": "2024-09-05",
  "endDate": "2025-01-15",
  "note": "Lớp chất lượng cao",
  "isActive": true,
  "studentCount": 45,
  "createdAt": "2024-08-20T10:00:00Z",
  "updatedAt": "2024-08-20T10:00:00Z",
  "assignmentId": "ta123-...",
  "assignmentStatus": "ACTIVE",
  "fileCount": 15,
  "quizCount": 50,
  "flashcardCount": 100,
  "summaryCount": 5,
  "videoCount": 2,
  "notebookId": "nb123-...",
  "notebookTitle": "Kỹ thuật lập trình (20241)"
}
```

---

## 3. Lấy danh sách Sinh viên trong Lớp (Class Members)

API này lấy danh sách sinh viên thuộc một lớp học phần cụ thể.

- **Endpoint:** `GET /classes/{classId}/members`
- **Auth:** Required (Lecturer Role)

### Path Parameters

| Param     | Type | Description         |
| :-------- | :--- | :------------------ |
| `classId` | UUID | ID của lớp học phần |

### Query Parameters

| Param     | Type   | Required | Description                                 | Default       |
| :-------- | :----- | :------- | :------------------------------------------ | :------------ |
| `q`       | String | No       | Tìm kiếm (Tên, MSSV)                        | `null`        |
| `page`    | Number | No       | Số trang                                    | `0`           |
| `size`    | Number | No       | Kích thước trang                            | `10`          |
| `sortBy`  | String | No       | Trường sắp xếp (`studentCode`, `firstName`) | `studentCode` |
| `sortDir` | String | No       | Hướng sắp xếp (`asc`, `desc`)               | `asc`         |

### Response Example

```json
{
  "content": [
    {
      "id": "m5n6o7p8-...",
      "studentCode": "20201234",
      "fullName": "Nguyễn Văn A",
      "firstName": "A",
      "lastName": "Nguyễn Văn",
      "dob": "2002-05-15",
      "classCode": "DATH_01",
      "subjectName": "Đồ án tổng hợp",
      "termName": "Học kỳ 1 2024-2025",
      "createdAt": "2024-09-01T08:00:00Z"
    }
  ],
  "meta": { ... }
}
```

---

## 4. Lấy danh sách Sinh viên theo Phân công (Assignment Students)

API này lấy toàn bộ sinh viên thuộc một phân công giảng dạy (có thể bao gồm nhiều lớp nếu assignment đó quản lý nhiều lớp).

- **Endpoint:** `GET /teaching-assignments/{assignmentId}/students`
- **Auth:** Required (Lecturer Role)

### Path Parameters

| Param          | Type | Description                |
| :------------- | :--- | :------------------------- |
| `assignmentId` | UUID | ID của phân công giảng dạy |

### Query Parameters

| Param          | Type | Required | Description                                  |
| :------------- | :--- | :------- | :------------------------------------------- |
| `classId`      | UUID | No       | Lọc thêm theo ID lớp cụ thể trong assignment |
| `q`, `page`... |      |          | (Tương tự API trên)                          |

---

## TypeScript Interfaces

Dưới đây là các Type definition để Frontend sử dụng:

```typescript
// Common Pagination
export interface PagedResponse<T> {
  content: T[];
  meta: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// 1. Class Response
export interface ClassResponse {
  id: string;
  classCode: string;
  subjectCode: string; // Mã môn (ví dụ: IT3040)
  subjectName: string; // Tên môn
  termName: string | null; // Tên học kỳ (ví dụ: 20241)
  room: string | null;
  dayOfWeek: number | null; // 2->Mon, 8->Sun
  periods: string | null; // Tiết học (ví dụ: 1-3)
  startDate: string | null; // YYYY-MM-DD
  endDate: string | null;
  note: string | null;
  isActive: boolean;
  studentCount: number; // Tổng số sinh viên
  createdAt: string;
  updatedAt?: string;
}

// 1.1 Class Detail Response (Extends Class Response)
export interface ClassDetailResponse extends ClassResponse {
  assignmentId: string;
  assignmentStatus: string;
  // Resource Counts
  fileCount: number;
  quizCount: number;
  flashcardCount: number;
  videoCount: number;
  summaryCount: number;
  // Notebook Info
  notebookId: string | null;
  notebookTitle: string | null;
}

// 2. Student Response (Class Member)
export interface ClassStudentResponse {
  id: string; // ID của ClassMember relationship (không phải User ID)
  studentCode: string;
  fullName: string;
  firstName: string;
  lastName: string;
  dob: string | null;
  classCode: string | null;
  subjectCode: string | null;
  subjectName: string | null;
  termName: string | null;
  createdAt: string;
}

// API Parameters Interface
export interface GetClassesParams {
  termId?: string;
  assignmentId?: string;
  q?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

export interface GetClassMembersParams {
  q?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
```

## Ghi chú cho FE integration

1.  **Lọc theo Học kỳ:** Khi user chọn dropdown Học kỳ trên UI -> gọi `GET /classes?termId={selectedTermId}`.
2.  **Search:** Input search box -> debounce -> gọi `GET /classes?q={keyword}`.
3.  **Chi tiết Lớp:** Khi click vào một lớp -> chuyển trang -> gọi `GET /classes/{classId}/members` để hiện danh sách sinh viên.
