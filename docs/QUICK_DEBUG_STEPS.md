# Quick Debug Steps - Exam Available API

## Vấn đề
API trả về `[]` mặc dù có 2 exam ACTIVE và sinh viên trong class.

## Test ngay các endpoints này:

### 1. Test query đơn giản (không filter thời gian)
```bash
GET /debug/exams/simple-test/{studentCode}
```
**Mong đợi**: Trả về 2 exams nếu query cơ bản hoạt động

### 2. Test thời gian chi tiết  
```bash
GET /debug/exams/time-debug/{studentCode}
```
**Mong đợi**: Xem thời gian hiện tại vs thời gian exam

### 3. Test API chính với log
```bash
GET /api/exams/available
```
**Kiểm tra log**: Sẽ thấy log chi tiết về thời gian từng exam

## Các trường hợp có thể:

### Case 1: Query cơ bản sai
- `/debug/exams/simple-test/{studentCode}` trả về `[]`
- **Nguyên nhân**: Entity mapping hoặc foreign key sai
- **Fix**: Kiểm tra database schema

### Case 2: Thời gian sai
- `/debug/exams/simple-test/{studentCode}` trả về 2 exams
- `/debug/exams/time-debug/{studentCode}` cho thấy thời gian không match
- **Nguyên nhân**: Timezone, format thời gian, hoặc dữ liệu thời gian sai

### Case 3: Service logic sai
- Debug endpoints hoạt động nhưng API chính vẫn `[]`
- **Nguyên nhân**: Logic trong service layer

## Temporary Fix đã áp dụng:

1. **Bỏ filter thời gian** trong query để test query cơ bản
2. **Filter thời gian ở Java** với log chi tiết
3. **Thêm tolerance** ±1 phút cho thời gian

## Nếu vẫn `[]`:

### Kiểm tra database trực tiếp:
```sql
-- 1. Kiểm tra sinh viên trong lớp
SELECT cm.class_id, c.class_code 
FROM class_members cm 
JOIN classes c ON c.id = cm.class_id 
WHERE cm.student_code = 'YOUR_STUDENT_CODE';

-- 2. Kiểm tra exams trong lớp đó
SELECT e.id, e.title, e.status, e.start_time, e.end_time, NOW() as current_time
FROM exams e 
WHERE e.class_id = 'CLASS_ID_FROM_STEP_1'
AND e.status = 'ACTIVE';

-- 3. Kiểm tra thời gian
SELECT 
  e.start_time <= NOW() as started,
  e.end_time > NOW() as not_expired,
  e.start_time, e.end_time, NOW()
FROM exams e 
WHERE e.class_id = 'CLASS_ID_FROM_STEP_1';
```

## Sau khi fix:
1. Revert temporary changes trong service
2. Sử dụng lại query gốc với thời gian
3. Xóa debug controller