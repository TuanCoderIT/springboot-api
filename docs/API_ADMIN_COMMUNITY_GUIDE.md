# API Admin Community - Hướng dẫn cho Frontend

## Yêu cầu chung

- **Base URL:** `/admin/community`
- **Authentication:** Bearer Token (JWT)
- **Role yêu cầu:** `ADMIN`

---

## 1. Tạo Community Notebook

### Endpoint

```
POST /admin/community
```

### Content-Type

```
multipart/form-data
```

### Request Parameters

| Tên           | Kiểu   | Bắt buộc | Mô tả                                            |
| ------------- | ------ | -------- | ------------------------------------------------ |
| `title`       | string | ✅       | Tiêu đề notebook (max 255 ký tự)                 |
| `description` | string | ✅       | Mô tả notebook                                   |
| `visibility`  | string | ✅       | Độ hiển thị: `public`, `private`, `members_only` |
| `thumbnail`   | File   | ❌       | Ảnh thumbnail (image/\*)                         |

### Response (200 OK)

```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "type": "community",
  "visibility": "public | private | members_only",
  "thumbnailUrl": "string | null",
  "memberCount": 1,
  "createdAt": "2025-12-11T10:00:00+07:00",
  "updatedAt": "2025-12-11T10:00:00+07:00"
}
```

### Lỗi có thể xảy ra

| Status | Mô tả                                                      |
| ------ | ---------------------------------------------------------- |
| 401    | Chưa đăng nhập hoặc token hết hạn                          |
| 403    | Không có quyền ADMIN                                       |
| 400    | Dữ liệu không hợp lệ (title rỗng, visibility không hợp lệ) |
| 500    | Lỗi server (upload thumbnail thất bại)                     |

### Ví dụ Frontend (TypeScript)

```typescript
const formData = new FormData();
formData.append("title", "Tên Community");
formData.append("description", "Mô tả chi tiết");
formData.append("visibility", "public");
if (thumbnailFile) {
  formData.append("thumbnail", thumbnailFile);
}

const response = await fetch("/admin/community", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    // Không set Content-Type, browser tự xử lý cho multipart
  },
  body: formData,
});
```

---

## 2. Cập nhật Community Notebook

### Endpoint

```
PUT /admin/community/{id}
```

### Content-Type

```
multipart/form-data
```

### Path Parameters

| Tên  | Kiểu | Mô tả                        |
| ---- | ---- | ---------------------------- |
| `id` | UUID | ID của notebook cần cập nhật |

### Request Parameters

| Tên           | Kiểu   | Bắt buộc | Mô tả                                                |
| ------------- | ------ | -------- | ---------------------------------------------------- |
| `title`       | string | ✅       | Tiêu đề mới                                          |
| `description` | string | ✅       | Mô tả mới                                            |
| `visibility`  | string | ✅       | Độ hiển thị mới                                      |
| `thumbnail`   | File   | ❌       | Ảnh thumbnail mới (nếu không gửi, giữ nguyên ảnh cũ) |

### Response (200 OK)

```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "type": "community",
  "visibility": "public | private | members_only",
  "thumbnailUrl": "string | null",
  "memberCount": 10,
  "createdAt": "2025-12-11T10:00:00+07:00",
  "updatedAt": "2025-12-11T12:00:00+07:00"
}
```

### Lỗi có thể xảy ra

| Status | Mô tả                                  |
| ------ | -------------------------------------- |
| 401    | Chưa đăng nhập hoặc token hết hạn      |
| 403    | Không có quyền ADMIN                   |
| 404    | Không tìm thấy notebook với ID đã cho  |
| 400    | Dữ liệu không hợp lệ                   |
| 500    | Lỗi server (upload thumbnail thất bại) |

### Ví dụ Frontend (TypeScript)

```typescript
const formData = new FormData();
formData.append("title", "Tiêu đề mới");
formData.append("description", "Mô tả mới");
formData.append("visibility", "members_only");
if (newThumbnailFile) {
  formData.append("thumbnail", newThumbnailFile);
}

const response = await fetch(`/admin/community/${notebookId}`, {
  method: "PUT",
  headers: {
    Authorization: `Bearer ${token}`,
  },
  body: formData,
});
```

---

## 3. Xóa Community Notebook

### Endpoint

```
DELETE /admin/community/{id}
```

### Path Parameters

| Tên  | Kiểu | Mô tả                   |
| ---- | ---- | ----------------------- |
| `id` | UUID | ID của notebook cần xóa |

### Response

- **204 No Content:** Xóa thành công

### Lỗi có thể xảy ra

| Status | Mô tả                   |
| ------ | ----------------------- |
| 401    | Chưa đăng nhập          |
| 403    | Không có quyền ADMIN    |
| 404    | Không tìm thấy notebook |

---

## 4. Lấy thông tin 1 Community

### Endpoint

```
GET /admin/community/{id}
```

### Response (200 OK)

```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "type": "community",
  "visibility": "public",
  "thumbnailUrl": "string | null",
  "memberCount": 25,
  "createdAt": "2025-12-11T10:00:00+07:00",
  "updatedAt": "2025-12-11T12:00:00+07:00"
}
```

---

## 5. Danh sách Community (có phân trang)

### Endpoint

```
GET /admin/community
```

### Query Parameters

| Tên          | Kiểu   | Mặc định    | Mô tả                                                           |
| ------------ | ------ | ----------- | --------------------------------------------------------------- |
| `q`          | string | -           | Tìm kiếm theo title/description                                 |
| `visibility` | string | -           | Lọc theo visibility                                             |
| `sortBy`     | string | `createdAt` | Field để sort: `createdAt`, `updatedAt`, `title`, `memberCount` |
| `sortDir`    | string | `desc`      | Hướng sort: `asc`, `desc`                                       |
| `page`       | int    | 0           | Số trang (bắt đầu từ 0)                                         |
| `size`       | int    | 10          | Số item mỗi trang                                               |

### Response (200 OK)

```json
{
  "content": [
    {
      "id": "uuid",
      "title": "string",
      "description": "string",
      "type": "community",
      "visibility": "public",
      "thumbnailUrl": "string | null",
      "memberCount": 10,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 50,
    "totalPages": 5
  }
}
```

---

## Lưu ý quan trọng

1. **Visibility values:** Chỉ chấp nhận `public`, `private`, `members_only`

2. **Thumbnail:**

   - Nếu không gửi thumbnail khi tạo mới → notebook không có ảnh
   - Nếu không gửi thumbnail khi update → giữ nguyên ảnh cũ
   - Để xóa thumbnail, backend cần API riêng (hiện chưa có)

3. **multipart/form-data:** Không set `Content-Type` header thủ công khi dùng FormData, browser sẽ tự thêm boundary

4. **memberCount:** Chỉ đếm các member có status `approved`
