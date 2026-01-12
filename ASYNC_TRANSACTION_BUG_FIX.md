# Phân Tích Lỗi: Async Transaction Bug

## Lỗi

```
java.lang.RuntimeException: File không tồn tại: 241e0e94-f56a-4029-aa28-51764728a8e2
    at FileProcessingTaskService.startAIProcessing(FileProcessingTaskService.java:41)
```

## Nguyên Nhân

### Vấn đề: Transaction Boundary với @Async

1. **Method `startAIProcessing` được đánh dấu `@Async` và `@Transactional`**:
   ```java
   @Async
   @Transactional
   public void startAIProcessing(NotebookFile file) {
       // ...
       NotebookFile loadedFile = fileRepository.findById(fileId)
           .orElseThrow(() -> new RuntimeException("File không tồn tại: " + fileId));
   }
   ```

2. **Flow xảy ra lỗi**:
   ```
   Main Thread (Transaction A):
   1. Save NotebookFile → INSERT vào DB
   2. Gọi startAIProcessing(file) → @Async proxy
   3. Method return ngay (không chờ)
   
   Async Thread (Transaction B):
   4. Thread mới bắt đầu
   5. Tạo transaction MỚI (@Transactional)
   6. Gọi fileRepository.findById(fileId)
   7. ❌ KHÔNG TÌM THẤY vì Transaction A chưa commit!
   ```

3. **Tại sao không tìm thấy?**:
   - Transaction A chưa commit (INSERT chưa được flush)
   - Transaction B (async thread) có isolation level READ_COMMITTED
   - Chưa thấy data từ Transaction A (chưa commit)

## Giải Pháp

### Solution 1: Flush & Commit Trước Khi Gọi Async ✅ (Khuyến nghị)

**Cách làm**: Đảm bảo transaction commit trước khi gọi async method.

```java
// Trong UserNotebookFileService.uploadFiles()
@Transactional
public List<NotebookFile> uploadFiles(...) {
    // ...
    
    NotebookFile savedFile = notebookFileRepository.save(newFile);
    saved.add(savedFile);
    
    // QUAN TRỌNG: Flush để đảm bảo INSERT được ghi vào DB
    notebookFileRepository.flush(); // Hoặc entityManager.flush()
    
    // Hoặc commit transaction trước
    // Nhưng trong cùng method thì chỉ cần flush là đủ
    
    if ("approved".equals(initStatus)) {
        // Bây giờ mới gọi async (sau khi flush)
        fileProcessingTaskService.startAIProcessing(savedFile);
    }
    
    return saved;
}
```

### Solution 2: Reload Entity Trong Async Method (Đã làm, nhưng cần flush)

**Hiện tại code đã reload**:
```java
@Async
@Transactional
public void startAIProcessing(NotebookFile file) {
    UUID fileId = file.getId();
    
    // Reload từ DB (đúng rồi)
    NotebookFile loadedFile = fileRepository.findById(fileId)
        .orElseThrow(() -> new RuntimeException("File không tồn tại: " + fileId));
    // ...
}
```

**Vấn đề**: Vẫn lỗi vì transaction chính chưa commit khi async thread chạy.

**Giải pháp**: Cần flush trước khi gọi async.

### Solution 3: Chỉ Truyền fileId (Thay đổi signature)

**Cách làm**: Thay đổi method signature để chỉ nhận fileId:

```java
// OLD
@Async
@Transactional
public void startAIProcessing(NotebookFile file) { ... }

// NEW
@Async
@Transactional
public void startAIProcessing(UUID fileId) {
    NotebookFile loadedFile = fileRepository.findById(fileId)
        .orElseThrow(() -> new NotFoundException("File không tồn tại: " + fileId));
    // ...
}
```

**Lợi ích**:
- Không phụ thuộc vào entity instance
- Rõ ràng hơn về việc reload từ DB
- Tránh detached entity issues

**Nhược điểm**:
- Cần thay đổi tất cả chỗ gọi method

### Solution 4: Transaction Propagation (Không khuyến nghị)

Có thể dùng `@Transactional(propagation = Propagation.REQUIRES_NEW)` nhưng không giải quyết vấn đề gốc (transaction chính chưa commit).

## Khuyến Nghị

### ✅ Giải pháp đơn giản nhất: Thêm flush()

**Trong tất cả các service gọi `startAIProcessing`**, thêm flush trước khi gọi async:

```java
// UserNotebookFileService.uploadFiles()
NotebookFile savedFile = notebookFileRepository.save(newFile);
saved.add(savedFile);

// THÊM DÒNG NÀY:
entityManager.flush(); // Hoặc notebookFileRepository.flush()

if ("approved".equals(initStatus)) {
    fileProcessingTaskService.startAIProcessing(savedFile);
}
```

**Các chỗ cần sửa**:
1. `UserNotebookFileService.uploadFiles()` - dòng 111
2. `LecturerNotebookFileService.uploadFiles()` - dòng 92
3. `AdminRegulationService.uploadFiles()` - dòng 200
4. `ChapterItemService.uploadFiles()` - dòng 107
5. `AdminNotebookFileService.uploadFiles()` - dòng 97
6. Tất cả các chỗ gọi `startAIProcessing()` sau khi save

## Tại Sao Cần Flush?

- `save()` chỉ đưa entity vào persistence context, chưa ghi DB
- `flush()` ghi thay đổi vào DB ngay (nhưng chưa commit)
- `commit()` mới thực sự commit transaction
- Với `@Transactional`, commit tự động khi method return
- Nhưng async thread có thể chạy TRƯỚC khi transaction commit
- → Cần `flush()` để đảm bảo INSERT đã được ghi trước khi async thread query

## Testing

Sau khi fix, test:
1. Upload file
2. Kiểm tra log không còn lỗi "File không tồn tại"
3. File được xử lý thành công (status = "done")

