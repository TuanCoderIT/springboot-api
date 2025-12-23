# Chapter Items API

Quản lý nội dung (items) trong chương học.

**Base URL:** `/lecturer/chapters/{chapterId}/items`

---

## 1. Upload Files

Upload file tài liệu vào chương.

```
POST /lecturer/chapters/{chapterId}/items/files
Content-Type: multipart/form-data
```

### Request

| Field                      | Type   | Required | Mô tả                                       |
| -------------------------- | ------ | -------- | ------------------------------------------- |
| `files`                    | File[] | ✅       | Danh sách files (PDF, DOC, DOCX, PPT, PPTX) |
| `fileInfos[i].title`       | string | ❌       | Tiêu đề hiển thị (mặc định = tên file)      |
| `fileInfos[i].description` | string | ❌       | Mô tả file                                  |

### Response `201 Created`

```json
[
  {
    "id": "8d567faa-5d76-40c0-a33d-700d15d559c8",
    "itemType": "FILE",
    "refId": "dc52f46b-ed79-4b93-9a28-ca63123db872",
    "title": "Bài 1: Giới thiệu",
    "sortOrder": 0,
    "metadata": {
      "mimeType": "application/pdf",
      "fileSize": 633802,
      "storageUrl": "/uploads/abc.pdf",
      "originalFilename": "bai1.pdf"
    },
    "createdAt": "2025-12-22T21:17:58+07:00"
  }
]
```

---

## 2. Move Item

Di chuyển item sang chapter khác hoặc đổi vị trí trong chapter.

```
PATCH /lecturer/chapters/{chapterId}/items/{itemId}/move
Content-Type: application/json
```

### Request Body

```json
{
  "targetChapterId": "3f4e3c6d-8ac1-449e-9344-7058cc6d4fa1",
  "targetIndex": 0
}
```

| Field             | Type   | Required | Mô tả                                     |
| ----------------- | ------ | -------- | ----------------------------------------- |
| `targetChapterId` | UUID   | ✅       | ID chapter đích                           |
| `targetIndex`     | number | ❌       | Vị trí (0-based). `null` = cuối danh sách |

### Response `200 OK`

```json
{
  "id": "8d567faa-5d76-40c0-a33d-700d15d559c8",
  "itemType": "FILE",
  "refId": "dc52f46b-ed79-4b93-9a28-ca63123db872",
  "title": "Bài 1: Giới thiệu",
  "sortOrder": 0,
  "metadata": {...},
  "createdAt": "2025-12-22T21:17:58+07:00"
}
```

### Use Cases

| Hành động                   | targetChapterId      | targetIndex      |
| --------------------------- | -------------------- | ---------------- |
| Đổi vị trí trong chapter    | = chapterId hiện tại | vị trí mới       |
| Di chuyển sang chapter khác | = chapter mới        | vị trí hoặc null |

---

## 3. Reorder Items

Sắp xếp lại thứ tự tất cả items trong chapter (dùng cho drag & drop).

```
PATCH /lecturer/chapters/{chapterId}/items/reorder
Content-Type: application/json
```

### Request Body

```json
{
  "orderedIds": ["item-uuid-3", "item-uuid-1", "item-uuid-2"]
}
```

| Field        | Type   | Required | Mô tả                              |
| ------------ | ------ | -------- | ---------------------------------- |
| `orderedIds` | UUID[] | ✅       | Danh sách item IDs theo thứ tự mới |

### Response `200 OK`

```json
[
  { "id": "item-uuid-3", "sortOrder": 0, ... },
  { "id": "item-uuid-1", "sortOrder": 1, ... },
  { "id": "item-uuid-2", "sortOrder": 2, ... }
]
```

---

## 4. Delete Item

Xóa item khỏi chapter. Nếu item là FILE, file và chunks cũng bị xóa.

```
DELETE /lecturer/chapters/{chapterId}/items/{itemId}
```

### Response `204 No Content`

(Không có body)

---

## TypeScript Interfaces

```typescript
interface ChapterItemResponse {
  id: string;
  itemType: "FILE" | "QUIZ" | "LINK" | "TEXT";
  refId: string | null;
  title: string;
  sortOrder: number;
  metadata: Record<string, any>;
  createdAt: string;
}

interface MoveItemRequest {
  targetChapterId: string;
  targetIndex?: number | null;
}

interface ReorderItemsRequest {
  orderedIds: string[];
}
```

---

## Error Responses

| Status | Mô tả                           |
| ------ | ------------------------------- |
| `400`  | Request không hợp lệ            |
| `403`  | Không có quyền truy cập chapter |
| `404`  | Chapter hoặc Item không tồn tại |
