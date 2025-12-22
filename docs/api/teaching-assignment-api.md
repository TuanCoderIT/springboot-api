# Teaching Assignment API - Admin

API quản lý phân công giảng dạy cho Admin.

## Base URL

```
/admin/teaching-assignments
```

---

## 1. Lấy danh sách phân công

### Request

```http
GET /admin/teaching-assignments?termId={uuid}&teacherId={uuid}&status={string}
```

| Query Param | Type   | Required | Description                                                       |
| ----------- | ------ | -------- | ----------------------------------------------------------------- |
| `termId`    | UUID   | ❌       | Lọc theo học kỳ                                                   |
| `teacherId` | UUID   | ❌       | Lọc theo giảng viên                                               |
| `status`    | String | ❌       | Lọc theo trạng thái phê duyệt (`PENDING`, `APPROVED`, `REJECTED`) |

### Response

```json
[
  {
    "id": "22bfd357-e85d-40a7-8670-5bb5d545af83",
    "term": {
      "id": "7ef2a9a7-cb2a-46f2-8440-fcad43230a61",
      "code": "2018_HK1",
      "name": "Học kỳ 1 - Năm học 2018-2019",
      "startDate": "2018-09-01",
      "endDate": "2019-01-15",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "totalAssignments": 15
    },
    "subject": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "code": "INF30087",
      "name": "Cấu trúc dữ liệu và giải thuật",
      "credit": 3,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00+07:00",
      "updatedAt": "2024-01-01T00:00:00+07:00",
      "majorCount": 2,
      "assignmentCount": 5,
      "studentCount": 120
    },
    "teacher": {
      "id": "22bfd357-e85d-40a7-8670-5bb5d545af83",
      "email": "22574802011019@vinhuni.edu.vn",
      "fullName": "NGUYỄN XUÂN HUỲNH",
      "teacherCode": "GV001",
      "academicDegree": "MASTER",
      "academicRank": "LECTURER",
      "orgUnit": {
        "id": "uuid",
        "code": "CNTT",
        "name": "Khoa Công nghệ thông tin",
        "type": "FACULTY"
      }
    },
    "status": "ACTIVE",
    "approvalStatus": "APPROVED",
    "createdBy": "ADMIN",
    "approvedBy": "991c40a1-c2b1-4e62-972a-33deafd708ff",
    "approvedAt": "2024-12-20T21:00:00+07:00",
    "createdAt": "2024-12-20T20:00:00+07:00",
    "note": "Phân công giảng dạy môn CTDL&GT HK1/2018-2019",
    "classCount": 3,
    "studentCount": 12
  }
]
```

---

## 2. Tạo phân công mới

### Request

```http
POST /admin/teaching-assignments
Content-Type: application/json
```

```json
{
  "termId": "7ef2a9a7-cb2a-46f2-8440-fcad43230a61",
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "teacherUserId": "22bfd357-e85d-40a7-8670-5bb5d545af83",
  "note": "Phân công giảng dạy môn CTDL&GT"
}
```

| Field           | Type   | Required | Description             |
| --------------- | ------ | -------- | ----------------------- |
| `termId`        | UUID   | ✅       | ID học kỳ               |
| `subjectId`     | UUID   | ✅       | ID môn học              |
| `teacherUserId` | UUID   | ✅       | ID giảng viên (user ID) |
| `note`          | String | ❌       | Ghi chú                 |

### Response

```json
{
  "id": "22bfd357-e85d-40a7-8670-5bb5d545af83",
  "term": { ... },
  "subject": { ... },
  "teacher": { ... },
  "status": "ACTIVE",
  "approvalStatus": "PENDING",
  "createdBy": "ADMIN",
  "approvedBy": null,
  "approvedAt": null,
  "createdAt": "2024-12-20T22:00:00+07:00",
  "note": "Phân công giảng dạy môn CTDL&GT",
  "classCount": 0,
  "studentCount": 0
}
```

> **Lưu ý:** Khi tạo phân công, trạng thái mặc định là `PENDING` (chờ duyệt). Admin cần phê duyệt sau đó.

---

## 3. Phê duyệt / Từ chối phân công

### Request

```http
PATCH /admin/teaching-assignments/{id}/approval
Content-Type: application/json
```

```json
{
  "status": "APPROVED",
  "note": "Đã xem xét và phê duyệt"
}
```

| Field    | Type   | Required | Description                            |
| -------- | ------ | -------- | -------------------------------------- |
| `status` | String | ✅       | Trạng thái: `APPROVED` hoặc `REJECTED` |
| `note`   | String | ❌       | Ghi chú lý do                          |

### Response

```json
{
  "id": "...",
  "approvalStatus": "APPROVED",
  "approvedBy": "991c40a1-c2b1-4e62-972a-33deafd708ff",
  "approvedAt": "2024-12-20T22:30:00+07:00",
  "note": "Đã xem xét và phê duyệt",
  ...
}
```

---

## 4. Xóa phân công

### Request

```http
DELETE /admin/teaching-assignments/{id}
```

### Response

```
204 No Content
```

---

## TypeScript Interfaces

```typescript
interface AssignmentResponse {
  id: string;
  term: TermResponse;
  subject: SubjectResponse;
  teacher: LecturerResponse;
  status: string;
  approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
  createdBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
  note: string | null;
  classCount: number;
  studentCount: number;
}

interface CreateAssignmentRequest {
  termId: string;
  subjectId: string;
  teacherUserId: string;
  note?: string;
}

interface ApproveAssignmentRequest {
  status: "APPROVED" | "REJECTED";
  note?: string;
}
```

---

## Error Responses

| Status | Error                       | Description                 |
| ------ | --------------------------- | --------------------------- |
| 404    | `Không tìm thấy học kỳ`     | termId không tồn tại        |
| 404    | `Không tìm thấy môn học`    | subjectId không tồn tại     |
| 404    | `Không tìm thấy giảng viên` | teacherUserId không tồn tại |
| 404    | `Không tìm thấy phân công`  | Assignment ID không tồn tại |
| 403    | `Forbidden`                 | Không có quyền Admin        |
