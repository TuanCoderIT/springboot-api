# Hướng dẫn tích hợp Subject API cho Frontend

Tài liệu này tổng hợp các TypeScript interfaces và cấu trúc API mới nhất dành cho module quản lý Môn học (Subject).

## 1. TypeScript Interfaces (Chuẩn)

```typescript
// --- Cơ bản ---
interface SubjectResponse {
  id: string;
  code: string;
  name: string;
  credit: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  majorCount: number; // Số ngành học có môn này
  assignmentCount: number; // Số đợt phân công giảng dạy
  studentCount: number; // Tổng số sinh viên đã/đang học
}

// --- Chi tiết ---
interface SubjectDetailResponse extends SubjectResponse {
  majors: MajorInSubjectInfo[];
  assignments: AssignmentInfo[];
}

interface MajorInSubjectInfo {
  id: string;
  code: string;
  name: string;
  termNo: number | null;
  isRequired: boolean;
  knowledgeBlock: string | null;
}

interface AssignmentInfo {
  id: string;
  termName: string; // Tên học kỳ (VD: "Học kỳ 1 - 2024-2025")
  lecturerName: string; // Tên giảng viên
  lecturerEmail: string; // Email giảng viên (Mới)
  status: string; // Trạng thái đợt dạy (ACTIVE, CLOSED)
  approvalStatus: string; // Trạng thái phê duyệt (Mới: APPROVED, PENDING)
  note: string | null; // Ghi chú (Mới)
  classCount: number; // Số lớp mở cho đợt này
  createdAt: string;
  classes: ClassInfo[]; // Danh sách chi tiết lớp học (Mới)
}

interface ClassInfo {
  id: string;
  code: string;
  name: string;
  maxStudents: number;
  note: string | null;
  isActive: boolean;
}

// --- Request ---
interface MajorAssignment {
  majorId: string;
  termNo?: number | null;
  isRequired?: boolean;
  knowledgeBlock?: string | null;
}

interface CreateSubjectRequest {
  code: string;
  name: string;
  credit?: number | null;
  isActive?: boolean;
  majorAssignments?: MajorAssignment[];
}

interface UpdateSubjectRequest {
  code?: string;
  name?: string;
  credit?: number | null;
  isActive?: boolean;
  majorAssignments?: MajorAssignment[] | null;
}
```

## 2. Các Endpoint Quan Trọng

### ➕ Tạo mới (`POST /admin/subject`)

Gửi kèm `majorAssignments` để gắn môn vào ngành ngay khi tạo.

### 📝 Cập nhật (`PUT /admin/subject/{id}`)

Lưu ý về field `majorAssignments`:

- **Không gửi / null**: Giữ nguyên liên kết cũ.
- **Mảng rỗng `[]`**: Xóa trắng toàn bộ liên kết ngành.
- **Có dữ liệu**: Ghi đè (Replace) toàn bộ liên kết cũ bằng mảng mới.

### 🔍 Danh sách (`GET /admin/subject`)

Hỗ trợ các query params:

- `page`, `size`, `sortBy`, `sortDir`
- `q`: Tìm theo code/name.
- `majorId`: Lọc môn học thuộc một ngành cụ thể.
- `isActive`: Lọc theo trạng thái.

## 3. Lưu ý Logic

- **Môn học không có `termNo` hay `knowledgeBlock` riêng**: Các thông tin này chỉ tồn tại khi môn học được gán vào một **Ngành (Major)** nhất định.
- **Xóa môn học**: Backend sẽ tự động xóa các liên kết ngành (`MajorSubject`), nhưng sẽ **CHẶN** xóa nếu môn học đã có dữ liệu phân công giảng dạy (`TeachingAssignment`).
