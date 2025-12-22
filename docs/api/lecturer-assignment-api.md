# 📚 API Lấy Danh Sách Lớp Học Phần Của Giảng Viên

> **Base URL:** `http://localhost:8386/lecturer/assignments`  
> **Auth:** Cần Bearer Token (role LECTURER)

---

## 📋 Tổng quan

API này cho phép giảng viên xem danh sách các lớp học phần được phân công và gửi yêu cầu dạy môn mới.

| API                 | URL                             | Method | Mô tả                                 |
| ------------------- | ------------------------------- | ------ | ------------------------------------- |
| Danh sách phân công | `/lecturer/assignments`         | GET    | Lấy danh sách phân công có phân trang |
| **Xin dạy môn học** | `/lecturer/assignments/request` | POST   | Gửi yêu cầu xin dạy môn trong học kỳ  |

---

## 📦 TypeScript Interfaces

```typescript
// === REQUEST ===

interface ListMyAssignmentsParams {
  termId?: string; // UUID học kỳ - filter theo học kỳ cụ thể
  status?: string; // Trạng thái phê duyệt: "PENDING" | "APPROVED" | "REJECTED"
  termStatus?: string; // Trạng thái thời gian học kỳ: "ACTIVE" | "UPCOMING" | "PAST"
  page?: number; // Trang (bắt đầu từ 0), default: 0
  size?: number; // Số item mỗi trang, default: 10
}

// Yêu cầu xin dạy môn học
interface RequestTeachingRequest {
  termId: string; // UUID học kỳ muốn dạy (required)
  subjectId: string; // UUID môn học muốn dạy (required)
  note?: string; // Ghi chú (optional)
}

// === RESPONSE ===

interface LecturerAssignmentResponse {
  id: string; // UUID của phân công
  subjectCode: string; // Mã môn học
  subjectName: string; // Tên môn học
  termName: string; // Tên học kỳ
  status: string; // Trạng thái (ACTIVE/INACTIVE)
  approvalStatus: string; // Trạng thái phê duyệt: PENDING | APPROVED | REJECTED
  classCount: number; // Số lớp học phần
  studentCount: number; // Số sinh viên đã đăng ký
  createdAt: string; // Thời gian tạo (ISO datetime)
  termStatus: string; // Trạng thái học kỳ: ACTIVE | UPCOMING | PAST
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

## 🆕 Xin dạy môn học (Request Teaching)

```
POST /lecturer/assignments/request
```

### Request Body

```json
{
  "termId": "7ef2a9a7-cb2a-46f2-8440-fcad43230a61",
  "subjectId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "note": "Tôi có kinh nghiệm 5 năm giảng dạy môn này"
}
```

### Response ✅ 200

```json
{
  "id": "new-assignment-uuid",
  "subjectCode": "INF30087",
  "subjectName": "Cấu trúc dữ liệu và giải thuật",
  "termName": "Học kỳ 1 - Năm học 2024-2025",
  "status": "ACTIVE",
  "approvalStatus": "PENDING",
  "classCount": 0,
  "studentCount": 0,
  "createdAt": "2024-12-21T23:15:00+07:00",
  "termStatus": "UPCOMING"
}
```

### Error Responses

| Status | Message                                         |
| ------ | ----------------------------------------------- |
| 400    | Bạn đã đăng ký dạy môn này trong học kỳ này rồi |
| 404    | Không tìm thấy học kỳ / môn học                 |

---

## 💡 Ví dụ React - Xin dạy môn học

### Service API

```typescript
// lib/api/lecturer-assignments.ts
export async function requestTeaching(data: RequestTeachingRequest) {
  return fetcher<LecturerAssignmentResponse>("/lecturer/assignments/request", {
    method: "POST",
    body: JSON.stringify(data),
  });
}
```

### React Hook

```typescript
// hooks/useRequestTeaching.ts
import useSWRMutation from "swr/mutation";

export function useRequestTeaching() {
  const { trigger, isMutating, error } = useSWRMutation(
    "request-teaching",
    (_, { arg }: { arg: RequestTeachingRequest }) => requestTeaching(arg)
  );

  return { submit: trigger, isLoading: isMutating, error };
}
```

### Component Dialog

```tsx
// components/lecturer/request-teaching-dialog.tsx
"use client";

import { useState } from "react";
import { useRequestTeaching } from "@/hooks/useRequestTeaching";
import { TermSelect, SubjectSelect } from "@/components/selects";

export function RequestTeachingDialog({ onSuccess }) {
  const [termId, setTermId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [note, setNote] = useState("");
  const { submit, isLoading } = useRequestTeaching();

  const handleSubmit = async () => {
    try {
      await submit({ termId, subjectId, note });
      onSuccess?.();
    } catch (error) {
      alert(error.message);
    }
  };

  return (
    <div>
      <TermSelect value={termId} onChange={setTermId} />
      <SubjectSelect value={subjectId} onChange={setSubjectId} />
      <textarea
        value={note}
        onChange={(e) => setNote(e.target.value)}
        placeholder="Ghi chú (tùy chọn)"
      />
      <button
        onClick={handleSubmit}
        disabled={isLoading || !termId || !subjectId}
      >
        {isLoading ? "Đang gửi..." : "Gửi yêu cầu"}
      </button>
    </div>
  );
}
```

---

## 🔍 Lấy danh sách phân công

```
GET /lecturer/assignments?page=0&size=10&termStatus=ACTIVE
```

### Query Parameters

| Query Param  | Type   | Default | Mô tả                                                               |
| ------------ | ------ | ------- | ------------------------------------------------------------------- |
| `termId`     | UUID   | -       | Lọc theo học kỳ cụ thể                                              |
| `status`     | String | -       | Lọc theo trạng thái phê duyệt (`PENDING`, `APPROVED`, `REJECTED`)   |
| `termStatus` | String | -       | Lọc theo trạng thái thời gian học kỳ (`ACTIVE`, `UPCOMING`, `PAST`) |
| `page`       | number | 0       | Trang (bắt đầu từ 0)                                                |
| `size`       | number | 10      | Số item mỗi trang                                                   |

### Response ✅ 200

```json
{
  "data": [
    {
      "id": "22bfd357-e85d-40a7-8670-5bb5d545af83",
      "subjectCode": "INF30087",
      "subjectName": "Cấu trúc dữ liệu và giải thuật",
      "termName": "Học kỳ 1 - Năm học 2024-2025",
      "status": "ACTIVE",
      "approvalStatus": "APPROVED",
      "classCount": 3,
      "studentCount": 120,
      "createdAt": "2024-12-20T20:00:00+07:00",
      "termStatus": "ACTIVE"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 🎯 Use Cases

| Mục đích                      | API Call                                      |
| ----------------------------- | --------------------------------------------- |
| Xin dạy môn mới               | `POST /lecturer/assignments/request`          |
| Xem yêu cầu đang chờ duyệt    | `GET /lecturer/assignments?status=PENDING`    |
| Xem phân công đã được duyệt   | `GET /lecturer/assignments?status=APPROVED`   |
| Xem phân công học kỳ hiện tại | `GET /lecturer/assignments?termStatus=ACTIVE` |
