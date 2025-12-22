# 🎓 API Quản lý Ngành học (Major)

> **Base URL:** `http://localhost:8386/admin/major`  
> **Auth:** Cần Bearer Token (role ADMIN)

---

## 📋 Tổng quan

| API                       | URL                | Method | Mô tả                                  |
| ------------------------- | ------------------ | ------ | -------------------------------------- |
| [Danh sách](#1-danh-sách) | `/admin/major`     | GET    | Lấy danh sách có phân trang, filter    |
| [Chi tiết](#2-chi-tiết)   | `/admin/major/:id` | GET    | Lấy chi tiết + chương trình đào tạo    |
| [Tạo mới](#3-tạo-mới)     | `/admin/major`     | POST   | Tạo ngành học mới                      |
| [Cập nhật](#4-cập-nhật)   | `/admin/major/:id` | PUT    | Sửa ngành học                          |
| [Xóa](#5-xóa)             | `/admin/major/:id` | DELETE | Xóa ngành học (nếu không có ràng buộc) |

---

## 📦 TypeScript Interfaces

```typescript
// === REQUEST ===

interface CreateMajorRequest {
  code: string; // Bắt buộc - Mã ngành (unique), max 50 ký tự
  name: string; // Bắt buộc - Tên ngành, max 255 ký tự
  orgUnitId?: string; // UUID đơn vị tổ chức (Khoa)
  isActive?: boolean; // Default: true
}

interface UpdateMajorRequest {
  code?: string;
  name?: string;
  orgUnitId?: string;
  isActive?: boolean;
}

interface ListMajorRequest {
  page?: number; // Default: 0
  size?: number; // Default: 10
  sortBy?: string; // Default: "code" | Các giá trị: code, name, createdAt
  sortDir?: string; // "asc" | "desc", default: "asc"
  q?: string; // Tìm theo code hoặc name
  isActive?: boolean; // Filter theo trạng thái
  orgUnitId?: string; // Filter theo đơn vị tổ chức
}

// === RESPONSE ===

interface MajorResponse {
  id: string;
  code: string;
  name: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  orgUnit: OrgUnitInfo | null;
  subjectCount: number; // Số môn học trong chương trình đào tạo
  studentCount: number; // Số sinh viên đang học ngành này
}

interface MajorDetailResponse extends MajorResponse {
  subjects: SubjectInMajorInfo[]; // Chương trình đào tạo
}

interface SubjectInMajorInfo {
  id: string;
  code: string;
  name: string;
  credit: number | null;
  termNo: number | null; // Học kỳ trong chương trình đào tạo
  isRequired: boolean; // Môn bắt buộc hay tự chọn
  knowledgeBlock: string | null; // Khối kiến thức
}

interface OrgUnitInfo {
  id: string;
  code: string;
  name: string;
  type: string | null;
}
```

---

## 1. Danh sách

```
GET /admin/major?page=0&size=20
```

### Request

| Query Param | Type    | Default | Mô tả                                  |
| ----------- | ------- | ------- | -------------------------------------- |
| `page`      | number  | 0       | Trang (từ 0)                           |
| `size`      | number  | 10      | Số item/trang                          |
| `sortBy`    | string  | "code"  | Sort theo: `code`, `name`, `createdAt` |
| `sortDir`   | string  | "asc"   | "asc" hoặc "desc"                      |
| `q`         | string  | -       | Tìm theo code hoặc name                |
| `isActive`  | boolean | -       | Filter theo trạng thái                 |
| `orgUnitId` | UUID    | -       | Filter theo đơn vị tổ chức (Khoa)      |

**Ví dụ:**

```
GET /admin/major?sortBy=code&sortDir=asc
GET /admin/major?orgUnitId=uuid-khoa-cntt     // Ngành thuộc Khoa CNTT
GET /admin/major?q=công+nghệ&isActive=true
```

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "uuid-1",
      "code": "CNTT",
      "name": "Công nghệ thông tin",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-06-15T10:30:00+07:00",
      "orgUnit": {
        "id": "uuid-org",
        "code": "KHOA_CNTT",
        "name": "Khoa Công nghệ Thông tin",
        "type": "faculty"
      },
      "subjectCount": 45,
      "studentCount": 320
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
GET /admin/major/:id
```

### Response ✅ 200

> Bao gồm chương trình đào tạo (danh sách môn học, sắp xếp theo học kỳ).

```json
{
  "id": "uuid-1",
  "code": "CNTT",
  "name": "Công nghệ thông tin",
  "isActive": true,
  "createdAt": "2024-01-01T00:00:00+07:00",
  "updatedAt": "2024-06-15T10:30:00+07:00",
  "orgUnit": {
    "id": "uuid-org",
    "code": "KHOA_CNTT",
    "name": "Khoa Công nghệ Thông tin",
    "type": "faculty"
  },
  "subjectCount": 3,
  "studentCount": 320,
  "subjects": [
    {
      "id": "uuid-s1",
      "code": "CS101",
      "name": "Nhập môn lập trình",
      "credit": 3,
      "termNo": 1,
      "isRequired": true,
      "knowledgeBlock": "Cơ sở ngành"
    },
    {
      "id": "uuid-s2",
      "code": "CS201",
      "name": "Cấu trúc dữ liệu",
      "credit": 4,
      "termNo": 2,
      "isRequired": true,
      "knowledgeBlock": "Cơ sở ngành"
    },
    {
      "id": "uuid-s3",
      "code": "CS401",
      "name": "Trí tuệ nhân tạo",
      "credit": 3,
      "termNo": 7,
      "isRequired": false,
      "knowledgeBlock": "Chuyên ngành"
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
POST /admin/major
Content-Type: application/json
```

### Request Body

```json
{
  "code": "KTPM",
  "name": "Kỹ thuật phần mềm",
  "orgUnitId": "uuid-khoa-cntt",
  "isActive": true
}
```

### Validation Rules

| Field       | Bắt buộc | Rules                |
| ----------- | -------- | -------------------- |
| `code`      | ✅       | Unique, max 50 ký tự |
| `name`      | ✅       | Max 255 ký tự        |
| `orgUnitId` | ❌       | UUID đơn vị tổ chức  |
| `isActive`  | ❌       | Default: `true`      |

### Response ✅ 201

```json
{
  "id": "uuid-new",
  "code": "KTPM",
  "name": "Kỹ thuật phần mềm",
  "isActive": true,
  "createdAt": "2024-12-20T13:00:00+07:00",
  "updatedAt": "2024-12-20T13:00:00+07:00",
  "orgUnit": { ... },
  "subjectCount": 0,
  "studentCount": 0
}
```

### Lỗi

| Status | Khi nào                                   |
| ------ | ----------------------------------------- |
| ❌ 400 | Thiếu field bắt buộc hoặc validation fail |
| ❌ 404 | `orgUnitId` không tồn tại                 |
| ❌ 409 | `code` đã tồn tại trong hệ thống          |

---

## 4. Cập nhật

```
PUT /admin/major/:id
Content-Type: application/json
```

### Request Body

```json
{
  "name": "Kỹ thuật phần mềm (cập nhật)",
  "isActive": false
}
```

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "code": "KTPM",
  "name": "Kỹ thuật phần mềm (cập nhật)",
  "isActive": false,
  ...
}
```

### Lỗi

| Status | Khi nào                   |
| ------ | ------------------------- |
| ❌ 404 | ID không tồn tại          |
| ❌ 404 | `orgUnitId` không tồn tại |
| ❌ 409 | `code` mới đã tồn tại     |

---

## 5. Xóa

```
DELETE /admin/major/:id
```

### Response ✅ 204

Không có body.

### Lỗi

| Status | Khi nào                                              |
| ------ | ---------------------------------------------------- |
| ❌ 404 | ID không tồn tại                                     |
| ❌ 409 | **Ngành đang có sinh viên theo học**                 |
| ❌ 409 | **Ngành đang có môn học trong chương trình đào tạo** |

> ⚠️ **Lưu ý:** Không thể xóa ngành học nếu:
>
> 1. Có `StudentProfile` đang theo học ngành này
> 2. Có `MajorSubject` (chương trình đào tạo)

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useMajors.ts
import useSWR from "swr";

export function useMajors(params: ListMajorRequest = {}) {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.sortBy) searchParams.set("sortBy", params.sortBy);
  if (params.sortDir) searchParams.set("sortDir", params.sortDir);
  if (params.q) searchParams.set("q", params.q);
  if (params.isActive !== undefined)
    searchParams.set("isActive", String(params.isActive));
  if (params.orgUnitId) searchParams.set("orgUnitId", params.orgUnitId);

  const { data, error, mutate } = useSWR(
    `/admin/major?${searchParams}`,
    fetcher
  );

  return {
    majors: (data?.data ?? []) as MajorResponse[],
    meta: data?.meta,
    isLoading: !error && !data,
    mutate,
  };
}

export function useMajorDetail(id: string | null) {
  const { data, error, mutate } = useSWR(
    id ? `/admin/major/${id}` : null,
    fetcher
  );

  return {
    major: data as MajorDetailResponse | undefined,
    isLoading: !error && !data,
    mutate,
  };
}
```

### CRUD Actions

```typescript
// actions/major.ts
const API_BASE = "/admin/major";

export async function createMajor(data: CreateMajorRequest) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 404) throw new Error("Không tìm thấy đơn vị tổ chức");
  if (res.status === 409) throw new Error("Mã ngành học đã tồn tại");
  if (!res.ok) throw new Error("Tạo ngành học thất bại");
  return res.json() as Promise<MajorResponse>;
}

export async function updateMajor(id: string, data: UpdateMajorRequest) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (res.status === 404) throw new Error("Không tìm thấy ngành học");
  if (res.status === 409) throw new Error("Mã ngành học mới đã tồn tại");
  if (!res.ok) throw new Error("Cập nhật thất bại");
  return res.json() as Promise<MajorResponse>;
}

export async function deleteMajor(id: string) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) throw new Error("Không tìm thấy ngành học");
  if (res.status === 409)
    throw new Error("Không thể xóa ngành học đang có ràng buộc dữ liệu");
  if (!res.ok) throw new Error("Xóa thất bại");
}
```
