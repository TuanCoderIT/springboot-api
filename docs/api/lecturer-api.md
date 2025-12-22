# 👨‍🏫 API Quản lý Giảng viên (Lecturer)

> **Base URL:** `http://localhost:8386/admin/lecturer`  
> **Auth:** Cần Bearer Token (role ADMIN)

---

## 📋 Tổng quan

| API                       | URL                   | Method | Mô tả                       |
| ------------------------- | --------------------- | ------ | --------------------------- |
| [Danh sách](#1-danh-sách) | `/admin/lecturer`     | GET    | Lấy danh sách có phân trang |
| [Chi tiết](#2-chi-tiết)   | `/admin/lecturer/:id` | GET    | Lấy 1 giảng viên            |
| [Tạo mới](#3-tạo-mới)     | `/admin/lecturer`     | POST   | Tạo giảng viên mới          |
| [Cập nhật](#4-cập-nhật)   | `/admin/lecturer/:id` | PUT    | Sửa giảng viên              |
| [Xóa](#5-xóa)             | `/admin/lecturer/:id` | DELETE | Xóa giảng viên              |

---

## 📦 TypeScript Interfaces

```typescript
// === REQUEST ===

// Tạo mới
interface CreateLecturerRequest {
  email: string; // Bắt buộc - Email (unique)
  fullName: string; // Bắt buộc - Họ tên
  password: string; // Bắt buộc - Mật khẩu (min 6 ký tự)
  avatarUrl?: string; // URL ảnh đại diện
  lecturerCode: string; // Bắt buộc - Mã giảng viên (unique)
  orgUnitId?: string; // UUID đơn vị tổ chức
  academicDegree?: string; // "ThS" | "TS" | "PGS.TS" | "GS.TS"
  academicRank?: string; // "PGS" | "GS"
  specialization?: string; // Chuyên ngành
  phone?: string; // SĐT
}

// Cập nhật (tất cả optional)
interface UpdateLecturerRequest {
  email?: string;
  fullName?: string;
  password?: string;
  avatarUrl?: string;
  active?: boolean;
  lecturerCode?: string;
  orgUnitId?: string;
  academicDegree?: string;
  academicRank?: string;
  specialization?: string;
  phone?: string;
}

// Query params danh sách
interface ListLecturerRequest {
  page?: number; // Default: 0
  size?: number; // Default: 10
  sortBy?: string; // Default: "createdAt"
  sortDir?: string; // "asc" | "desc", default: "desc"
  q?: string; // Tìm theo tên/email/mã GV
  orgUnitId?: string; // Filter theo đơn vị tổ chức
}

// === RESPONSE ===

interface LecturerResponse {
  id: string;
  fullName: string;
  email: string;
  role: string; // "LECTURER"
  active: boolean | null;
  avatarUrl: string | null;
  createdAt: string; // ISO datetime
  updatedAt: string;
  // TeacherProfile
  lecturerCode: string | null;
  academicDegree: string | null;
  academicRank: string | null;
  specialization: string | null;
  phone: string | null;
  // OrgUnit
  orgUnit: OrgUnitInfo | null;
}

interface OrgUnitInfo {
  id: string;
  code: string;
  name: string;
  type: string | null;
}

interface PagedResponse<T> {
  data: T[];
  meta: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}
```

---

## 1. Danh sách

```
GET /admin/lecturer?page=0&size=20
```

### Request

| Query Param | Type   | Default     | Mô tả                            |
| ----------- | ------ | ----------- | -------------------------------- |
| `page`      | number | 0           | Trang (từ 0)                     |
| `size`      | number | 10          | Số item/trang                    |
| `sortBy`    | string | "createdAt" | Sort theo field                  |
| `sortDir`   | string | "desc"      | "asc" hoặc "desc"                |
| `q`         | string | -           | Tìm theo tên/email/mã giảng viên |
| `orgUnitId` | UUID   | -           | Filter theo đơn vị tổ chức       |

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "uuid-1",
      "fullName": "Nguyễn Văn A",
      "email": "nva@example.com",
      "role": "LECTURER",
      "active": null,
      "avatarUrl": "https://...",
      "createdAt": "2024-01-15T10:30:00+07:00",
      "updatedAt": "2024-01-15T10:30:00+07:00",
      "lecturerCode": "GV001",
      "academicDegree": "Tiến sĩ",
      "academicRank": "Phó Giáo sư",
      "specialization": "Công nghệ phần mềm",
      "phone": "0901234567",
      "orgUnit": {
        "id": "uuid-org",
        "code": "CNTT",
        "name": "Khoa Công nghệ Thông tin",
        "type": "faculty"
      }
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 2. Chi tiết

```
GET /admin/lecturer/:id
```

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "fullName": "Nguyễn Văn A",
  "email": "nva@example.com",
  "role": "LECTURER",
  "avatarUrl": "https://...",
  "lecturerCode": "GV001",
  "academicDegree": "Tiến sĩ",
  "academicRank": "Phó Giáo sư",
  "specialization": "Công nghệ phần mềm",
  "phone": "0901234567",
  "orgUnit": {
    "id": "uuid-org",
    "code": "CNTT",
    "name": "Khoa Công nghệ Thông tin",
    "type": "faculty"
  },
  "createdAt": "2024-01-15T10:30:00+07:00",
  "updatedAt": "2024-01-15T10:30:00+07:00"
}
```

### Lỗi

| Status | Khi nào          |
| ------ | ---------------- |
| ❌ 404 | ID không tồn tại |

---

## 3. Tạo mới

```
POST /admin/lecturer
Content-Type: application/json
```

### Request Body

```json
{
  "email": "nva@example.com",
  "fullName": "Nguyễn Văn A",
  "password": "123456",
  "avatarUrl": "https://...",
  "lecturerCode": "GV001",
  "orgUnitId": "uuid-org-unit",
  "academicDegree": "Tiến sĩ",
  "academicRank": "Phó Giáo sư",
  "specialization": "Công nghệ phần mềm",
  "phone": "0901234567"
}
```

### Validation Rules

| Field            | Bắt buộc | Rules                        |
| ---------------- | -------- | ---------------------------- |
| `email`          | ✅       | Phải là email hợp lệ, unique |
| `fullName`       | ✅       | Max 255 ký tự                |
| `password`       | ✅       | Min 6 ký tự                  |
| `avatarUrl`      | ❌       | URL ảnh                      |
| `lecturerCode`   | ✅       | Unique trong hệ thống        |
| `orgUnitId`      | ❌       | UUID đơn vị tổ chức          |
| `academicDegree` | ❌       | VD: "ThS", "TS", "PGS.TS"    |
| `academicRank`   | ❌       | VD: "PGS", "GS"              |
| `specialization` | ❌       | Chuyên ngành                 |
| `phone`          | ❌       | SĐT                          |

### Response ✅ 201

```json
{
  "id": "uuid-new",
  "fullName": "Nguyễn Văn A",
  "email": "nva@example.com",
  "role": "LECTURER",
  "lecturerCode": "GV001",
  "orgUnit": { ... },
  ...
}
```

### Lỗi

| Status | Khi nào                                   |
| ------ | ----------------------------------------- |
| ❌ 400 | Thiếu field bắt buộc hoặc validation fail |
| ❌ 404 | `orgUnitId` không tồn tại                 |
| ❌ 409 | `email` hoặc `lecturerCode` đã tồn tại    |

---

## 4. Cập nhật

```
PUT /admin/lecturer/:id
Content-Type: application/json
```

### Request Body

> Chỉ gửi field cần sửa

```json
{
  "fullName": "Nguyễn Văn B",
  "phone": "0909999999",
  "orgUnitId": "uuid-org-unit-new"
}
```

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "fullName": "Nguyễn Văn B",
  "phone": "0909999999",
  "orgUnit": { ... },
  ...
}
```

### Lỗi

| Status | Khi nào                                |
| ------ | -------------------------------------- |
| ❌ 404 | ID không tồn tại                       |
| ❌ 404 | `orgUnitId` không tồn tại              |
| ❌ 409 | `email` hoặc `lecturerCode` đã tồn tại |

---

## 5. Xóa

```
DELETE /admin/lecturer/:id
```

### Response ✅ 204

Không có body.

### Lỗi

| Status | Khi nào          |
| ------ | ---------------- |
| ❌ 404 | ID không tồn tại |

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useLecturers.ts
import useSWR from "swr";

export function useLecturers(page = 0, size = 20) {
  const { data, error, mutate } = useSWR(
    `/admin/lecturer?page=${page}&size=${size}`
  );

  return {
    lecturers: data?.data ?? [],
    meta: data?.meta,
    isLoading: !error && !data,
    mutate,
  };
}

// Tạo mới
async function createLecturer(data: CreateLecturerRequest) {
  const res = await fetch("/admin/lecturer", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (res.status === 409) {
    throw new Error("Email hoặc mã giảng viên đã tồn tại");
  }

  return res.json();
}
```
