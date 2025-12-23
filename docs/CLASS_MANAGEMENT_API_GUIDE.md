# API Quản lý Lớp học phần - Hướng dẫn sử dụng

## Tổng quan

API này cung cấp các tính năng để giảng viên quản lý lớp học phần và import sinh viên từ file Excel.

### Các tính năng chính:
1. **Tạo lớp học phần + import sinh viên** (Luồng A)
2. **Import sinh viên vào lớp có sẵn** (Luồng B)  
3. **Preview dữ liệu Excel** trước khi import

## Cấu trúc File Excel

File Excel cần có các cột theo thứ tự:
- **Cột A**: `student_code` (MSSV) - Bắt buộc
- **Cột B**: `full_name` (Họ tên) - Bắt buộc  
- **Cột C**: `date_of_birth` (Ngày sinh) - Tùy chọn

### Ví dụ file Excel:
```
| student_code | full_name      | date_of_birth |
|-------------|----------------|---------------|
| 2021001     | Nguyễn Văn A   | 01/01/2003   |
| 2021002     | Trần Thị B     | 15/05/2003   |
```

## API Endpoints

### 1. Tạo lớp học phần + Import sinh viên (Luồng A)

**POST** `/api/lecturer/class-management/create-with-students`

**Headers:**
```
Content-Type: multipart/form-data
X-User-Id: {lecturer_uuid}
```

**Body (Form Data):**
```
excelFile: [File Excel]
className: "Lớp 01 - Lập trình Java"
subjectId: "uuid-of-subject"
teachingAssignmentId: "uuid-of-teaching-assignment"
room: "A101" (optional)
dayOfWeek: 2 (optional, 2=Thứ 3)
periods: "1-3" (optional)
note: "Ghi chú" (optional)
```

**Response:**
```json
{
  "success": true,
  "message": "Tạo lớp học phần và import sinh viên thành công",
  "data": {
    "totalRows": 50,
    "successCount": 45,
    "duplicateCount": 3,
    "errorCount": 2,
    "duplicates": [
      {
        "rowNumber": 5,
        "studentCode": "2021005",
        "fullName": "Nguyễn Văn E",
        "reason": "Sinh viên đã tồn tại trong môn học này"
      }
    ],
    "errors": [
      {
        "rowNumber": 10,
        "studentCode": "",
        "fullName": "Trần Văn K",
        "reason": "Mã sinh viên không được để trống"
      }
    ]
  }
}
```

### 2. Import sinh viên vào lớp có sẵn (Luồng B)

**POST** `/api/lecturer/class-management/import-students`

**Headers:**
```
Content-Type: multipart/form-data
```

**Body (Form Data):**
```
excelFile: [File Excel]
classId: "uuid-of-existing-class"
```

**Response:** Giống như API tạo lớp mới

### 3. Preview dữ liệu Excel

**POST** `/api/lecturer/class-management/preview-excel`

**Headers:**
```
Content-Type: multipart/form-data
```

**Body (Form Data):**
```
excelFile: [File Excel]
classId: "uuid-of-class" (optional - để check trùng)
```

**Response:** Giống như các API khác nhưng không lưu vào database

## Quy tắc nghiệp vụ

### 1. Kiểm tra trùng sinh viên
- Một sinh viên **KHÔNG được** xuất hiện 2 lần trong cùng một môn học
- Điều kiện check: `(student_code + subject_id)`
- Sinh viên có thể học nhiều môn khác nhau

### 2. Notebook tự động
- Mỗi lớp học phần gắn với 1 notebook cộng đồng
- Notebook được tạo tự động hoặc sử dụng lại từ Teaching Assignment
- Sinh viên import vào lớp sẽ tự động thành member của notebook với role "student"

### 3. Xử lý lỗi
- **Transaction rollback** nếu có lỗi nghiêm trọng
- Sinh viên lỗi sẽ được báo cáo chi tiết
- Sinh viên hợp lệ vẫn được import thành công

## Ví dụ sử dụng với cURL

### Tạo lớp mới:
```bash
curl -X POST "http://localhost:8386/api/lecturer/class-management/create-with-students" \
  -H "X-User-Id: lecturer-uuid-here" \
  -F "excelFile=@students.xlsx" \
  -F "className=Lớp 01 - Java Programming" \
  -F "subjectId=subject-uuid-here" \
  -F "teachingAssignmentId=assignment-uuid-here" \
  -F "room=A101" \
  -F "dayOfWeek=2"
```

### Import vào lớp có sẵn:
```bash
curl -X POST "http://localhost:8386/api/lecturer/class-management/import-students" \
  -F "excelFile=@students.xlsx" \
  -F "classId=class-uuid-here"
```

### Preview Excel:
```bash
curl -X POST "http://localhost:8386/api/lecturer/class-management/preview-excel" \
  -F "excelFile=@students.xlsx" \
  -F "classId=class-uuid-here"
```

## Lưu ý kỹ thuật

1. **File size limit**: Tối đa 100MB (cấu hình trong application.yml)
2. **Supported formats**: .xlsx (Excel 2007+)
3. **Authentication**: Cần header `X-User-Id` với UUID của giảng viên
4. **Database**: Sử dụng PostgreSQL với transaction support
5. **Logging**: Tất cả hoạt động được log chi tiết

## Troubleshooting

### Lỗi thường gặp:

1. **"Không tìm thấy phân công giảng dạy"**
   - Kiểm tra `teachingAssignmentId` có đúng không
   - Đảm bảo giảng viên có quyền với assignment này

2. **"Sinh viên đã tồn tại trong môn học này"**
   - Sinh viên đã được import vào lớp khác của cùng môn
   - Cần xóa khỏi lớp cũ trước khi import lại

3. **"File Excel không đúng định dạng"**
   - Đảm bảo file là .xlsx
   - Kiểm tra cấu trúc cột theo yêu cầu
   - Dòng đầu tiên là header, dữ liệu bắt đầu từ dòng 2

4. **"Lớp học phần này chưa có notebook được liên kết"**
   - Teaching Assignment chưa có notebook
   - Cần tạo notebook cho assignment trước