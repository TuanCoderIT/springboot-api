# 🏢 API Quản lý Đơn vị Tổ chức (OrgUnit)

> **Base URL:** `http://localhost:8386/admin/org-units`  
> **Auth:** Cần Bearer Token (role ADMIN)

---

## 📋 Tổng quan

| API                       | URL                    | Method | Mô tả                       |
| ------------------------- | ---------------------- | ------ | --------------------------- |
| [Danh sách](#1-danh-sách) | `/admin/org-units`     | GET    | Lấy danh sách có phân trang |
| [Chi tiết](#2-chi-tiết)   | `/admin/org-units/:id` | GET    | Lấy 1 đơn vị                |
| [Tạo mới](#3-tạo-mới)     | `/admin/org-units`     | POST   | Tạo đơn vị mới              |
| [Cập nhật](#4-cập-nhật)   | `/admin/org-units/:id` | PUT    | Sửa đơn vị                  |
| [Xóa](#5-xóa)             | `/admin/org-units/:id` | DELETE | Xóa đơn vị                  |

---

## 📦 TypeScript Interfaces

```typescript
// Request tạo mới
interface CreateOrgUnitRequest {
  code: string; // Bắt buộc - Mã đơn vị (unique)
  name: string; // Bắt buộc - Tên đơn vị
  type?: string; // "faculty" | "department" | "center"...
  parentId?: string; // UUID đơn vị cha
  isActive?: boolean; // Default: true
}

// Request cập nhật (tất cả optional)
interface UpdateOrgUnitRequest {
  code?: string;
  name?: string;
  type?: string;
  parentId?: string;
  isActive?: boolean;
}

// Response trả về
interface OrgUnitResponse {
  id: string;
  code: string;
  name: string;
  type: string | null;
  isActive: boolean;
  createdAt: string; // ISO datetime
  updatedAt: string;
  parent: {
    id: string;
    code: string;
    name: string;
  } | null;
}

// Response phân trang
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
GET /admin/org-units?page=0&size=20
```

### Request

| Query Param | Type    | Default     | Mô tả             |
| ----------- | ------- | ----------- | ----------------- |
| `page`      | number  | 0           | Trang (từ 0)      |
| `size`      | number  | 20          | Số item/trang     |
| `sortBy`    | string  | "createdAt" | Sort theo field   |
| `sortDir`   | string  | "desc"      | "asc" hoặc "desc" |
| `q`         | string  | -           | Tìm kiếm          |
| `type`      | string  | -           | Filter loại       |
| `isActive`  | boolean | -           | Filter trạng thái |

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "uuid-1",
      "code": "CNTT",
      "name": "Khoa Công nghệ Thông tin",
      "type": "faculty",
      "isActive": true,
      "createdAt": "2024-01-15T10:30:00+07:00",
      "updatedAt": "2024-01-15T10:30:00+07:00",
      "parent": null
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
GET /admin/org-units/:id
```

### Request

| Path Param | Type | Mô tả     |
| ---------- | ---- | --------- |
| `id`       | UUID | ID đơn vị |

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "code": "HTTT",
  "name": "Bộ môn Hệ thống Thông tin",
  "type": "department",
  "isActive": true,
  "createdAt": "2024-01-16T08:00:00+07:00",
  "updatedAt": "2024-01-16T08:00:00+07:00",
  "parent": {
    "id": "uuid-parent",
    "code": "CNTT",
    "name": "Khoa Công nghệ Thông tin"
  }
}
```

### Lỗi

| Status | Khi nào          |
| ------ | ---------------- |
| ❌ 404 | ID không tồn tại |

---

## 3. Tạo mới

```
POST /admin/org-units
Content-Type: application/json
```

### Request Body

```json
{
  "code": "CNTT",
  "name": "Khoa Công nghệ Thông tin",
  "type": "faculty",
  "parentId": null,
  "isActive": true
}
```

| Field      | Bắt buộc | Mô tả              |
| ---------- | -------- | ------------------ |
| `code`     | ✅       | Mã đơn vị (unique) |
| `name`     | ✅       | Tên đơn vị         |
| `type`     | ❌       | Loại đơn vị        |
| `parentId` | ❌       | ID đơn vị cha      |
| `isActive` | ❌       | Default `true`     |

### Response ✅ 201

```json
{
  "id": "uuid-new",
  "code": "CNTT",
  "name": "Khoa Công nghệ Thông tin",
  "type": "faculty",
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00+07:00",
  "updatedAt": "2024-01-15T10:30:00+07:00",
  "parent": null
}
```

### Lỗi

| Status | Khi nào                  |
| ------ | ------------------------ |
| ❌ 400 | Thiếu `code` hoặc `name` |
| ❌ 404 | `parentId` không tồn tại |
| ❌ 409 | `code` đã tồn tại        |

---

## 4. Cập nhật

```
PUT /admin/org-units/:id
Content-Type: application/json
```

### Request Body

> Chỉ gửi field cần sửa

```json
{
  "name": "Khoa CNTT (đổi tên)",
  "isActive": false
}
```

### Response ✅ 200

```json
{
  "id": "uuid-1",
  "code": "CNTT",
  "name": "Khoa CNTT (đổi tên)",
  "type": "faculty",
  "isActive": false,
  "createdAt": "2024-01-15T10:30:00+07:00",
  "updatedAt": "2024-01-20T15:45:00+07:00",
  "parent": null
}
```

### Lỗi

| Status | Khi nào                  |
| ------ | ------------------------ |
| ❌ 404 | ID không tồn tại         |
| ❌ 404 | `parentId` không tồn tại |
| ❌ 409 | `code` mới bị trùng      |
| ❌ 409 | `parentId` = chính nó    |

---

## 5. Xóa

```
DELETE /admin/org-units/:id
```

### Response ✅ 204

Không có body.

### Lỗi

| Status | Khi nào          |
| ------ | ---------------- |
| ❌ 404 | ID không tồn tại |

---

## 🎨 Type gợi ý

| Value        | Mô tả          |
| ------------ | -------------- |
| `university` | Trường/Đại học |
| `faculty`    | Khoa           |
| `department` | Bộ môn/Phòng   |
| `center`     | Trung tâm      |
| `office`     | Văn phòng      |

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useOrgUnits.ts
import useSWR from "swr";

const fetcher = (url: string) =>
  fetch(url, { headers: { Authorization: `Bearer ${token}` } }).then((res) =>
    res.json()
  );

export function useOrgUnits(page = 0, size = 20) {
  const { data, error, mutate } = useSWR(
    `/admin/org-units?page=${page}&size=${size}`,
    fetcher
  );

  return {
    orgUnits: data?.data ?? [],
    meta: data?.meta,
    isLoading: !error && !data,
    isError: error,
    mutate,
  };
}

// Tạo mới
async function createOrgUnit(data: CreateOrgUnitRequest) {
  const res = await fetch("/admin/org-units", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.message);
  }

  return res.json();
}
```
