# Manual Class & Student Management API Guide

## Tổng quan

Hệ thống cung cấp API để giảng viên quản lý lớp học phần và sinh viên bằng cách thủ công, không cần file Excel.

## Luồng nghiệp vụ

### 1. Tạo lớp học phần thủ công
- Giảng viên tạo lớp học phần mới
- Hệ thống tự động tạo notebook cộng đồng
- Gán notebook cho lớp học phần

### 2. Thêm sinh viên thủ công
- Giảng viên nhập thông tin sinh viên từng người
- Hệ thống kiểm tra trùng lặp trong cùng môn học
- Tự động tạo tài khoản nếu sinh viên chưa có
- Gửi email thông báo tài khoản mới
- Thêm sinh viên vào lớp và notebook

## API Endpoints

### 1. Tạo lớp học phần thủ công

```http
POST /api/lecturer/manual-class-management/create-class
Content-Type: application/json
X-User-Id: {lecturer-uuid}

{
  "className": "Lớp 01 - Java Programming",
  "subjectId": "subject-uuid-here",
  "room": "A101",
  "dayOfWeek": 2,
  "periods": "1-3",
  "note": "Lớp học buổi sáng"
}
```

**Response thành công:**
```json
{
  "success": true,
  "message": "Tạo lớp học phần thành công",
  "classId": "class-uuid-here",
  "className": "Lớp 01 - Java Programming",
  "subjectName": "Lập trình Java"
}
```

**Response lỗi:**
```json
{
  "success": false,
  "message": "Giảng viên chưa được phân công dạy môn học này",
  "classId": null,
  "className": null,
  "subjectName": null
}
```

### 2. Thêm sinh viên thủ công

```http
POST /api/lecturer/manual-class-management/add-student
Content-Type: application/json

{
  "classId": "class-uuid-here",
  "studentCode": "2021001",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "2000-01-01",
  "email": "student@example.com"
}
```

**Response thành công (tài khoản mới):**
```json
{
  "success": true,
  "message": "Đã thêm sinh viên và tạo tài khoản mới",
  "userCreated": true,
  "emailSent": true,
  "studentCode": "2021001",
  "fullName": "Nguyễn Văn A",
  "email": "student@example.com"
}
```

**Response thành công (tài khoản đã có):**
```json
{
  "success": true,
  "message": "Đã thêm sinh viên (tài khoản đã tồn tại)",
  "userCreated": false,
  "emailSent": false,
  "studentCode": "2021001",
  "fullName": "Nguyễn Văn A",
  "email": "student@example.com"
}
```

**Response lỗi (trùng lặp):**
```json
{
  "success": false,
  "message": "Sinh viên đã tồn tại trong môn học này",
  "userCreated": false,
  "emailSent": false,
  "studentCode": "2021001",
  "fullName": "Nguyễn Văn A",
  "email": "student@example.com"
}
```

## Validation Rules

### Tạo lớp học phần
- `className`: Bắt buộc, không được để trống
- `subjectId`: Bắt buộc, phải là UUID hợp lệ
- `room`, `dayOfWeek`, `periods`, `note`: Tùy chọn

### Thêm sinh viên
- `classId`: Bắt buộc, phải là UUID hợp lệ
- `studentCode`: Bắt buộc, không được để trống
- `fullName`: Bắt buộc, không được để trống
- `email`: Bắt buộc, phải là email hợp lệ
- `dateOfBirth`: Tùy chọn, định dạng YYYY-MM-DD

## Business Logic

### Kiểm tra trùng lặp
- Hệ thống kiểm tra trùng lặp theo `(studentCode + subjectId)`
- Không cho phép thêm sinh viên đã tồn tại trong cùng môn học

### Tạo tài khoản tự động
- Kiểm tra user theo email (không theo MSSV)
- Nếu chưa có: tạo user mới với role "student"
- Tạo mật khẩu ngẫu nhiên 8 ký tự
- Gửi email thông báo tài khoản

### Quản lý notebook
- Tự động tạo notebook cộng đồng cho lớp mới
- Thêm giảng viên làm owner
- Thêm sinh viên làm member với role "student"

## Email Template

Khi tạo tài khoản mới, hệ thống gửi email:

```
Subject: Tài khoản hệ thống học tập đã được tạo

Xin chào [Họ tên],

Tài khoản hệ thống học tập của bạn đã được tạo thành công.

Thông tin đăng nhập:
- Email: [email]
- Mật khẩu: [password]
- MSSV: [studentCode]

Vui lòng truy cập hệ thống tại: [baseUrl]

LƯU Ý QUAN TRỌNG:
- Vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên
- Không chia sẻ thông tin đăng nhập với người khác
- Liên hệ giảng viên nếu có vấn đề về tài khoản

Trân trọng,
Hệ thống quản lý học tập
```
## Test Examples

### Tạo lớp mới
```bash
curl -X POST "http://localhost:8386/api/lecturer/manual-class-management/create-class" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: lecturer-uuid-here" \
  -d '{
    "className": "Lớp 01 - Java Programming",
    "subjectId": "subject-uuid-here",
    "room": "A101",
    "dayOfWeek": 2,
    "periods": "1-3"
  }'
```

### Thêm sinh viên
```bash
curl -X POST "http://localhost:8386/api/lecturer/manual-class-management/add-student" \
  -H "Content-Type: application/json" \
  -d '{
    "classId": "class-uuid-here",
    "studentCode": "2021001",
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "2000-01-01",
    "email": "student@example.com"
  }'
```

## Error Handling

### Lỗi thường gặp
1. **Giảng viên chưa được phân công**: Không tìm thấy teaching assignment
2. **Sinh viên đã tồn tại**: Trùng lặp trong cùng môn học
3. **Lớp không tồn tại**: ClassId không hợp lệ
4. **Email không hợp lệ**: Định dạng email sai
5. **Lỗi gửi email**: Cấu hình SMTP không đúng

### Xử lý lỗi
- Tất cả lỗi đều được log chi tiết
- Response luôn có trường `success` để frontend kiểm tra
- Message lỗi rõ ràng, dễ hiểu cho người dùng

## Integration với Excel Import

Logic tạo user và gửi email được thiết kế để tái sử dụng:
- `UserManagementService.findOrCreateStudentUser()` 
- `EmailService.sendNewAccountEmail()`

Có thể tích hợp vào luồng import Excel để tự động tạo tài khoản cho sinh viên chưa có.