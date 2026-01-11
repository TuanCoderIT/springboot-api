# Simple Fix - Exam Available API

## Vấn đề đã xác định:
- SQL queries hoạt động tốt
- JPQL queries có vấn đề với entity mapping
- Nhiều lỗi compile do missing @Slf4j

## Quick Fix:

### 1. Sửa ExamRepository - chỉ giữ 1 method
```java
// Xóa tất cả methods duplicate, chỉ giữ:
@Query(value = "SELECT * FROM exams e " +
               "WHERE e.class_id IN (" +
               "    SELECT cm.class_id FROM class_members cm " +
               "    WHERE cm.student_code = :studentCode" +
               ") " +
               "AND e.status = 'ACTIVE' " +
               "ORDER BY e.start_time ASC", nativeQuery = true)
List<Exam> findAvailableExamsForStudent(@Param("studentCode") String studentCode);
```

### 2. Sửa ExamServiceImpl
```java
@Override
@Transactional(readOnly = true)
public List<ExamResponse> getAvailableExamsForStudent(String studentCode) {
    List<Exam> exams = examRepository.findAvailableExamsForStudent(studentCode);
    System.out.println("Found " + exams.size() + " exams for student " + studentCode);
    
    return exams.stream()
        .map(exam -> mapToExamResponseForStudent(exam, studentCode))
        .collect(Collectors.toList());
}
```

### 3. Test ngay:
```bash
GET /api/exams/available
```

## Root Cause:
Entity mapping `cm.classField.id` trong JPQL không match với database schema `cm.class_id`.

## Solution:
Sử dụng Native SQL thay vì JPQL để match chính xác với SQL queries đã test thành công.