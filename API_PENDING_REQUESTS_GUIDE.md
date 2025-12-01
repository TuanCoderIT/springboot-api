# Hướng Dẫn API Lấy Danh Sách Yêu Cầu Tham Gia Notebook

## Endpoint

```
GET /admin/community/pending-requests
```

## Mô tả

Lấy danh sách các yêu cầu tham gia notebook với phân trang, lọc, tìm kiếm và sắp xếp. Mặc định sẽ trả về các yêu cầu có status = "pending", nhưng có thể lọc theo các status khác (approved, rejected, blocked).

## Authentication

Yêu cầu JWT token trong cookie `AUTH-TOKEN`. Endpoint này chỉ dành cho admin.

## Query Parameters

| Parameter    | Type    | Required | Default     | Mô tả                                                                            |
| ------------ | ------- | -------- | ----------- | -------------------------------------------------------------------------------- |
| `notebookId` | UUID    | No       | null        | Lọc theo notebook cụ thể                                                         |
| `status`     | String  | No       | "pending"   | Lọc theo status (pending, approved, rejected, blocked)                           |
| `q`          | String  | No       | null        | Tìm kiếm theo notebook title, user fullName, user email                          |
| `sortBy`     | String  | No       | "createdAt" | Sắp xếp theo field (createdAt, joinedAt, updatedAt, userFullName, notebookTitle) |
| `sortDir`    | String  | No       | "desc"      | Hướng sắp xếp (asc, desc)                                                        |
| `page`       | Integer | No       | 0           | Số trang (0-based)                                                               |
| `size`       | Integer | No       | 10          | Số lượng items mỗi trang                                                         |

## Response Format

### Success Response (200 OK)

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "notebookId": "660e8400-e29b-41d4-a716-446655440001",
      "notebookTitle": "Nhóm Toán Học",
      "userId": "770e8400-e29b-41d4-a716-446655440002",
      "userFullName": "Nguyễn Văn A",
      "userEmail": "nguyenvana@example.com",
      "role": "member",
      "status": "pending",
      "joinedAt": null,
      "createdAt": "2024-01-20T10:00:00Z",
      "updatedAt": "2024-01-20T10:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "total": 25,
    "totalPages": 3
  }
}
```

## Response Fields

### PendingRequestResponse

| Field           | Type                   | Mô tả                                             |
| --------------- | ---------------------- | ------------------------------------------------- |
| `id`            | UUID                   | ID của member record                              |
| `notebookId`    | UUID                   | ID của notebook                                   |
| `notebookTitle` | String                 | Tên notebook                                      |
| `userId`        | UUID                   | ID của user                                       |
| `userFullName`  | String                 | Họ tên đầy đủ của user                            |
| `userEmail`     | String                 | Email của user                                    |
| `role`          | String                 | Role của member (owner, admin, member)            |
| `status`        | String                 | Trạng thái (pending, approved, rejected, blocked) |
| `joinedAt`      | OffsetDateTime \| null | Thời gian tham gia (null nếu chưa approved)       |
| `createdAt`     | OffsetDateTime         | Thời gian tạo yêu cầu                             |
| `updatedAt`     | OffsetDateTime         | Thời gian cập nhật cuối                           |

### PagedResponse Meta

| Field        | Type    | Mô tả                                                                                             |
| ------------ | ------- | ------------------------------------------------------------------------------------------------- |
| `page`       | Integer | Số trang hiện tại (0-based)                                                                       |
| `size`       | Integer | Số lượng items mỗi trang                                                                          |
| `total`      | Long    | **Tổng số items** theo filter hiện tại (notebookId, status, q). Không bị ảnh hưởng bởi pagination |
| `totalPages` | Integer | Tổng số trang                                                                                     |

## Error Responses

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2024-01-20T10:00:00Z"
}
```

### 500 Internal Server Error

```json
{
  "status": 500,
  "message": "Internal server error",
  "timestamp": "2024-01-20T10:00:00Z"
}
```

## Ví dụ Request

### 1. Lấy tất cả yêu cầu pending (mặc định)

```bash
GET /admin/community/pending-requests
```

### 2. Lọc theo notebook cụ thể

```bash
GET /admin/community/pending-requests?notebookId=660e8400-e29b-41d4-a716-446655440001
```

### 3. Lọc theo status

```bash
GET /admin/community/pending-requests?status=approved
```

### 4. Tìm kiếm

```bash
GET /admin/community/pending-requests?q=nguyenvana
```

### 5. Kết hợp nhiều filters

```bash
GET /admin/community/pending-requests?notebookId=660e8400-e29b-41d4-a716-446655440001&status=pending&q=nguyen&sortBy=createdAt&sortDir=desc&page=0&size=20
```

### 6. Sắp xếp theo tên user

```bash
GET /admin/community/pending-requests?sortBy=userFullName&sortDir=asc
```

## TypeScript Interfaces

```typescript
interface PendingRequestResponse {
  id: string;
  notebookId: string;
  notebookTitle: string;
  userId: string;
  userFullName: string;
  userEmail: string;
  role: "owner" | "admin" | "member";
  status: "pending" | "approved" | "rejected" | "blocked";
  joinedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

interface PagedMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface PagedResponse<T> {
  items: T[];
  meta: PagedMeta;
}

type PendingRequestsResponse = PagedResponse<PendingRequestResponse>;
```

## Ví dụ Code

### React/TypeScript với Axios

```typescript
import axios, { AxiosError } from "axios";

const API_BASE_URL = "http://localhost:8386/admin/community";

export interface GetPendingRequestsParams {
  notebookId?: string;
  status?: "pending" | "approved" | "rejected" | "blocked";
  q?: string;
  sortBy?: string;
  sortDir?: "asc" | "desc";
  page?: number;
  size?: number;
}

export const getPendingRequests = async (
  params: GetPendingRequestsParams = {}
): Promise<PendingRequestsResponse> => {
  try {
    const queryParams = new URLSearchParams();

    if (params.notebookId) queryParams.append("notebookId", params.notebookId);
    if (params.status) queryParams.append("status", params.status);
    if (params.q) queryParams.append("q", params.q);
    if (params.sortBy) queryParams.append("sortBy", params.sortBy);
    if (params.sortDir) queryParams.append("sortDir", params.sortDir);
    if (params.page !== undefined)
      queryParams.append("page", params.page.toString());
    if (params.size !== undefined)
      queryParams.append("size", params.size.toString());

    const url = `${API_BASE_URL}/pending-requests${
      queryParams.toString() ? `?${queryParams.toString()}` : ""
    }`;

    const response = await axios.get<PendingRequestsResponse>(url, {
      withCredentials: true, // Quan trọng: để gửi cookie
    });

    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<{ message: string }>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const message = axiosError.response.data?.message || "Có lỗi xảy ra";

        switch (status) {
          case 401:
            throw new Error(
              "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
            );
          case 500:
            throw new Error("Lỗi server. Vui lòng thử lại sau.");
          default:
            throw new Error(`Lỗi không xác định: ${message}`);
        }
      } else {
        throw new Error(
          "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng."
        );
      }
    } else {
      throw new Error("Có lỗi không xác định xảy ra.");
    }
  }
};
```

### React Component Example

```typescript
import React, { useState, useEffect } from "react";
import { getPendingRequests, PendingRequestResponse } from "./api";

const PendingRequestsList: React.FC = () => {
  const [requests, setRequests] = useState<PendingRequestsResponse | null>(
    null
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [notebookId, setNotebookId] = useState<string>("");
  const [status, setStatus] = useState<
    "pending" | "approved" | "rejected" | "blocked" | ""
  >("pending");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [sortBy, setSortBy] = useState<string>("createdAt");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);

  const loadRequests = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await getPendingRequests({
        notebookId: notebookId || undefined,
        status: status || undefined,
        q: searchQuery || undefined,
        sortBy,
        sortDir,
        page,
        size,
      });
      setRequests(data);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách yêu cầu");
      console.error("Error loading pending requests:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [notebookId, status, searchQuery, sortBy, sortDir, page, size]);

  const handleStatusChange = (newStatus: string) => {
    setStatus(newStatus as any);
    setPage(0); // Reset về trang đầu khi thay đổi filter
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setPage(0);
  };

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortBy(field);
      setSortDir("desc");
    }
    setPage(0);
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case "pending":
        return "status-badge status-pending";
      case "approved":
        return "status-badge status-approved";
      case "rejected":
        return "status-badge status-rejected";
      case "blocked":
        return "status-badge status-blocked";
      default:
        return "status-badge";
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case "pending":
        return "Đang chờ";
      case "approved":
        return "Đã duyệt";
      case "rejected":
        return "Đã từ chối";
      case "blocked":
        return "Đã chặn";
      default:
        return status;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (loading && !requests) {
    return <div className="loading">Đang tải...</div>;
  }

  return (
    <div className="pending-requests-container">
      <h1>Danh Sách Yêu Cầu Tham Gia</h1>

      {/* Filters */}
      <div className="filters">
        <div className="filter-group">
          <label>Notebook ID:</label>
          <input
            type="text"
            value={notebookId}
            onChange={(e) => setNotebookId(e.target.value)}
            placeholder="Lọc theo notebook ID..."
          />
        </div>

        <div className="filter-group">
          <label>Trạng thái:</label>
          <select
            value={status}
            onChange={(e) => handleStatusChange(e.target.value)}
          >
            <option value="">Tất cả</option>
            <option value="pending">Đang chờ</option>
            <option value="approved">Đã duyệt</option>
            <option value="rejected">Đã từ chối</option>
            <option value="blocked">Đã chặn</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Tìm kiếm:</label>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            placeholder="Tìm theo tên, email, notebook..."
          />
        </div>
      </div>

      {/* Error Message */}
      {error && <div className="error-message">{error}</div>}

      {/* Table */}
      {requests && (
        <>
          <table className="requests-table">
            <thead>
              <tr>
                <th onClick={() => handleSort("notebookTitle")}>
                  Notebook{" "}
                  {sortBy === "notebookTitle" && (
                    <span>{sortDir === "asc" ? "↑" : "↓"}</span>
                  )}
                </th>
                <th onClick={() => handleSort("userFullName")}>
                  Người yêu cầu{" "}
                  {sortBy === "userFullName" && (
                    <span>{sortDir === "asc" ? "↑" : "↓"}</span>
                  )}
                </th>
                <th>Email</th>
                <th>Role</th>
                <th>Trạng thái</th>
                <th onClick={() => handleSort("createdAt")}>
                  Ngày tạo{" "}
                  {sortBy === "createdAt" && (
                    <span>{sortDir === "asc" ? "↑" : "↓"}</span>
                  )}
                </th>
                <th>Tham gia</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {requests.items.map((request) => (
                <tr key={request.id}>
                  <td>{request.notebookTitle}</td>
                  <td>{request.userFullName}</td>
                  <td>{request.userEmail}</td>
                  <td>
                    <span className={`role-badge role-${request.role}`}>
                      {request.role}
                    </span>
                  </td>
                  <td>
                    <span className={getStatusBadgeClass(request.status)}>
                      {getStatusLabel(request.status)}
                    </span>
                  </td>
                  <td>{formatDate(request.createdAt)}</td>
                  <td>
                    {request.joinedAt
                      ? formatDate(request.joinedAt)
                      : "Chưa tham gia"}
                  </td>
                  <td>
                    {request.status === "pending" && (
                      <div className="action-buttons">
                        <button
                          onClick={() => handleApprove(request)}
                          className="btn-approve"
                        >
                          Duyệt
                        </button>
                        <button
                          onClick={() => handleReject(request)}
                          className="btn-reject"
                        >
                          Từ chối
                        </button>
                      </div>
                    )}
                    {request.status === "approved" && (
                      <button
                        onClick={() => handleBlock(request)}
                        className="btn-block"
                      >
                        Chặn
                      </button>
                    )}
                    {request.status === "blocked" && (
                      <button
                        onClick={() => handleUnblock(request)}
                        className="btn-unblock"
                      >
                        Mở chặn
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {requests.meta.totalPages > 1 && (
            <div className="pagination">
              <button
                onClick={() => setPage(page - 1)}
                disabled={page === 0}
                className="page-button"
              >
                Trước
              </button>

              <span className="page-info">
                Trang {requests.meta.page + 1} / {requests.meta.totalPages} (
                {requests.meta.totalElements} yêu cầu)
              </span>

              <button
                onClick={() => setPage(page + 1)}
                disabled={page >= requests.meta.totalPages - 1}
                className="page-button"
              >
                Sau
              </button>

              <select
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
                className="size-select"
              >
                <option value={10}>10 / trang</option>
                <option value={20}>20 / trang</option>
                <option value={50}>50 / trang</option>
                <option value={100}>100 / trang</option>
              </select>
            </div>
          )}
        </>
      )}

      {requests && requests.items.length === 0 && (
        <div className="empty-state">Không có yêu cầu nào</div>
      )}
    </div>
  );
};

export default PendingRequestsList;
```

### Hook Custom cho Pending Requests

```typescript
import { useState, useEffect } from "react";
import {
  getPendingRequests,
  PendingRequestsResponse,
  GetPendingRequestsParams,
} from "./api";

export const usePendingRequests = (params: GetPendingRequestsParams = {}) => {
  const [data, setData] = useState<PendingRequestsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await getPendingRequests(params);
      setData(result);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [JSON.stringify(params)]); // Re-fetch khi params thay đổi

  return {
    data,
    loading,
    error,
    refetch: fetchData,
  };
};
```

### Sử dụng Hook

```typescript
import { usePendingRequests } from "./hooks/usePendingRequests";

const MyComponent: React.FC = () => {
  const { data, loading, error, refetch } = usePendingRequests({
    status: "pending",
    page: 0,
    size: 20,
  });

  if (loading) return <div>Đang tải...</div>;
  if (error) return <div>Lỗi: {error}</div>;

  return (
    <div>
      {data?.items.map((request) => (
        <div key={request.id}>{request.userFullName}</div>
      ))}
      <button onClick={refetch}>Tải lại</button>
    </div>
  );
};
```

## Lưu ý quan trọng

1. **Cookie Authentication**:

   - API sử dụng cookie-based authentication
   - Cookie name: `AUTH-TOKEN`
   - Cần set `withCredentials: true` trong axios hoặc fetch config

2. **Status Filter Mặc định**:

   - Nếu không truyền `status`, mặc định sẽ là "pending"
   - Có thể truyền các status khác: "approved", "rejected", "blocked"

3. **Search Query**:

   - Tìm kiếm trong: notebook title, user fullName, user email
   - Không phân biệt hoa thường

4. **Sorting**:

   - Có thể sort theo: `createdAt`, `joinedAt`, `updatedAt`, `userFullName`, `notebookTitle`
   - Mặc định: sortBy = "createdAt", sortDir = "desc"

5. **Pagination**:

   - `page` là 0-based (trang đầu = 0)
   - Mặc định: page = 0, size = 10

## Best Practices

### 1. Debounce Search Input

```typescript
import { useDebouncedCallback } from "use-debounce";

const [searchQuery, setSearchQuery] = useState<string>("");

const debouncedSearch = useDebouncedCallback((value: string) => {
  setSearchQuery(value);
  setPage(0); // Reset về trang đầu khi search
}, 500);

<input
  type="text"
  onChange={(e) => debouncedSearch(e.target.value)}
  placeholder="Tìm kiếm..."
/>;
```

### 2. Reset Page khi Filter thay đổi

```typescript
const handleFilterChange = (newFilter: any) => {
  setFilter(newFilter);
  setPage(0); // Quan trọng: reset về trang đầu
};
```

### 3. Loading States

```typescript
{
  loading && !requests && <div>Đang tải...</div>;
}
{
  loading && requests && <div>Đang tải thêm...</div>;
}
```

### 4. Empty States

```typescript
{
  !loading && requests && requests.items.length === 0 && (
    <div className="empty-state">
      {searchQuery || status !== "pending"
        ? "Không tìm thấy yêu cầu nào"
        : "Chưa có yêu cầu nào"}
    </div>
  );
}
```

### 5. Error Handling

```typescript
{
  error && (
    <div className="error-message">
      {error}
      <button onClick={loadRequests}>Thử lại</button>
    </div>
  );
}
```

## Workflow đề xuất

1. **Hiển thị danh sách yêu cầu pending** (mặc định)
2. **Cho phép lọc**:
   - Theo notebook cụ thể
   - Theo status
   - Tìm kiếm theo keyword
3. **Sắp xếp** theo các field khác nhau
4. **Phân trang** để xem nhiều yêu cầu
5. **Thao tác**:
   - Approve/Reject cho pending requests
   - Block/Unblock cho approved/blocked members
6. **Refresh** danh sách sau khi thao tác

## Use Cases

### Use Case 1: Xem tất cả yêu cầu pending

```typescript
const { data } = usePendingRequests({
  status: "pending",
});
```

### Use Case 2: Xem yêu cầu của một notebook cụ thể

```typescript
const { data } = usePendingRequests({
  notebookId: "660e8400-e29b-41d4-a716-446655440001",
  status: "pending",
});
```

### Use Case 3: Tìm kiếm user đã request

```typescript
const { data } = usePendingRequests({
  q: "nguyenvana@example.com",
});
```

## Tổng kết

- ✅ Hỗ trợ phân trang đầy đủ
- ✅ Lọc theo notebookId, status
- ✅ Tìm kiếm đa trường (title, name, email)
- ✅ Sắp xếp linh hoạt
- ✅ Response đầy đủ thông tin (role, joinedAt, updatedAt)
- ✅ Optimized query (JOIN FETCH, không có N+1 problem)
- 💡 Nên sử dụng debounce cho search input
- 💡 Nên reset page khi filter thay đổi
