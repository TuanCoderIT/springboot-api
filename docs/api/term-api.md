# 📅 API Quản lý Học kỳ (Term)

> **Base URL:** `http://localhost:8386/admin/term`  
> **Auth:** Cần Bearer Token (role ADMIN)

---

## 📋 Tổng quan

| API                       | URL               | Method | Mô tả                                  |
| ------------------------- | ----------------- | ------ | -------------------------------------- |
| [Danh sách](#1-danh-sách) | `/admin/term`     | GET    | Lấy danh sách có phân trang            |
| [Chi tiết](#2-chi-tiết)   | `/admin/term/:id` | GET    | Lấy chi tiết + môn học trong kỳ        |
| [Tạo mới](#3-tạo-mới)     | `/admin/term`     | POST   | Tạo học kỳ mới                         |
| [Cập nhật](#4-cập-nhật)   | `/admin/term/:id` | PUT    | Sửa học kỳ                             |
| [Xóa](#5-xóa)             | `/admin/term/:id` | DELETE | Xóa học kỳ (nếu không có phân công GV) |

---

## 📦 TypeScript Interfaces

```typescript
// === REQUEST ===

// Tạo mới
interface CreateTermRequest {
  code: string; // Bắt buộc - Mã học kỳ (unique), max 50 ký tự
  name: string; // Bắt buộc - Tên học kỳ, max 255 ký tự
  startDate?: string; // ISO date: "2024-09-01"
  endDate?: string; // ISO date: "2025-01-15"
  isActive?: boolean; // Default: true
}

// Cập nhật (tất cả optional)
interface UpdateTermRequest {
  code?: string; // Max 50 ký tự
  name?: string; // Max 255 ký tự
  startDate?: string;
  endDate?: string;
  isActive?: boolean;
}

// Query params danh sách
interface ListTermRequest {
  page?: number; // Default: 0
  size?: number; // Default: 10
  sortBy?: string; // Default: "createdAt"
  sortDir?: string; // "asc" | "desc", default: "desc"
  q?: string; // Tìm theo code hoặc name
  isActive?: boolean; // Filter theo trạng thái active
}

// === RESPONSE ===

interface TermResponse {
  id: string;
  code: string;
  name: string;
  startDate: string | null; // ISO date
  endDate: string | null; // ISO date
  isActive: boolean;
  createdAt: string; // ISO datetime
  totalAssignments: number; // Tổng số phân công giảng dạy trong kỳ
}

interface TermDetailResponse extends TermResponse {
  subjects: SubjectInTermInfo[]; // Danh sách môn học được mở trong kỳ
}

interface SubjectInTermInfo {
  id: string;
  code: string;
  name: string;
  credit: number | null;
  teacherCount: number; // Số giảng viên đang dạy môn này trong kỳ
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
GET /admin/term?page=0&size=20
```

### Request

| Query Param | Type    | Default     | Mô tả                                                          |
| ----------- | ------- | ----------- | -------------------------------------------------------------- |
| `page`      | number  | 0           | Trang (từ 0)                                                   |
| `size`      | number  | 10          | Số item/trang                                                  |
| `sortBy`    | string  | "createdAt" | Sort theo: `code`, `name`, `startDate`, `endDate`, `createdAt` |
| `sortDir`   | string  | "desc"      | "asc" hoặc "desc"                                              |
| `q`         | string  | -           | Tìm theo code hoặc name                                        |
| `isActive`  | boolean | -           | Filter theo trạng thái                                         |

**Ví dụ:**

```
GET /admin/term?sortBy=code&sortDir=asc      // Sort theo mã học kỳ A-Z
GET /admin/term?sortBy=startDate&sortDir=desc // Sort theo ngày bắt đầu mới nhất
GET /admin/term?q=2024&isActive=true          // Tìm học kỳ 2024 đang active
```

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "code": "HK1-2024",
      "name": "Học kỳ 1 năm học 2024-2025",
      "startDate": "2024-09-01",
      "endDate": "2025-01-15",
      "isActive": true,
      "createdAt": "2024-08-01T00:00:00+07:00",
      "totalAssignments": 150
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "code": "HK2-2024",
      "name": "Học kỳ 2 năm học 2024-2025",
      "startDate": "2025-02-01",
      "endDate": "2025-06-30",
      "isActive": false,
      "createdAt": "2024-08-01T00:00:00+07:00",
      "totalAssignments": 0
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
GET /admin/term/:id
```

### Response ✅ 200

> Bao gồm danh sách các môn học được mở trong học kỳ và số giảng viên phụ trách mỗi môn.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "code": "HK1-2024",
  "name": "Học kỳ 1 năm học 2024-2025",
  "startDate": "2024-09-01",
  "endDate": "2025-01-15",
  "isActive": true,
  "createdAt": "2024-08-01T00:00:00+07:00",
  "totalAssignments": 150,
  "subjects": [
    {
      "id": "uuid-subject-1",
      "code": "CS101",
      "name": "Nhập môn lập trình",
      "credit": 3,
      "teacherCount": 5
    },
    {
      "id": "uuid-subject-2",
      "code": "CS201",
      "name": "Cấu trúc dữ liệu và giải thuật",
      "credit": 4,
      "teacherCount": 3
    },
    {
      "id": "uuid-subject-3",
      "code": "CS301",
      "name": "Cơ sở dữ liệu",
      "credit": 3,
      "teacherCount": 4
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
POST /admin/term
Content-Type: application/json
```

### Request Body

```json
{
  "code": "HK1-2025",
  "name": "Học kỳ 1 năm học 2025-2026",
  "startDate": "2025-09-01",
  "endDate": "2026-01-15",
  "isActive": true
}
```

### Validation Rules

| Field       | Bắt buộc | Rules                        |
| ----------- | -------- | ---------------------------- |
| `code`      | ✅       | Unique, max 50 ký tự         |
| `name`      | ✅       | Max 255 ký tự                |
| `startDate` | ❌       | ISO date format (YYYY-MM-DD) |
| `endDate`   | ❌       | ISO date format              |
| `isActive`  | ❌       | Default: `true`              |

### Response ✅ 201

```json
{
  "id": "uuid-new",
  "code": "HK1-2025",
  "name": "Học kỳ 1 năm học 2025-2026",
  "startDate": "2025-09-01",
  "endDate": "2026-01-15",
  "isActive": true,
  "createdAt": "2024-12-20T12:00:00+07:00",
  "totalAssignments": 0
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
PUT /admin/term/:id
Content-Type: application/json
```

### Request Body

> Chỉ gửi field cần sửa

```json
{
  "name": "Học kỳ 1 (đã cập nhật)",
  "isActive": false
}
```

### Response ✅ 200

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "code": "HK1-2024",
  "name": "Học kỳ 1 (đã cập nhật)",
  "startDate": "2024-09-01",
  "endDate": "2025-01-15",
  "isActive": false,
  "createdAt": "2024-08-01T00:00:00+07:00",
  "totalAssignments": 150
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
DELETE /admin/term/:id
```

### Response ✅ 204

Không có body.

### Lỗi

| Status | Khi nào                                             |
| ------ | --------------------------------------------------- |
| ❌ 404 | ID không tồn tại                                    |
| ❌ 409 | **Học kỳ đang có phân công giảng dạy (khóa ngoại)** |

> ⚠️ **Lưu ý:** Không thể xóa học kỳ nếu đã có `TeachingAssignment` liên kết. Cần xóa hết các phân công giảng dạy trước.

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useTerms.ts
import useSWR from "swr";

const fetcher = (url: string) =>
  fetch(url, { headers: { Authorization: `Bearer ${token}` } }).then((res) =>
    res.json()
  );

// Danh sách
export function useTerms(page = 0, size = 20, isActive?: boolean) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (isActive !== undefined) params.set("isActive", String(isActive));

  const { data, error, mutate } = useSWR(`/admin/term?${params}`, fetcher);

  return {
    terms: (data?.data ?? []) as TermResponse[],
    meta: data?.meta,
    isLoading: !error && !data,
    mutate,
  };
}

// Chi tiết
export function useTermDetail(id: string) {
  const { data, error, mutate } = useSWR(
    id ? `/admin/term/${id}` : null,
    fetcher
  );

  return {
    term: data as TermDetailResponse | undefined,
    isLoading: !error && !data,
    mutate,
  };
}
```

### Xử lý CRUD

```typescript
// actions/term.ts
import { toast } from "sonner";

const API_BASE = "/admin/term";

// Tạo mới
export async function createTerm(data: CreateTermRequest) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 409) {
    throw new Error("Mã học kỳ đã tồn tại");
  }
  if (!res.ok) {
    throw new Error("Tạo học kỳ thất bại");
  }

  return res.json() as Promise<TermResponse>;
}

// Cập nhật
export async function updateTerm(id: string, data: UpdateTermRequest) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 404) {
    throw new Error("Không tìm thấy học kỳ");
  }
  if (res.status === 409) {
    throw new Error("Mã học kỳ mới đã tồn tại");
  }
  if (!res.ok) {
    throw new Error("Cập nhật thất bại");
  }

  return res.json() as Promise<TermResponse>;
}

// Xóa
export async function deleteTerm(id: string) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) {
    throw new Error("Không tìm thấy học kỳ");
  }
  if (res.status === 409) {
    // ⚠️ Có khóa ngoại - không thể xóa
    throw new Error("Không thể xóa học kỳ đang có phân công giảng dạy");
  }
  if (!res.ok) {
    throw new Error("Xóa thất bại");
  }
}
```

### Ví dụ UI xử lý lỗi xóa

```tsx
// components/TermDeleteDialog.tsx
import { AlertDialog } from "@/components/ui/alert-dialog";
import { toast } from "sonner";
import { deleteTerm } from "@/actions/term";

export function TermDeleteDialog({ term, onSuccess }) {
  const handleDelete = async () => {
    try {
      await deleteTerm(term.id);
      toast.success("Đã xóa học kỳ");
      onSuccess?.();
    } catch (error) {
      // Hiển thị thông báo lỗi rõ ràng cho khóa ngoại
      if (error.message.includes("phân công giảng dạy")) {
        toast.error("Không thể xóa", {
          description: `Học kỳ "${term.name}" đang có ${term.totalAssignments} phân công giảng dạy. Vui lòng xóa các phân công trước.`,
        });
      } else {
        toast.error(error.message);
      }
    }
  };

  return (
    <AlertDialog>
      {/* Dialog content */}
      <Button onClick={handleDelete} variant="destructive">
        Xóa
      </Button>
    </AlertDialog>
  );
}
```
