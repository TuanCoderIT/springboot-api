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
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu",
      "termName": "Học kỳ 1 - 2024-2025",
      "status": "ACTIVE",
      "approvalStatus": "APPROVED",
      "classCount": 3,
      "studentCount": 120,
      "termStatus": "ACTIVE",
      "createdAt": "2024-09-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 5, "totalPages": 1 }
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

## 6. Sinh viên của phân công (tất cả lớp)

### GET /lecturer/teaching-assignments/{assignmentId}/students

Lấy **toàn bộ sinh viên** trong 1 phân công (gộp từ tất cả lớp).

**Path Params:** `assignmentId` (UUID)

**Query Params:** Giống API 7

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
