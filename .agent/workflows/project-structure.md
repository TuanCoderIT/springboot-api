---
description: Cấu trúc dự án Spring Boot API và coding conventions
---

# 📁 Project Structure - Spring Boot API

## 🏗️ Cấu Trúc Thư Mục

```
src/main/java/com/example/springboot_api/
├── SpringbootApiApplication.java    # Entry point
│
├── common/                          # Shared utilities
│   ├── exceptions/                  # Custom exceptions (NotFoundException, ConflictException...)
│   └── security/                    # Security utils (UserPrincipal, JwtProvider...)
│
├── config/                          # Configuration classes
│   ├── AI/                          # AI model configs
│   ├── security/                    # Security configs (SecurityConfig, CorsConfig...)
│   ├── websocket/                   # WebSocket configs
│   ├── AsyncConfig.java             # Async thread pool config
│   └── WebConfig.java               # Web MVC config
│
├── controllers/                     # REST Controllers
│   ├── admin/                       # Admin endpoints (/admin/*)
│   ├── shared/                      # Shared endpoints (/auth/*, /public/*)
│   └── user/                        # User endpoints (/user/*)
│
├── dto/                             # Data Transfer Objects
│   ├── admin/                       # Admin DTOs
│   │   ├── lecturer/                # CreateLecturerRequest, LecturerResponse...
│   │   ├── notebook/
│   │   └── user/
│   ├── shared/                      # Shared DTOs
│   │   ├── auth/                    # AuthRequest, AuthResponse...
│   │   ├── ai/                      # AI-related DTOs
│   │   ├── chat/                    # Chat DTOs
│   │   └── PagedResponse.java       # Pagination wrapper
│   └── user/                        # User DTOs
│       ├── notebook/                # NotebookRequest, NotebookResponse...
│       ├── flashcard/               # Flashcard DTOs
│       ├── quiz/                    # Quiz DTOs
│       └── ...                      # Other feature DTOs
│
├── mappers/                         # Entity-to-DTO mappers
│   ├── LecturerMapper.java          # User/TeacherProfile -> LecturerResponse
│   ├── NotebookMapper.java          # Notebook -> NotebookResponse
│   ├── FlashcardMapper.java         # Flashcard -> FlashcardResponse
│   ├── QuizMapper.java              # Quiz -> QuizResponse
│   └── ...                          # Other mappers
│
├── models/                          # JPA Entities
│   ├── User.java                    # User entity
│   ├── Notebook.java                # Notebook entity
│   ├── TeacherProfile.java          # Teacher profile entity
│   └── ...                          # Other entities
│
├── repositories/                    # JPA Repositories
│   ├── admin/                       # Admin repos (UserRepository, LecturerRepository...)
│   ├── shared/                      # Shared repos (AuthRepository, FileRepository...)
│   └── user/                        # User-specific repos
│
├── services/                        # Business Logic
│   ├── admin/                       # Admin services
│   │   ├── LecturerService.java     # CRUD giảng viên
│   │   └── ...
│   ├── shared/                      # Shared services
│   │   ├── ai/                      # AI services (generation, parsing...)
│   │   └── ...
│   └── user/                        # User services
│       ├── NotebookService.java     # Notebook operations
│       └── ...
│
└── utils/                           # Utility classes
    └── UrlNormalizer.java           # URL normalization helper
```

---

## 📝 Coding Conventions

### 1. Naming Conventions

| Element  | Convention  | Example                                  |
| -------- | ----------- | ---------------------------------------- |
| Class    | PascalCase  | `LecturerService`, `NotebookMapper`      |
| Method   | camelCase   | `findByEmail()`, `toLecturerResponse()`  |
| Variable | camelCase   | `lecturerCode`, `orgUnit`                |
| Constant | UPPER_SNAKE | `ROLE_LECTURER`, `TABLE_NAME`            |
| Package  | lowercase   | `controllers.admin`, `dto.user.notebook` |

### 2. DTO Naming

```java
// Request DTOs - suffix "Request"
CreateLecturerRequest.java
UpdateLecturerRequest.java
ListLecturerRequest.java      // For list/filter params

// Response DTOs - suffix "Response" hoặc "Info" (nested)
LecturerResponse.java
OrgUnitInfo.java              // Nested/simple DTO
```

### 3. Repository Pattern

```java
// Đặt trong repositories/{scope}/
// Scope: admin, shared, user

@Repository
public interface LecturerRepository extends JpaRepository<User, UUID> {

    // Custom query methods
    @Query("SELECT u FROM User u WHERE u.role = 'LECTURER'")
    Page<User> findAllLecturers(String search, Pageable pageable);

    Optional<User> findByEmail(String email);
}
```

### 4. Mapper Pattern

```java
// Đặt trong mappers/
@Component
@RequiredArgsConstructor
public class LecturerMapper {

    private final UrlNormalizer urlNormalizer;  // Inject dependencies nếu cần

    public LecturerResponse toLecturerResponse(User user) {
        if (user == null) return null;

        // Normalize URLs nếu cần
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = urlNormalizer.normalizeToFull(avatarUrl);
        }

        return LecturerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                // ... other fields
                .build();
    }
}
```

### 5. Service Pattern

```java
@Service
@RequiredArgsConstructor
public class LecturerService {

    // 1. Dependencies - inject via constructor (Lombok @RequiredArgsConstructor)
    private final LecturerRepository lecturerRepo;
    private final TeacherProfileRepository teacherProfileRepo;
    private final LecturerMapper lecturerMapper;           // Mapper riêng
    private final BCryptPasswordEncoder encoder;

    // 2. Read operations - @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public LecturerResponse getOne(UUID id) {
        return lecturerRepo.findLecturerById(id)
                .map(lecturerMapper::toLecturerResponse)   // Dùng mapper
                .orElseThrow(() -> new NotFoundException("Không tìm thấy"));
    }

    // 3. Write operations - @Transactional
    @Transactional
    public LecturerResponse create(CreateLecturerRequest req) {
        // Validate business rules
        if (lecturerRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Email đã tồn tại");
        }

        // Create entities
        User user = User.builder().build();
        lecturerRepo.save(user);

        return lecturerMapper.toLecturerResponse(user);
    }

    // 4. Tách logic phức tạp ra private methods
    private void updateTeacherProfile(TeacherProfile profile, UpdateLecturerRequest req) {
        // Update logic
    }
}
```

### 6. Controller Pattern

```java
@RestController
@RequestMapping("/admin/lecturers")
@RequiredArgsConstructor
public class LecturerController {

    private final LecturerService lecturerService;

    @GetMapping
    public ResponseEntity<PagedResponse<LecturerResponse>> list(
            @ModelAttribute ListLecturerRequest req) {
        return ResponseEntity.ok(lecturerService.list(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LecturerResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(lecturerService.getOne(id));
    }

    @PostMapping
    public ResponseEntity<LecturerResponse> create(
            @Valid @RequestBody CreateLecturerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lecturerService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LecturerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLecturerRequest req) {
        return ResponseEntity.ok(lecturerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        lecturerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## ⚡ Best Practices

### Performance

```java
// 1. Dùng readOnly cho read operations
@Transactional(readOnly = true)
public List<User> findAll() { ... }

// 2. Fetch lazy associations khi cần
@Query("SELECT u FROM User u LEFT JOIN FETCH u.teacherProfile WHERE u.id = :id")
Optional<User> findByIdWithProfile(@Param("id") UUID id);

// 3. Pagination cho list queries
Page<User> findAllLecturers(String search, Pageable pageable);
```

### Clean Code

```java
// 1. Early return thay vì nested if
public LecturerResponse get(UUID id) {
    if (id == null) return null;           // Early return

    return lecturerRepo.findById(id)
            .map(lecturerMapper::toLecturerResponse)
            .orElseThrow(() -> new NotFoundException("Not found"));
}

// 2. Optional thay vì null check
Optional.ofNullable(req.getSortBy()).orElse("createdAt");

// 3. Method reference
result.map(lecturerMapper::toLecturerResponse)    // ✅
result.map(u -> lecturerMapper.toLecturerResponse(u))  // ❌

// 4. Builder pattern cho complex objects
User.builder()
    .email(req.getEmail())
    .fullName(req.getFullName())
    .build();
```

### Validation

```java
// DTO validation với Jakarta Validation
@Data
public class CreateLecturerRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được quá 255 ký tự")
    private String fullName;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
```

### Exception Handling

```java
// Custom exceptions trong common/exceptions/
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

// Sử dụng
throw new NotFoundException("Không tìm thấy giảng viên");
throw new ConflictException("Email đã tồn tại");
```

---

## 📋 Checklist Khi Tạo Feature Mới

- [ ] Tạo Entity trong `models/`
- [ ] Tạo Repository trong `repositories/{scope}/`
- [ ] Tạo DTOs trong `dto/{scope}/{feature}/`
  - [ ] `Create{Feature}Request.java`
  - [ ] `Update{Feature}Request.java`
  - [ ] `{Feature}Response.java`
  - [ ] Các nested DTOs nếu cần
- [ ] Tạo Mapper trong `mappers/`
- [ ] Tạo Service trong `services/{scope}/`
- [ ] Tạo Controller trong `controllers/{scope}/`
- [ ] Test API
