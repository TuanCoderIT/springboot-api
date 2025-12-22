# 📚 API Quản lý Môn học (Subject)

> **Base URL:** `http://localhost:8386/admin/subject`  
> **Auth:** Cần Bearer Token (role ADMIN)

---

## 📋 Tổng quan

| API                       | URL                  | Method | Mô tả                                          |
| ------------------------- | -------------------- | ------ | ---------------------------------------------- |
| [Danh sách](#1-danh-sách) | `/admin/subject`     | GET    | Lấy danh sách có phân trang, filter theo ngành |
| [Chi tiết](#2-chi-tiết)   | `/admin/subject/:id` | GET    | Lấy chi tiết + các ngành có môn này            |
| [Tạo mới](#3-tạo-mới)     | `/admin/subject`     | POST   | Tạo môn học mới                                |
| [Cập nhật](#4-cập-nhật)   | `/admin/subject/:id` | PUT    | Sửa môn học                                    |
| [Xóa](#5-xóa)             | `/admin/subject/:id` | DELETE | Xóa môn học (nếu không có ràng buộc)           |

---

## 📦 TypeScript Interfaces

```typescript
// === REQUEST ===

// Gán môn học vào ngành (chương trình đào tạo)
interface MajorAssignment {
  majorId: string; // UUID ngành học
  termNo?: number; // Học kỳ trong chương trình đào tạo (1-8)
  isRequired?: boolean; // Môn bắt buộc (default: true)
  knowledgeBlock?: string; // Khối kiến thức (VD: "Cơ sở ngành", "Chuyên ngành")
}

interface CreateSubjectRequest {
  code: string; // Bắt buộc - Mã môn học (unique), max 50 ký tự
  name: string; // Bắt buộc - Tên môn học, max 255 ký tự
  credit?: number; // Số tín chỉ
  isActive?: boolean; // Default: true
  majorAssignments?: MajorAssignment[]; // Gán vào các ngành học
}

interface UpdateSubjectRequest {
  code?: string;
  name?: string;
  credit?: number;
  isActive?: boolean;
  majorAssignments?: MajorAssignment[]; // Cập nhật ngành (replace toàn bộ)
}

interface ListSubjectRequest {
  page?: number; // Default: 0
  size?: number; // Default: 10
  sortBy?: string; // Default: "code" | Các giá trị: code, name, credit, createdAt
  sortDir?: string; // "asc" | "desc", default: "asc"
  q?: string; // Tìm theo code hoặc name
  isActive?: boolean; // Filter theo trạng thái
  majorId?: string; // Filter theo ngành học (UUID)
}

// === RESPONSE ===

interface SubjectResponse {
  id: string;
  code: string;
  name: string;
  credit: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  majorCount: number; // Số ngành học có môn này
  assignmentCount: number; // Số phân công giảng dạy
}

interface SubjectDetailResponse extends SubjectResponse {
  majors: MajorInSubjectInfo[]; // Danh sách ngành học có môn này
}

interface MajorInSubjectInfo {
  id: string;
  code: string;
  name: string;
  termNo: number | null; // Học kỳ trong chương trình đào tạo
  isRequired: boolean; // Môn bắt buộc hay tự chọn
  knowledgeBlock: string | null; // Khối kiến thức
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
GET /admin/subject?page=0&size=20
```

### Request

| Query Param | Type    | Default | Mô tả                                                  |
| ----------- | ------- | ------- | ------------------------------------------------------ |
| `page`      | number  | 0       | Trang (từ 0)                                           |
| `size`      | number  | 10      | Số item/trang                                          |
| `sortBy`    | string  | "code"  | Sort theo: `code`, `name`, `credit`, `createdAt`       |
| `sortDir`   | string  | "asc"   | "asc" hoặc "desc"                                      |
| `q`         | string  | -       | Tìm theo code hoặc name                                |
| `isActive`  | boolean | -       | Filter theo trạng thái                                 |
| `majorId`   | UUID    | -       | **Filter theo ngành học** (lọc môn thuộc ngành cụ thể) |

**Ví dụ:**

```
GET /admin/subject?sortBy=code&sortDir=asc        // Sort theo mã môn A-Z
GET /admin/subject?majorId=uuid-major             // Lọc môn học thuộc ngành cụ thể
GET /admin/subject?q=lập+trình&isActive=true      // Tìm môn "lập trình" đang active
```

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "uuid-1",
      "code": "CS101",
      "name": "Nhập môn lập trình",
      "credit": 3,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-06-15T10:30:00+07:00",
      "majorCount": 5,
      "assignmentCount": 12
    },
    {
      "id": "uuid-2",
      "code": "CS201",
      "name": "Cấu trúc dữ liệu và giải thuật",
      "credit": 4,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-06-15T10:30:00+07:00",
      "majorCount": 3,
      "assignmentCount": 8
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

---

## 2. Chi tiết

```
GET /admin/subject/:id
```

### Response ✅ 200

> Bao gồm danh sách các ngành học có môn này trong chương trình đào tạo.

```json
{
  "id": "uuid-1",
  "code": "CS101",
  "name": "Nhập môn lập trình",
  "credit": 3,
  "isActive": true,
  "createdAt": "2024-01-01T00:00:00+07:00",
  "updatedAt": "2024-06-15T10:30:00+07:00",
  "majorCount": 2,
  "assignmentCount": 12,
  "majors": [
    {
      "id": "uuid-major-1",
      "code": "CNTT",
      "name": "Công nghệ thông tin",
      "termNo": 1,
      "isRequired": true,
      "knowledgeBlock": "Cơ sở ngành"
    },
    {
      "id": "uuid-major-2",
      "code": "KTPM",
      "name": "Kỹ thuật phần mềm",
      "termNo": 2,
      "isRequired": true,
      "knowledgeBlock": "Cơ sở ngành"
    }
  ]
}
```

### Lỗi

| Status | Khi nào          |
| ------ | ---------------- |
| ❌ 404 | ID không tồn tại |

---

## 3. Tạo mới

```
POST /admin/subject
Content-Type: application/json
```

### Request Body

```json
{
  "code": "CS401",
  "name": "Trí tuệ nhân tạo",
  "credit": 3,
  "isActive": true,
  "majorAssignments": [
    {
      "majorId": "uuid-cntt",
      "termNo": 7,
      "isRequired": false,
      "knowledgeBlock": "Chuyên ngành"
    },
    {
      "majorId": "uuid-ktpm",
      "termNo": 6,
      "isRequired": true,
      "knowledgeBlock": "Cơ sở ngành"
    }
  ]
}
```

### Validation Rules

| Field              | Bắt buộc | Rules                       |
| ------------------ | -------- | --------------------------- |
| `code`             | ✅       | Unique, max 50 ký tự        |
| `name`             | ✅       | Max 255 ký tự               |
| `credit`           | ❌       | Số nguyên                   |
| `isActive`         | ❌       | Default: `true`             |
| `majorAssignments` | ❌       | Array của `MajorAssignment` |

> **Tip:** Để tạo môn học mà không gán vào ngành nào, bỏ qua hoặc gửi `majorAssignments: []`

### Response ✅ 201

```json
{
  "id": "uuid-new",
  "code": "CS401",
  "name": "Trí tuệ nhân tạo",
  "credit": 3,
  "isActive": true,
  "createdAt": "2024-12-20T13:00:00+07:00",
  "updatedAt": "2024-12-20T13:00:00+07:00",
  "majorCount": 0,
  "assignmentCount": 0
}
```

### Lỗi

| Status | Khi nào                                   |
| ------ | ----------------------------------------- |
| ❌ 400 | Thiếu field bắt buộc hoặc validation fail |
| ❌ 409 | `code` đã tồn tại trong hệ thống          |

---

## 4. Cập nhật

```
PUT /admin/subject/:id
Content-Type: application/json
```

### Request Body

> Chỉ gửi field cần sửa. Với `majorAssignments`:
>
> - **`null`** (hoặc không gửi): giữ nguyên liên kết hiện tại
> - **`[]`** (array rỗng): xóa hết liên kết với ngành
> - **Array có items**: replace toàn bộ liên kết

```json
{
  "name": "Trí tuệ nhân tạo nâng cao",
  "credit": 4,
  "majorAssignments": [
    {
      "majorId": "uuid-cntt",
      "termNo": 7,
      "isRequired": true,
      "knowledgeBlock": "Chuyên ngành"
    }
  ]
}
```

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "code": "CS401",
  "name": "Trí tuệ nhân tạo nâng cao",
  "credit": 4,
  "isActive": true,
  "createdAt": "2024-12-20T13:00:00+07:00",
  "updatedAt": "2024-12-20T14:00:00+07:00",
  "majorCount": 2,
  "assignmentCount": 5
}
```

### Lỗi

| Status | Khi nào               |
| ------ | --------------------- |
| ❌ 404 | ID không tồn tại      |
| ❌ 409 | `code` mới đã tồn tại |

---

## 5. Xóa

```
DELETE /admin/subject/:id
```

### Response ✅ 204

Không có body.

### Lỗi

| Status | Khi nào                                                      |
| ------ | ------------------------------------------------------------ |
| ❌ 404 | ID không tồn tại                                             |
| ❌ 409 | **Môn học đang có phân công giảng dạy (TeachingAssignment)** |

> ⚠️ **Lưu ý:**
>
> - Chỉ chặn xóa nếu có `TeachingAssignment` (phân công giảng dạy)
> - Liên kết `MajorSubject` sẽ được **tự động xóa** khi xóa môn học

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useSubjects.ts
import useSWR from "swr";

const fetcher = (url: string) =>
  fetch(url, { headers: { Authorization: `Bearer ${token}` } }).then((res) =>
    res.json()
  );

// Danh sách
export function useSubjects(params: ListSubjectRequest = {}) {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.sortBy) searchParams.set("sortBy", params.sortBy);
  if (params.sortDir) searchParams.set("sortDir", params.sortDir);
  if (params.q) searchParams.set("q", params.q);
  if (params.isActive !== undefined)
    searchParams.set("isActive", String(params.isActive));
  if (params.majorId) searchParams.set("majorId", params.majorId);

  const { data, error, mutate } = useSWR(
    `/admin/subject?${searchParams}`,
    fetcher
  );

  return {
    subjects: (data?.data ?? []) as SubjectResponse[],
    meta: data?.meta,
    isLoading: !error && !data,
    mutate,
  };
}

// Chi tiết
export function useSubjectDetail(id: string | null) {
  const { data, error, mutate } = useSWR(
    id ? `/admin/subject/${id}` : null,
    fetcher
  );

  return {
    subject: data as SubjectDetailResponse | undefined,
    isLoading: !error && !data,
    mutate,
  };
}
```

### Xử lý CRUD

```typescript
// actions/subject.ts
const API_BASE = "/admin/subject";

export async function createSubject(data: CreateSubjectRequest) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 409) throw new Error("Mã môn học đã tồn tại");
  if (!res.ok) throw new Error("Tạo môn học thất bại");
  return res.json() as Promise<SubjectResponse>;
}

export async function updateSubject(id: string, data: UpdateSubjectRequest) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 404) throw new Error("Không tìm thấy môn học");
  if (res.status === 409) throw new Error("Mã môn học mới đã tồn tại");
  if (!res.ok) throw new Error("Cập nhật thất bại");
  return res.json() as Promise<SubjectResponse>;
}

export async function deleteSubject(id: string) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) throw new Error("Không tìm thấy môn học");
  if (res.status === 409) {
    throw new Error("Không thể xóa môn học đang có ràng buộc dữ liệu");
  }
  if (!res.ok) throw new Error("Xóa thất bại");
}
```
