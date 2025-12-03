# Hướng Dẫn Phân Quyền (Authorization Guide)

## Tổng quan

Hệ thống đã được tích hợp phân quyền dựa trên **Role-Based Access Control (RBAC)** với Spring Security.

## Các Role trong hệ thống

- **ADMIN**: Quản trị viên hệ thống, có quyền truy cập tất cả các endpoint admin
- **TEACHER**: Giáo viên (có thể mở rộng thêm quyền sau)
- **STUDENT**: Học sinh, chỉ có quyền truy cập các endpoint user

## Cấu trúc phân quyền

### 1. URL-based Authorization (SecurityConfig)

Các endpoint được phân quyền theo URL pattern:

```java
.requestMatchers("/admin/**").hasAnyRole("ADMIN")  // Chỉ ADMIN
.requestMatchers("/user/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")  // Tất cả user
```

### 2. Method-based Authorization (@PreAuthorize)

Sử dụng annotation `@PreAuthorize` để kiểm tra quyền ở method/class level:

```java
@RestController
@RequestMapping("/admin/community")
@PreAuthorize("hasRole('ADMIN')")  // Tất cả methods trong controller này cần ADMIN
public class AdminCommunityController {
    // ...
}
```

### 3. Notebook-level Authorization

Sử dụng `NotebookPermissionChecker` để kiểm tra quyền trong notebook:

```java
@Autowired
private NotebookPermissionChecker permissionChecker;

public void deleteNotebook(UUID notebookId) {
    // Chỉ owner hoặc system admin mới có thể xóa
    if (!permissionChecker.isOwner(notebookId) && !permissionChecker.isSystemAdmin()) {
        throw new UnauthorizedException("Không có quyền xóa notebook");
    }
    // ...
}
```

## Cách sử dụng

### 1. Sử dụng @PreAuthorize trong Controller

```java
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")  // Có thể thêm ở method level
    public List<UserResponse> list() {
        // ...
    }
}
```

### 2. Sử dụng Custom Annotations

```java
import com.example.springboot_api.common.security.RequireAdmin;

@RestController
@RequestMapping("/admin/community")
@RequireAdmin  // Thay vì @PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityController {
    // ...
}
```

### 3. Kiểm tra quyền trong Service Layer

```java
@Service
@RequiredArgsConstructor
public class AdminCommunityService {
    
    private final NotebookPermissionChecker permissionChecker;
    
    @Transactional
    public void delete(UUID id) {
        Notebook nb = notebookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found"));
        
        // Kiểm tra quyền
        if (!permissionChecker.isSystemAdmin() && !permissionChecker.isOwner(id)) {
            throw new UnauthorizedException("Không có quyền xóa");
        }
        
        notebookRepository.deleteById(id);
    }
}
```

## Các phương thức trong NotebookPermissionChecker

### `isOwner(UUID notebookId)`
Kiểm tra user hiện tại có phải owner của notebook không.

### `isAdmin(UUID notebookId)`
Kiểm tra user hiện tại có phải admin hoặc owner của notebook không.

### `isMember(UUID notebookId)`
Kiểm tra user hiện tại có phải member đã approved của notebook không.

### `isSystemAdmin()`
Kiểm tra user hiện tại có phải system admin (role ADMIN) không.

### `getMembership(UUID notebookId)`
Lấy thông tin membership của user hiện tại trong notebook.

## Ví dụ sử dụng

### Ví dụ 1: Bảo vệ endpoint chỉ cho ADMIN

```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public PagedResponse<UserResponse> listUsers() {
    return userService.list();
}
```

### Ví dụ 2: Kiểm tra quyền trong Service

```java
public void updateNotebook(UUID notebookId, UpdateRequest req) {
    Notebook nb = notebookRepository.findById(notebookId)
            .orElseThrow(() -> new NotFoundException("Not found"));
    
    // Chỉ owner hoặc admin của notebook mới có thể update
    if (!permissionChecker.isAdmin(notebookId) && !permissionChecker.isSystemAdmin()) {
        throw new UnauthorizedException("Không có quyền cập nhật");
    }
    
    // Update logic...
}
```

### Ví dụ 3: Kiểm tra quyền khi xóa member

```java
public void deleteMember(UUID memberId) {
    NotebookMember member = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("Not found"));
    
    UUID notebookId = member.getNotebook().getId();
    
    // Chỉ owner hoặc admin của notebook mới có thể xóa member
    if (!permissionChecker.isAdmin(notebookId) && !permissionChecker.isSystemAdmin()) {
        throw new UnauthorizedException("Không có quyền xóa member");
    }
    
    memberRepository.delete(member);
}
```

## Xử lý lỗi

Khi user không có quyền truy cập, hệ thống sẽ trả về:

```json
{
  "status": 403,
  "message": "Không có quyền truy cập. Vui lòng kiểm tra role của bạn.",
  "timestamp": "2024-01-20T10:00:00"
}
```

## Testing

### Test với user STUDENT

```bash
# 1. Đăng nhập với STUDENT
POST /auth/login
{ "email": "student@test.com", "password": "123" }

# 2. Thử truy cập admin endpoint (sẽ bị 403)
GET /admin/community/pending-requests
Cookie: AUTH-TOKEN=xxx
# Response: 403 Forbidden
```

### Test với user ADMIN

```bash
# 1. Đăng nhập với ADMIN
POST /auth/login
{ "email": "admin@test.com", "password": "123" }

# 2. Truy cập admin endpoint (sẽ thành công)
GET /admin/community/pending-requests
Cookie: AUTH-TOKEN=xxx
# Response: 200 OK
```

## Lưu ý quan trọng

1. **ROLE_ prefix**: Spring Security yêu cầu prefix `ROLE_` cho authorities. Đã được xử lý tự động trong `JwtAuthenticationFilter`.

2. **Method Security**: Phải bật `@EnableMethodSecurity` trong `SecurityConfig` để sử dụng `@PreAuthorize`.

3. **Notebook-level permissions**: Sử dụng `NotebookPermissionChecker` để kiểm tra quyền trong notebook, không chỉ dựa vào system role.

4. **SecurityContext**: `NotebookPermissionChecker` tự động lấy user từ `SecurityContext`, không cần truyền tham số.

5. **Exception Handling**: `AccessDeniedException` được xử lý tự động bởi `GlobalExceptionHandler`.

## Best Practices

1. **Luôn kiểm tra quyền ở cả Controller và Service layer** nếu cần bảo mật cao.

2. **Sử dụng `NotebookPermissionChecker`** cho các thao tác liên quan đến notebook thay vì chỉ kiểm tra system role.

3. **Custom annotations** giúp code dễ đọc hơn, nhưng `@PreAuthorize` linh hoạt hơn.

4. **Kiểm tra null** khi sử dụng `getCurrentUser()` để tránh NullPointerException.

5. **Logging**: Nên log các lần truy cập bị từ chối để theo dõi bảo mật.

---

_Tài liệu được tạo: 2024-01-20_

