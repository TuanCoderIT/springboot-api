# API Notebook Members

---

## 1. Lấy quyền của bản thân trong Notebook

### Endpoint

```
GET /user/notebooks/{notebookId}/me
```

### Mô tả

API này trả về thông tin quyền của user hiện tại trong notebook, bao gồm role, status và các permission flags.

### Authentication

```
Authorization: Bearer <accessToken>
```

### Path Parameters

| Tham số      | Kiểu   | Bắt buộc | Mô tả           |
| ------------ | ------ | -------- | --------------- |
| `notebookId` | `UUID` | ✅       | ID của notebook |

### Response (200)

```typescript
interface MyMembershipResponse {
  memberId: string; // Member ID (UUID)
  role: string; // "owner" | "admin" | "member"
  status: string; // "approved" | "pending" | "rejected"
  joinedAt: string | null; // ISO 8601 datetime
  canManageMembers: boolean; // Có thể quản lý thành viên không
  canUploadFiles: boolean; // Có thể upload files không
  canDeleteNotebook: boolean; // Có thể xóa notebook không
  canEditNotebook: boolean; // Có thể chỉnh sửa notebook không
}
```

### Ví dụ Response

```json
{
  "memberId": "a1b2c3d4-1234-5678-abcd-ef1234567890",
  "role": "owner",
  "status": "approved",
  "joinedAt": "2025-12-01T10:00:00+07:00",
  "canManageMembers": true,
  "canUploadFiles": true,
  "canDeleteNotebook": true,
  "canEditNotebook": true
}
```

### Lỗi có thể xảy ra

| HTTP Code          | Nguyên nhân            | Response                                                                   |
| ------------------ | ---------------------- | -------------------------------------------------------------------------- |
| `401 Unauthorized` | Không có token         | `{"status": 401, "message": "Unauthorized"}`                               |
| `404 Not Found`    | Notebook không tồn tại | `{"status": 404, "message": "Notebook không tồn tại"}`                     |
| `404 Not Found`    | User không phải member | `{"status": 404, "message": "Bạn không phải thành viên của notebook này"}` |

### Code mẫu TypeScript

```typescript
interface MyMembershipResponse {
  memberId: string;
  role: "owner" | "admin" | "member";
  status: "approved" | "pending" | "rejected";
  joinedAt: string | null;
  canManageMembers: boolean;
  canUploadFiles: boolean;
  canDeleteNotebook: boolean;
  canEditNotebook: boolean;
}

async function getMyMembership(
  notebookId: string,
  token: string
): Promise<MyMembershipResponse> {
  const response = await fetch(
    `${API_BASE_URL}/user/notebooks/${notebookId}/me`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  return response.json();
}

// Sử dụng
const membership = await getMyMembership(notebookId, token);

if (membership.canManageMembers) {
  // Hiển thị nút quản lý thành viên
}

if (membership.canUploadFiles) {
  // Hiển thị nút upload file
}
```

---

## 2. Lấy danh sách thành viên Notebook

### Endpoint

```
GET /user/notebooks/{notebookId}/members
```

### Mô tả

API này trả về danh sách thành viên đã được duyệt (`approved`) trong một notebook. Sử dụng **cursor-based pagination** để phân trang hiệu quả.

---

## Authentication

```
Authorization: Bearer <accessToken>
```

---

## Path Parameters

| Tham số      | Kiểu   | Bắt buộc | Mô tả           |
| ------------ | ------ | -------- | --------------- |
| `notebookId` | `UUID` | ✅       | ID của notebook |

---

## Query Parameters

| Tham số  | Kiểu     | Bắt buộc | Mặc định | Mô tả                                  |
| -------- | -------- | -------- | -------- | -------------------------------------- |
| `q`      | `string` | ❌       | `null`   | Tìm kiếm theo tên hoặc email           |
| `cursor` | `string` | ❌       | `null`   | Cursor phân trang (ISO 8601 datetime)  |
| `limit`  | `number` | ❌       | `20`     | Số items mỗi trang (khuyến nghị 10-50) |

---

## Response

### Success (200)

```typescript
interface NotebookMembersResponse {
  items: NotebookMemberItem[];
  cursorNext: string | null; // Dùng cho request tiếp theo
  hasMore: boolean; // Còn dữ liệu không
  total: number; // Tổng số members thỏa điều kiện
}

interface NotebookMemberItem {
  id: string; // Member ID (UUID)
  userId: string; // User ID (UUID)
  fullName: string; // Tên đầy đủ
  email: string; // Email
  avatarUrl: string | null; // URL avatar
  role: string; // "owner" | "admin" | "member"
  status: string; // Luôn là "approved"
  joinedAt: string; // ISO 8601 datetime
}
```

### Ví dụ Response

```json
{
  "items": [
    {
      "id": "a1b2c3d4-1234-5678-abcd-ef1234567890",
      "userId": "user-uuid-here",
      "fullName": "Nguyễn Văn A",
      "email": "nguyenvana@example.com",
      "avatarUrl": "https://storage.example.com/avatar.jpg",
      "role": "owner",
      "status": "approved",
      "joinedAt": "2025-12-01T10:00:00+07:00"
    },
    {
      "id": "b2c3d4e5-2345-6789-bcde-f12345678901",
      "userId": "user-uuid-here-2",
      "fullName": "Trần Thị B",
      "email": "tranthib@example.com",
      "avatarUrl": null,
      "role": "member",
      "status": "approved",
      "joinedAt": "2025-12-05T14:30:00+07:00"
    }
  ],
  "cursorNext": "2025-12-05T14:30:00+07:00",
  "hasMore": true,
  "total": 25
}
```

---

## Lỗi có thể xảy ra

| HTTP Code          | Nguyên nhân                        | Response                                                                 |
| ------------------ | ---------------------------------- | ------------------------------------------------------------------------ |
| `401 Unauthorized` | Không có token hoặc token hết hạn  | `{"status": 401, "message": "Unauthorized"}`                             |
| `403 Forbidden`    | Không phải thành viên của notebook | `{"status": 403, "message": "Bạn không có quyền truy cập notebook này"}` |
| `403 Forbidden`    | Chưa được duyệt vào notebook       | `{"status": 403, "message": "Bạn chưa được duyệt vào notebook này"}`     |
| `404 Not Found`    | Notebook không tồn tại             | `{"status": 404, "message": "Notebook không tồn tại"}`                   |
| `400 Bad Request`  | Cursor không hợp lệ                | `{"status": 400, "message": "Cursor không hợp lệ"}`                      |

---

## Cách sử dụng Cursor Pagination

### Lần đầu tiên (không có cursor)

```typescript
const response = await fetch("/user/notebooks/{notebookId}/members?limit=20", {
  headers: {
    Authorization: `Bearer ${token}`,
  },
});
const data = await response.json();
// data.cursorNext = "2025-12-05T14:30:00+07:00"
// data.hasMore = true
```

### Các lần tiếp theo (có cursor)

```typescript
const response = await fetch(
  `/user/notebooks/{notebookId}/members?limit=20&cursor=${data.cursorNext}`,
  {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }
);
```

### Kết hợp tìm kiếm

```typescript
const response = await fetch(
  `/user/notebooks/{notebookId}/members?q=nguyen&limit=20`,
  {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }
);
```

---

## Code mẫu TypeScript

```typescript
// Types
interface NotebookMemberItem {
  id: string;
  userId: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: string;
  status: string;
  joinedAt: string;
}

interface NotebookMembersResponse {
  items: NotebookMemberItem[];
  cursorNext: string | null;
  hasMore: boolean;
  total: number;
}

interface GetMembersParams {
  notebookId: string;
  q?: string;
  cursor?: string;
  limit?: number;
}

// API Function
async function getNotebookMembers(
  params: GetMembersParams,
  token: string
): Promise<NotebookMembersResponse> {
  const { notebookId, q, cursor, limit = 20 } = params;

  const searchParams = new URLSearchParams();
  if (q) searchParams.set("q", q);
  if (cursor) searchParams.set("cursor", cursor);
  searchParams.set("limit", String(limit));

  const url = `${API_BASE_URL}/user/notebooks/${notebookId}/members?${searchParams}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  return response.json();
}

// React Hook Example
function useMembersList(notebookId: string, searchQuery?: string) {
  const [members, setMembers] = useState<NotebookMemberItem[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);

  const loadMore = async () => {
    if (loading || !hasMore) return;

    setLoading(true);
    try {
      const data = await getNotebookMembers(
        {
          notebookId,
          q: searchQuery,
          cursor: cursor || undefined,
          limit: 20,
        },
        token
      );

      setMembers((prev) => (cursor ? [...prev, ...data.items] : data.items));
      setCursor(data.cursorNext);
      setHasMore(data.hasMore);
      setTotal(data.total);
    } catch (error) {
      console.error("Failed to load members:", error);
    } finally {
      setLoading(false);
    }
  };

  // Reset khi search query thay đổi
  useEffect(() => {
    setMembers([]);
    setCursor(null);
    setHasMore(true);
  }, [searchQuery]);

  return { members, loadMore, hasMore, loading, total };
}
```

---

## Lưu ý

1. **Cursor** là giá trị `joinedAt` của member cuối cùng (ISO 8601 format). **Không được tự tạo cursor**, chỉ sử dụng giá trị `cursorNext` từ response.

2. **Khi `hasMore = false`**, không cần gọi API tiếp vì đã hết dữ liệu.

3. **Khi thay đổi search query (`q`)**, reset cursor về `null` để bắt đầu từ đầu.

4. **API chỉ trả về members có `status = approved`**. Pending/rejected members không được hiển thị.

5. **Sắp xếp**: Members được sắp xếp theo `joinedAt` giảm dần (mới tham gia nhất hiển thị trước).
