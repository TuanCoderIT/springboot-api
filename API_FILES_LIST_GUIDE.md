# API Hướng dẫn: Danh sách Files trong Notebook

## Mục lục

1. [Lấy danh sách Files](#1-lấy-danh-sách-files)
2. [Lấy danh sách Contributors](#2-lấy-danh-sách-contributors)

---

## 1. Lấy danh sách Files

### Endpoint

```
GET /admin/notebooks/{notebookId}/files
```

## Mô tả

Lấy danh sách files trong một notebook với đầy đủ tính năng: lọc (filter), tìm kiếm (search), phân trang (pagination) và sắp xếp (sort).

## Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

## Path Parameters

| Parameter    | Type | Required | Description     |
| ------------ | ---- | -------- | --------------- |
| `notebookId` | UUID | Yes      | ID của notebook |

## Query Parameters

Tất cả query parameters đều **optional** (không bắt buộc).

| Parameter       | Type    | Default     | Description                                |
| --------------- | ------- | ----------- | ------------------------------------------ |
| `status`        | String  | -           | Lọc theo trạng thái file                   |
| `mimeType`      | String  | -           | Lọc theo loại file                         |
| `ocrDone`       | Boolean | -           | Lọc theo trạng thái OCR (true/false)       |
| `embeddingDone` | Boolean | -           | Lọc theo trạng thái Embedding (true/false) |
| `uploadedBy`    | UUID    | -           | Lọc theo ID người đóng góp                 |
| `search`        | String  | -           | Tìm kiếm theo tên file (case-insensitive)  |
| `sortBy`        | String  | `createdAt` | Sắp xếp theo field                         |
| `page`          | Integer | `0`         | Số trang (bắt đầu từ 0)                    |
| `size`          | Integer | `20`        | Số lượng items mỗi trang (1-100)           |

### Giá trị hợp lệ cho `status`:

- `pending` - Chờ duyệt
- `approved` - Đã duyệt
- `rejected` - Đã từ chối
- `processing` - Đang xử lý
- `failed` - Lỗi
- `done` - Hoàn thành

### Giá trị hợp lệ cho `mimeType`:

- `application/pdf` - File PDF
- `application/msword` - File Word (.doc)
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` - File Word (.docx)

### Giá trị hợp lệ cho `sortBy`:

- `createdAt` - Sắp xếp theo ngày tạo (mới nhất trước) - **Mặc định**
- `createdAtAsc` - Sắp xếp theo ngày tạo (cũ nhất trước)
- `updatedAt` - Sắp xếp theo ngày cập nhật (mới nhất trước)
- `updatedAtAsc` - Sắp xếp theo ngày cập nhật (cũ nhất trước)
- `originalFilename` hoặc `filename` - Sắp xếp theo tên file (A-Z)
- `originalFilenameDesc` hoặc `filenameDesc` - Sắp xếp theo tên file (Z-A)

## Response Format

### Success Response (200 OK)

```json
{
  "content": [
    {
      "id": "uuid",
      "originalFilename": "document.pdf",
      "mimeType": "application/pdf",
      "fileSize": 1024000,
      "storageUrl": "/uploads/filename.pdf",
      "status": "approved",
      "pagesCount": 10,
      "ocrDone": true,
      "embeddingDone": true,
      "chunkSize": 800,
      "chunkOverlap": 120,
      "chunksCount": 15,
      "uploadedBy": {
        "id": "user-uuid",
        "fullName": "Nguyễn Văn A",
        "email": "nguyenvana@example.com",
        "avatarUrl": "/uploads/avatar.jpg"
      },
      "createdAt": "2025-12-01T10:00:00+07:00",
      "updatedAt": "2025-12-01T11:00:00+07:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

### Response Fields

#### PageResponse

| Field           | Type                          | Description                      |
| --------------- | ----------------------------- | -------------------------------- |
| `content`       | Array\<NotebookFileResponse\> | Danh sách files                  |
| `page`          | Integer                       | Trang hiện tại (0-indexed)       |
| `size`          | Integer                       | Số lượng items mỗi trang         |
| `totalElements` | Long                          | Tổng số files thỏa mãn điều kiện |
| `totalPages`    | Integer                       | Tổng số trang                    |

#### NotebookFileResponse

| Field              | Type           | Description                                |
| ------------------ | -------------- | ------------------------------------------ |
| `id`               | UUID           | ID của file                                |
| `originalFilename` | String         | Tên file gốc                               |
| `mimeType`         | String         | Loại MIME của file                         |
| `fileSize`         | Long           | Kích thước file (bytes)                    |
| `storageUrl`       | String         | Đường dẫn lưu trữ file                     |
| `status`           | String         | Trạng thái file                            |
| `pagesCount`       | Integer        | Số trang (cho PDF)                         |
| `ocrDone`          | Boolean        | Đã hoàn thành OCR chưa                     |
| `embeddingDone`    | Boolean        | Đã tạo embedding chưa                      |
| `chunkSize`        | Integer        | Kích thước chunk (khi chia text)           |
| `chunkOverlap`     | Integer        | Độ overlap giữa các chunk                  |
| `chunksCount`      | Long           | **Tổng số chunks** đã được tạo từ file này |
| `uploadedBy`       | UploaderInfo   | **Thông tin người đóng góp file**          |
| `createdAt`        | OffsetDateTime | Thời gian upload (ISO 8601)                |
| `updatedAt`        | OffsetDateTime | Thời gian cập nhật (ISO 8601)              |

#### UploaderInfo (Nested Object)

| Field       | Type   | Description                    |
| ----------- | ------ | ------------------------------ |
| `id`        | UUID   | ID của người đóng góp          |
| `fullName`  | String | Tên đầy đủ                     |
| `email`     | String | Email                          |
| `avatarUrl` | String | URL ảnh đại diện (có thể null) |

### Error Response (400 Bad Request)

```json
{
  "status": 400,
  "message": "Page phải >= 0",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

### Error Response (401 Unauthorized)

```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2025-12-01T10:00:00+07:00"
}
```

## Ví dụ sử dụng

### 1. Lấy danh sách tất cả files (trang đầu tiên)

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files
```

Hoặc với query params mặc định:

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?page=0&size=20&sortBy=createdAt
```

### 2. Lọc theo status

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?status=approved
```

### 3. Lọc nhiều điều kiện

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?status=approved&mimeType=application/pdf&ocrDone=true&embeddingDone=true
```

### 4. Lọc theo người đóng góp

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?uploadedBy=user-uuid-here
```

### 5. Tìm kiếm theo tên file

Search sẽ tìm kiếm **không phân biệt hoa thường** trong tên file.

### 6. Sắp xếp theo tên file

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?sortBy=originalFilename
```

### 7. Phân trang

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?page=1&size=10
```

### 8. Kết hợp tất cả

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?status=approved&search=document&sortBy=updatedAt&page=0&size=10
```

## Ví dụ code (JavaScript/TypeScript)

### Fetch API

```javascript
async function getFiles(notebookId, filters = {}) {
  const params = new URLSearchParams({
    page: filters.page || 0,
    size: filters.size || 20,
    sortBy: filters.sortBy || "createdAt",
  });

  if (filters.status) params.append("status", filters.status);
  if (filters.mimeType) params.append("mimeType", filters.mimeType);
  if (filters.ocrDone !== undefined) params.append("ocrDone", filters.ocrDone);
  if (filters.embeddingDone !== undefined)
    params.append("embeddingDone", filters.embeddingDone);
  if (filters.uploadedBy) params.append("uploadedBy", filters.uploadedBy);
  if (filters.search) params.append("search", filters.search);

  const response = await fetch(
    `/admin/notebooks/${notebookId}/files?${params}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Sử dụng
const result = await getFiles("c3a7f558-faa7-4218-ae41-4ef57f976f34", {
  status: "approved",
  search: "report",
  sortBy: "createdAt",
  page: 0,
  size: 20,
});

console.log(`Tổng số files: ${result.totalElements}`);
console.log(`Trang ${result.page + 1}/${result.totalPages}`);
result.content.forEach((file) => {
  console.log(`${file.originalFilename} - ${file.chunksCount} chunks`);
  console.log(
    `Đóng góp bởi: ${file.uploadedBy.fullName} (${file.uploadedBy.email})`
  );
});
```

### Axios

```javascript
import axios from "axios";

async function getFiles(notebookId, filters = {}) {
  const response = await axios.get(`/admin/notebooks/${notebookId}/files`, {
    params: {
      status: filters.status,
      mimeType: filters.mimeType,
      ocrDone: filters.ocrDone,
      embeddingDone: filters.embeddingDone,
      search: filters.search,
      sortBy: filters.sortBy || "createdAt",
      page: filters.page || 0,
      size: filters.size || 20,
    },
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return response.data;
}
```

## Lưu ý quan trọng

1. **Pagination**:

   - `page` bắt đầu từ `0`
   - `size` phải từ `1` đến `100`
   - Nếu `page` >= `totalPages`, sẽ trả về mảng rỗng

2. **Search**:

   - Tìm kiếm **không phân biệt hoa thường**
   - Chỉ tìm trong `originalFilename`
   - Dùng pattern matching (LIKE), không phải exact match

3. **Filters**:

   - Tất cả filters có thể kết hợp với nhau
   - Nếu không truyền filter nào, sẽ trả về tất cả files
   - `null` hoặc `undefined` sẽ bị bỏ qua

4. **Sort**:

   - Mặc định: `createdAt` (mới nhất trước)
   - Có thể sắp xếp theo: `createdAt`, `updatedAt`, `originalFilename`

5. **chunksCount**:
   - Field này cho biết số lượng chunks đã được tạo từ file
   - Chunks là các đoạn text đã được chia nhỏ để tạo embedding
   - Giá trị này có thể là `0` nếu file chưa được xử lý hoặc xử lý thất bại

## Testing với cURL

```bash
curl -X 'GET' \
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files?status=approved&page=0&size=20&sortBy=createdAt' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE'
```

---

## 2. Lấy danh sách Contributors

### Endpoint

```
GET /admin/notebooks/{notebookId}/files/contributors
```

### Mô tả

Lấy danh sách tất cả người đóng góp (contributors) trong một notebook, bao gồm thông tin người dùng và số lượng file họ đã đóng góp. API này dùng để tạo dropdown/autocomplete cho filter theo người đóng góp.

### Authentication

Yêu cầu Bearer Token trong header:

```
Authorization: Bearer <token>
```

### Path Parameters

| Parameter    | Type | Required | Description     |
| ------------ | ---- | -------- | --------------- |
| `notebookId` | UUID | Yes      | ID của notebook |

### Query Parameters

| Parameter | Type   | Default | Description                             |
| --------- | ------ | ------- | --------------------------------------- |
| `search`  | String | -       | Tìm kiếm theo tên hoặc email (optional) |

### Response Format

#### Success Response (200 OK)

```json
[
  {
    "id": "user-uuid",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "avatarUrl": "/uploads/avatar.jpg",
    "filesCount": 15
  },
  {
    "id": "user-uuid-2",
    "fullName": "Trần Thị B",
    "email": "tranthib@example.com",
    "avatarUrl": null,
    "filesCount": 8
  }
]
```

#### ContributorInfo Fields

| Field        | Type   | Description                                        |
| ------------ | ------ | -------------------------------------------------- |
| `id`         | UUID   | ID của người đóng góp                              |
| `fullName`   | String | Tên đầy đủ                                         |
| `email`      | String | Email                                              |
| `avatarUrl`  | String | URL ảnh đại diện (có thể null)                     |
| `filesCount` | Long   | **Tổng số files đã đóng góp** (tính cả mọi status) |

### Lưu ý

- **Limit**: API luôn trả về tối đa **10 kết quả** (được sắp xếp theo số lượng file giảm dần)
- **filesCount**: Đếm **tất cả files** mà người đó đã upload, không phân biệt status (pending, approved, rejected, processing, failed, done)
- **Search**: Tìm kiếm **không phân biệt hoa thường** trong cả `fullName` và `email`
- **Sorting**: Sắp xếp theo số lượng file giảm dần, nếu bằng nhau thì sắp xếp theo tên A-Z

### Ví dụ sử dụng

#### 1. Lấy tất cả contributors (top 10)

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/contributors
```

#### 2. Tìm kiếm contributor theo tên/email

```bash
GET /admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/contributors?search=nguyen
```

### Ví dụ code (JavaScript/TypeScript)

```javascript
async function getContributors(notebookId, search = null) {
  const params = new URLSearchParams();
  if (search) params.append("search", search);

  const response = await fetch(
    `/admin/notebooks/${notebookId}/files/contributors?${params}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Sử dụng để tạo dropdown filter
const contributors = await getContributors(
  "c3a7f558-faa7-4218-ae41-4ef57f976f34"
);
contributors.forEach((contributor) => {
  console.log(`${contributor.fullName}: ${contributor.filesCount} files`);
});
```

### Testing với cURL

```bash
curl -X 'GET' \
  'http://localhost:8386/admin/notebooks/c3a7f558-faa7-4218-ae41-4ef57f976f34/files/contributors?search=nguyen' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE'
```
