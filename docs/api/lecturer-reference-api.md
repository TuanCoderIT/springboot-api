# 📚 API Dữ Liệu Tham Chiếu Cho Giảng Viên

> **Base URL:** `http://localhost:8386/lecturer/`  
> **Auth:** Cần Bearer Token (role LECTURER)  
> **Quyền hạn:** Read-only (chỉ GET)

---

## 📋 Tổng quan

Các API read-only cho giảng viên lấy dữ liệu tham chiếu: học kỳ, ngành học, môn học, đơn vị tổ chức.

| Resource  | URL                   | Mô tả                   |
| --------- | --------------------- | ----------------------- |
| Học kỳ    | `/lecturer/terms`     | Danh sách học kỳ        |
| Ngành học | `/lecturer/majors`    | Danh sách ngành đào tạo |
| Môn học   | `/lecturer/subjects`  | Danh sách môn học       |
| Đơn vị    | `/lecturer/org-units` | Danh sách đơn vị        |

---

## 📦 TypeScript Interfaces

```typescript
// === SHARED ===

interface PagedResponse<T> {
  data: T[];
  meta: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// === TERMS ===

interface ListTermParams {
  page?: number; // Default: 0
  size?: number; // Default: 10
  sortBy?: string; // Default: "createdAt"
  sortDir?: string; // "asc" | "desc", default: "desc"
  q?: string; // Tìm theo mã, tên học kỳ
  isActive?: boolean; // Filter theo trạng thái
}

interface TermResponse {
  id: string;
  code: string;
  name: string;
  startDate: string; // YYYY-MM-DD
  endDate: string;
  isActive: boolean;
  createdAt: string;
  totalAssignments: number;
}

// === MAJORS ===

interface ListMajorParams {
  page?: number;
  size?: number;
  sortBy?: string; // Default: "code"
  sortDir?: string;
  q?: string; // Tìm theo mã, tên ngành
  isActive?: boolean;
  orgUnitId?: string; // Filter theo đơn vị
}

interface MajorResponse {
  id: string;
  code: string;
  name: string;
  isActive: boolean;
  orgUnit: { id: string; code: string; name: string; type: string } | null;
  subjectCount: number;
  studentCount: number;
  createdAt: string;
  updatedAt: string;
}

// === SUBJECTS ===

interface ListSubjectParams {
  page?: number;
  size?: number;
  sortBy?: string; // Default: "code"
  sortDir?: string;
  q?: string; // Tìm theo mã, tên môn
  isActive?: boolean;
  majorId?: string; // Filter theo ngành
}

interface SubjectResponse {
  id: string;
  code: string;
  name: string;
  credit: number;
  isActive: boolean;
  majorCount: number;
  assignmentCount: number;
  studentCount: number;
  createdAt: string;
  updatedAt: string;
}

// === ORG UNITS ===

interface ListOrgUnitParams {
  page?: number;
  size?: number;
  sortBy?: string; // Default: "createdAt"
  sortDir?: string;
  q?: string; // Tìm theo mã, tên
  type?: string; // Filter: "faculty", "department"
  isActive?: boolean;
}

interface OrgUnitResponse {
  id: string;
  code: string;
  name: string;
  type: string;
  isActive: boolean;
  parent: { id: string; code: string; name: string } | null;
  createdAt: string;
  updatedAt: string;
}
```

---

## 🔍 Chi Tiết API

### 1. Học kỳ (Terms)

#### Lấy danh sách

```
GET /lecturer/terms
```

**Query Parameters:**

| Param      | Type    | Default   | Mô tả                                      |
| ---------- | ------- | --------- | ------------------------------------------ |
| `page`     | number  | 0         | Trang (bắt đầu từ 0)                       |
| `size`     | number  | 10        | Số item mỗi trang                          |
| `q`        | string  | -         | Tìm theo mã, tên học kỳ                    |
| `isActive` | boolean | -         | Filter theo trạng thái (true/false)        |
| `sortBy`   | string  | startDate | Sắp xếp theo field (code, name, startDate) |
| `sortDir`  | string  | desc      | Hướng sắp xếp (asc/desc)                   |

**Response:**

```json
{
  "data": [
    {
      "id": "7ef2a9a7-cb2a-46f2-8440-fcad43230a61",
      "code": "2024_HK1",
      "name": "Học kỳ 1 - Năm học 2024-2025",
      "startDate": "2024-09-01",
      "endDate": "2025-01-15",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "totalAssignments": 150
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

#### Lấy chi tiết

```
GET /lecturer/terms/{id}
```

**Path Parameters:**

| Param | Type | Mô tả         |
| ----- | ---- | ------------- |
| `id`  | UUID | ID của học kỳ |

**Response:** Trả về 1 object `TermResponse` (không có `data` wrapper)

---

### 2. Ngành học (Majors)

```
GET /lecturer/majors
GET /lecturer/majors/{id}
```

**Query Parameters (GET /lecturer/majors):**

| Param       | Type    | Default | Mô tả                            |
| ----------- | ------- | ------- | -------------------------------- |
| `page`      | number  | 0       | Trang (bắt đầu từ 0)             |
| `size`      | number  | 10      | Số item mỗi trang                |
| `q`         | string  | -       | Tìm theo mã, tên ngành           |
| `isActive`  | boolean | -       | Filter theo trạng thái           |
| `orgUnitId` | UUID    | -       | Filter theo đơn vị (Khoa/Bộ môn) |

**Response:**

```json
{
  "data": [
    {
      "id": "uuid",
      "code": "CNTT",
      "name": "Công nghệ Thông tin",
      "isActive": true,
      "orgUnit": {
        "id": "uuid",
        "code": "KHOA_CNTT",
        "name": "Khoa Công nghệ Thông tin",
        "type": "faculty"
      },
      "subjectCount": 45,
      "studentCount": 500,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-01-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

---

### 3. Môn học (Subjects)

#### Lấy danh sách

```
GET /lecturer/subjects
```

**Query Parameters:**

| Param      | Type    | Default | Mô tả                  |
| ---------- | ------- | ------- | ---------------------- |
| `page`     | number  | 0       | Trang (bắt đầu từ 0)   |
| `size`     | number  | 10      | Số item mỗi trang      |
| `q`        | string  | -       | Tìm theo mã, tên môn   |
| `isActive` | boolean | -       | Filter theo trạng thái |
| `majorId`  | UUID    | -       | Filter theo ngành học  |

**Response:**

```json
{
  "data": [
    {
      "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
      "code": "INF30087",
      "name": "Cấu trúc dữ liệu và giải thuật",
      "credit": 3,
      "isActive": true,
      "majorCount": 2,
      "assignmentCount": 5,
      "studentCount": 120,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-01-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

#### Lấy chi tiết

```
GET /lecturer/subjects/{id}
```

**Path Parameters:**

| Param | Type | Mô tả          |
| ----- | ---- | -------------- |
| `id`  | UUID | ID của môn học |

**Response:** Trả về 1 object `SubjectResponse` (không có `data` wrapper)

---

### 4. Đơn vị tổ chức (OrgUnits)

```
GET /lecturer/org-units
GET /lecturer/org-units/{id}
```

**Response GET /lecturer/org-units:**

```json
{
  "data": [
    {
      "id": "uuid",
      "code": "KHOA_CNTT",
      "name": "Khoa Công nghệ Thông tin",
      "type": "faculty",
      "isActive": true,
      "parent": null,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-01-01T00:00:00+07:00"
    }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

---

## 💡 Ví dụ React Hook

```typescript
// hooks/useLecturerData.ts
import useSWR from "swr";
import { fetcher } from "@/lib/fetcher";

export function useTerms(params: ListTermParams = {}) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  if (params.q) searchParams.set("q", params.q);
  if (params.isActive !== undefined)
    searchParams.set("isActive", String(params.isActive));

  return useSWR(`/lecturer/terms?${searchParams.toString()}`, fetcher);
}

export function useMajors(params: ListMajorParams = {}) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  if (params.q) searchParams.set("q", params.q);
  if (params.orgUnitId) searchParams.set("orgUnitId", params.orgUnitId);

  return useSWR(`/lecturer/majors?${searchParams.toString()}`, fetcher);
}

export function useSubjects(params: ListSubjectParams = {}) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  if (params.q) searchParams.set("q", params.q);
  if (params.majorId) searchParams.set("majorId", params.majorId);

  return useSWR(`/lecturer/subjects?${searchParams.toString()}`, fetcher);
}

export function useOrgUnits(params: ListOrgUnitParams = {}) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  if (params.q) searchParams.set("q", params.q);
  if (params.type) searchParams.set("type", params.type);

  return useSWR(`/lecturer/org-units?${searchParams.toString()}`, fetcher);
}
```

---

## ❌ Error Responses

| Status | Khi nào                           |
| ------ | --------------------------------- |
| 401    | Chưa đăng nhập hoặc token hết hạn |
| 403    | Không có quyền LECTURER           |
| 404    | ID không tồn tại                  |

---

## 🎯 Component Select - Tái sử dụng

### TermSelect Component

```tsx
// components/select/term-select.tsx
"use client";

import { useTerms } from "@/hooks/useLecturerData";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export function TermSelect({ value, onChange, disabled }: Props) {
  const { data, isLoading } = useTerms({ size: 100 });

  return (
    <Select
      value={value}
      onValueChange={onChange}
      disabled={disabled || isLoading}
    >
      <SelectTrigger>
        <SelectValue placeholder="Chọn học kỳ" />
      </SelectTrigger>
      <SelectContent>
        {data?.data.map((term) => (
          <SelectItem key={term.id} value={term.id}>
            {term.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

### SubjectSelect Component

```tsx
// components/select/subject-select.tsx
"use client";

import { useSubjects } from "@/hooks/useLecturerData";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  value: string;
  onChange: (value: string) => void;
  majorId?: string; // Filter theo ngành (optional)
  disabled?: boolean;
}

export function SubjectSelect({ value, onChange, majorId, disabled }: Props) {
  const { data, isLoading } = useSubjects({ size: 100, majorId });

  return (
    <Select
      value={value}
      onValueChange={onChange}
      disabled={disabled || isLoading}
    >
      <SelectTrigger>
        <SelectValue placeholder="Chọn môn học" />
      </SelectTrigger>
      <SelectContent>
        {data?.data.map((subject) => (
          <SelectItem key={subject.id} value={subject.id}>
            {subject.code} - {subject.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

---

## 🚀 Use Case: Form Xin Dạy Môn Học

```tsx
// components/lecturer/request-teaching-form.tsx
"use client";

import { useState } from "react";
import { TermSelect } from "@/components/select/term-select";
import { SubjectSelect } from "@/components/select/subject-select";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useRequestTeaching } from "@/hooks/useRequestTeaching";
import { toast } from "sonner";

export function RequestTeachingForm({ onSuccess }: { onSuccess?: () => void }) {
  const [termId, setTermId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [note, setNote] = useState("");
  const { submit, isLoading } = useRequestTeaching();

  const handleSubmit = async () => {
    if (!termId || !subjectId) {
      toast.error("Vui lòng chọn học kỳ và môn học");
      return;
    }

    try {
      await submit({ termId, subjectId, note: note || undefined });
      toast.success("Gửi yêu cầu thành công! Vui lòng chờ Admin duyệt.");
      onSuccess?.();
    } catch (error: any) {
      toast.error(error.message || "Có lỗi xảy ra");
    }
  };

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <label className="text-sm font-medium">Học kỳ *</label>
        <TermSelect value={termId} onChange={setTermId} />
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium">Môn học *</label>
        <SubjectSelect value={subjectId} onChange={setSubjectId} />
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium">Ghi chú</label>
        <Textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Nhập ghi chú (tùy chọn)"
          rows={3}
        />
      </div>

      <Button
        onClick={handleSubmit}
        disabled={isLoading || !termId || !subjectId}
        className="w-full"
      >
        {isLoading ? "Đang gửi..." : "Gửi yêu cầu dạy môn"}
      </Button>
    </div>
  );
}
```

### Hook useRequestTeaching

```typescript
// hooks/useRequestTeaching.ts
import useSWRMutation from "swr/mutation";
import { fetcher } from "@/lib/fetcher";

interface RequestTeachingRequest {
  termId: string;
  subjectId: string;
  note?: string;
}

async function requestTeaching(
  url: string,
  { arg }: { arg: RequestTeachingRequest }
) {
  return fetcher(url, {
    method: "POST",
    body: JSON.stringify(arg),
  });
}

export function useRequestTeaching() {
  const { trigger, isMutating, error } = useSWRMutation(
    "/lecturer/assignments/request",
    requestTeaching
  );

  return {
    submit: trigger,
    isLoading: isMutating,
    error,
  };
}
```
