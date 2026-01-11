# Debug Guide: API GET /api/exams/available trả về []

## Vấn đề
API `GET /api/exams/available` trả về mảng rỗng `[]` mặc dù sinh viên nằm trong lớp và có exam.

## Nguyên nhân có thể
1. **Query JPQL sai** - JOIN syntax không đúng với entity mapping
2. **Dữ liệu không đúng** - Exam không có status ACTIVE hoặc thời gian sai
3. **Student code sai** - Sinh viên không có trong bảng class_members
4. **Thời gian sai** - Exam chưa bắt đầu hoặc đã kết thúc

## Đã sửa
✅ **Fixed Query**: Thay JOIN bằng subquery để tránh lỗi entity mapping
```sql
-- CŨ (có thể lỗi)
JOIN Class_Member cm ON cm.classField.id = e.classEntity.id

-- MỚI (đã sửa)  
WHERE e.classEntity.id IN (
    SELECT cm.classField.id FROM Class_Member cm 
    WHERE cm.studentCode = :studentCode
)
```

## Debug Steps

### 1. Kiểm tra sinh viên thuộc lớp nào
```http
GET /debug/exams/student-classes/{studentCode}
```
**Kết quả mong đợi**: Danh sách class IDs và class codes

### 2. Kiểm tra tất cả exams của sinh viên
```http
GET /debug/exams/student-all-exams/{studentCode}
```
**Kết quả mong đợi**: Danh sách tất cả exams (không filter thời gian)

### 3. Kiểm tra available exams với debug info
```http
GET /debug/exams/student-available/{studentCode}
```
**Kết quả**: 
- `currentTime`: Thời gian hiện tại
- `availableExams`: Exams available (có filter thời gian)
- `allExams`: Tất cả exams của sinh viên
- `studentClasses`: Lớp học của sinh viên

### 4. Kiểm tra tất cả active exams
```http
GET /debug/exams/active
```
**Kết quả mong đợi**: Danh sách tất cả exams có status ACTIVE

## Cách test

### Bước 1: Kiểm tra dữ liệu cơ bản
```bash
# 1. Kiểm tra sinh viên có trong lớp không
curl -H "Authorization: Bearer {token}" \
  http://localhost:8386/debug/exams/student-classes/{studentCode}

# 2. Kiểm tra có exam nào không  
curl -H "Authorization: Bearer {token}" \
  http://localhost:8386/debug/exams/student-all-exams/{studentCode}
```

### Bước 2: Kiểm tra thời gian và status
```bash
# 3. Kiểm tra active exams
curl -H "Authorization: Bearer {token}" \
  http://localhost:8386/debug/exams/active

# 4. Debug full info
curl -H "Authorization: Bearer {token}" \
  http://localhost:8386/debug/exams/student-available/{studentCode}
```

## Checklist Debug

- [ ] Sinh viên có trong bảng `class_members`?
- [ ] Class ID trong `class_members` khớp với `exams.class_id`?
- [ ] Exam có `status = 'ACTIVE'`?
- [ ] `startTime <= now() <= endTime`?
- [ ] Student code đúng format?

## Các trường hợp thường gặp

### Case 1: Sinh viên không trong lớp
```json
// GET /debug/exams/student-classes/{studentCode}
[] // Mảng rỗng
```
**Giải pháp**: Thêm sinh viên vào lớp qua API class management

### Case 2: Exam chưa ACTIVE
```json
// Exam có status = 'DRAFT' hoặc 'PUBLISHED'
{
  "status": "PUBLISHED", // Cần là "ACTIVE"
  "startTime": "2024-12-30T09:00:00",
  "endTime": "2024-12-30T12:00:00"
}
```
**Giải pháp**: Activate exam qua API `PUT /api/exams/{examId}/activate`

### Case 3: Thời gian sai
```json
{
  "currentTime": "2024-12-30T08:00:00",
  "startTime": "2024-12-30T09:00:00", // Chưa bắt đầu
  "endTime": "2024-12-30T12:00:00"
}
```
**Giải pháp**: Đợi đến thời gian bắt đầu hoặc sửa thời gian exam

## Sau khi debug

1. **Xóa debug controller** khi đã fix xong
2. **Test lại API chính**: `GET /api/exams/available`
3. **Verify kết quả** với frontend

## Query đã sửa

```java
@Query("SELECT e FROM Exam e " +
       "WHERE e.classEntity.id IN (" +
       "    SELECT cm.classField.id FROM Class_Member cm " +
       "    WHERE cm.studentCode = :studentCode" +
       ") " +
       "AND e.status = 'ACTIVE' " +
       "AND e.startTime <= :now " +
       "AND e.endTime > :now " +
       "ORDER BY e.startTime ASC")
List<Exam> findAvailableExamsForStudent(@Param("studentCode") String studentCode, @Param("now") LocalDateTime now);
```