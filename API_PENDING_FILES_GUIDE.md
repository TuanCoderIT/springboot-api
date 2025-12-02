    # API Hướng dẫn: Danh sách Files Pending (Chờ Duyệt)

    ## Mục lục

    1. [Tổng quan](#1-tổng-quan)
    2. [Lấy danh sách Files Pending](#2-lấy-danh-sách-files-pending)
    3. [Ví dụ sử dụng](#3-ví-dụ-sử-dụng)
    4. [Ví dụ code (JavaScript/TypeScript)](#4-ví-dụ-code-javascripttypescript)

    ---

    ## 1. Tổng quan

    API này cho phép admin lấy danh sách **tất cả các files có status = "pending"** (chờ duyệt) trong hệ thống, với khả năng:

    - **Lọc theo notebook** (optional): Chỉ lấy files trong một notebook cụ thể
    - **Lọc theo loại file** (MIME type)
    - **Lọc theo người upload**
    - **Tìm kiếm** theo tên file
    - **Phân trang** (pagination)
    - **Sắp xếp** (sorting) theo nhiều tiêu chí

    ---

    ## 2. Lấy danh sách Files Pending

    ### Endpoint

    ```
    GET /admin/files/pending
    ```

    ### Mô tả

    Lấy danh sách tất cả files có status = "pending" (chờ duyệt) với đầy đủ tính năng: lọc (filter), tìm kiếm (search), phân trang (pagination) và sắp xếp (sort).

    **Lưu ý**: Nếu không truyền `notebookId`, API sẽ trả về **tất cả files pending** trong toàn bộ hệ thống. Nếu truyền `notebookId`, chỉ lấy files pending trong notebook đó.

    ### Authentication

    Yêu cầu Bearer Token trong header:

    ```
    Authorization: Bearer <token>
    ```

    ### Query Parameters

    Tất cả query parameters đều **optional** (không bắt buộc).

    | Parameter    | Type    | Default     | Description                                       |
    | ------------ | ------- | ----------- | ------------------------------------------------- |
    | `notebookId` | UUID    | -           | Lọc theo notebook (nếu không truyền = lấy tất cả) |
    | `mimeType`   | String  | -           | Lọc theo loại file                                |
    | `uploadedBy` | UUID    | -           | Lọc theo ID người đóng góp                        |
    | `search`     | String  | -           | Tìm kiếm theo tên file (case-insensitive)         |
    | `sortBy`     | String  | `createdAt` | Sắp xếp theo field                                |
    | `page`       | Integer | `0`         | Số trang (bắt đầu từ 0)                           |
    | `size`       | Integer | `20`        | Số lượng items mỗi trang (1-100)                  |

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

    ### Response Format

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
         "status": "pending",
         "pagesCount": 10,
         "ocrDone": false,
         "embeddingDone": false,
         "chunkSize": 800,
         "chunkOverlap": 120,
         "chunksCount": 0,
         "uploadedBy": {
             "id": "user-uuid",
             "fullName": "Nguyễn Văn A",
             "email": "nguyenvana@example.com",
             "avatarUrl": "/uploads/avatar.jpg"
         },
         "notebook": {
             "id": "notebook-uuid",
             "title": "Tên Notebook",
             "description": "Mô tả notebook",
             "type": "community",
             "visibility": "public",
             "thumbnailUrl": "/uploads/thumbnail.jpg"
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

    | Field              | Type           | Description                                     |
    | ------------------ | -------------- | ----------------------------------------------- |
    | `id`               | UUID           | ID của file                                     |
    | `originalFilename` | String         | Tên file gốc                                    |
    | `mimeType`         | String         | Loại MIME của file                              |
    | `fileSize`         | Long           | Kích thước file (bytes)                         |
    | `storageUrl`       | String         | Đường dẫn lưu trữ file                          |
    | `status`           | String         | Trạng thái file (luôn là "pending" cho API này) |
    | `pagesCount`       | Integer        | Số trang (cho PDF)                              |
    | `ocrDone`          | Boolean        | Đã hoàn thành OCR chưa                          |
    | `embeddingDone`    | Boolean        | Đã tạo embedding chưa                           |
    | `chunkSize`        | Integer        | Kích thước chunk (khi chia text)                |
    | `chunkOverlap`     | Integer        | Độ overlap giữa các chunk                       |
     | `chunksCount`      | Long           | **Tổng số chunks** đã được tạo từ file này      |
     | `uploadedBy`       | UploaderInfo   | **Thông tin người đóng góp file**               |
     | `notebook`         | NotebookInfo   | **Thông tin notebook chứa file này**            |
     | `createdAt`        | OffsetDateTime | Thời gian upload (ISO 8601)                     |
     | `updatedAt`        | OffsetDateTime | Thời gian cập nhật (ISO 8601)                   |

    #### UploaderInfo (Nested Object)

     | Field       | Type   | Description                    |
     | ----------- | ------ | ------------------------------ |
     | `id`        | UUID   | ID của người đóng góp          |
     | `fullName`  | String | Tên đầy đủ                     |
     | `email`     | String | Email                          |
     | `avatarUrl` | String | URL ảnh đại diện (có thể null) |

     #### NotebookInfo (Nested Object)

     | Field         | Type   | Description                                    |
     | ------------- | ------ | ---------------------------------------------- |
     | `id`          | UUID   | ID của notebook                                |
     | `title`       | String | Tiêu đề notebook                               |
     | `description` | String | Mô tả notebook (có thể null)                   |
     | `type`        | String | Loại notebook: "community", "private_group", "personal" |
     | `visibility`  | String | Hiển thị: "public", "private"                 |
     | `thumbnailUrl`| String | URL ảnh thumbnail (có thể null)                |

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

    ---

    ## 3. Ví dụ sử dụng

    ### 1. Lấy tất cả files pending trong hệ thống (trang đầu tiên)

    ```bash
    GET /admin/files/pending
    ```

    Hoặc với query params mặc định:

    ```bash
    GET /admin/files/pending?page=0&size=20&sortBy=createdAt
    ```

    ### 2. Lấy files pending trong một notebook cụ thể

    ```bash
    GET /admin/files/pending?notebookId=c3a7f558-faa7-4218-ae41-4ef57f976f34
    ```

    ### 3. Lọc theo loại file

    ```bash
    GET /admin/files/pending?mimeType=application/pdf
    ```

    ### 4. Lọc theo người upload

    ```bash
    GET /admin/files/pending?uploadedBy=user-uuid-here
    ```

    ### 5. Tìm kiếm theo tên file

    Search sẽ tìm kiếm **không phân biệt hoa thường** trong tên file.

    ```bash
    GET /admin/files/pending?search=report
    ```

    ### 6. Sắp xếp theo tên file

    ```bash
    GET /admin/files/pending?sortBy=originalFilename
    ```

    ### 7. Phân trang

    ```bash
    GET /admin/files/pending?page=1&size=10
    ```

    ### 8. Kết hợp tất cả filters

    ```bash
    GET /admin/files/pending?notebookId=c3a7f558-faa7-4218-ae41-4ef57f976f34&mimeType=application/pdf&search=document&sortBy=updatedAt&page=0&size=10
    ```

    ### 9. Lấy files pending từ một notebook và lọc theo người upload

    ```bash
    GET /admin/files/pending?notebookId=c3a7f558-faa7-4218-ae41-4ef57f976f34&uploadedBy=user-uuid-here
    ```

    ---

    ## 4. Ví dụ code (JavaScript/TypeScript)

    ### Fetch API

    ```javascript
    async function getPendingFiles(filters = {}) {
    const params = new URLSearchParams({
        page: filters.page || 0,
        size: filters.size || 20,
        sortBy: filters.sortBy || "createdAt",
    });

    if (filters.notebookId) params.append("notebookId", filters.notebookId);
    if (filters.mimeType) params.append("mimeType", filters.mimeType);
    if (filters.uploadedBy) params.append("uploadedBy", filters.uploadedBy);
    if (filters.search) params.append("search", filters.search);

    const response = await fetch(`/admin/files/pending?${params}`, {
        headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
        },
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
    }

    // Sử dụng: Lấy tất cả files pending
    const allPending = await getPendingFiles({
    sortBy: "createdAt",
    page: 0,
    size: 20,
    });

    // Sử dụng: Lấy files pending trong một notebook
    const notebookPending = await getPendingFiles({
    notebookId: "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    search: "report",
    sortBy: "createdAt",
    page: 0,
    size: 20,
    });

     console.log(`Tổng số files pending: ${allPending.totalElements}`);
     console.log(`Trang ${allPending.page + 1}/${allPending.totalPages}`);
     allPending.content.forEach((file) => {
     console.log(`${file.originalFilename} - ${file.chunksCount} chunks`);
     console.log(
         `Đóng góp bởi: ${file.uploadedBy.fullName} (${file.uploadedBy.email})`
     );
     console.log(
         `Notebook: ${file.notebook.title} (${file.notebook.type})`
     );
     });
    ```

    ### Axios

    ```javascript
    import axios from "axios";

    async function getPendingFiles(filters = {}) {
    const response = await axios.get("/admin/files/pending", {
        params: {
        notebookId: filters.notebookId,
        mimeType: filters.mimeType,
        uploadedBy: filters.uploadedBy,
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

    // Sử dụng
    const result = await getPendingFiles({
    notebookId: "c3a7f558-faa7-4218-ae41-4ef57f976f34",
    search: "report",
    sortBy: "createdAt",
    page: 0,
    size: 20,
    });
    ```

    ### React Hook Example

    ```typescript
    import { useState, useEffect } from "react";

    interface PendingFileFilters {
    notebookId?: string;
    mimeType?: string;
    uploadedBy?: string;
    search?: string;
    sortBy?: string;
    page?: number;
    size?: number;
    }

    interface UsePendingFilesReturn {
    files: NotebookFileResponse[];
    loading: boolean;
    error: Error | null;
    pagination: {
        page: number;
        size: number;
        totalElements: number;
        totalPages: number;
    };
    refetch: () => void;
    }

    function usePendingFiles(
    filters: PendingFileFilters = {}
    ): UsePendingFilesReturn {
    const [files, setFiles] = useState<NotebookFileResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const [pagination, setPagination] = useState({
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
    });

    const fetchPendingFiles = async () => {
        try {
        setLoading(true);
        const params = new URLSearchParams({
            page: String(filters.page ?? 0),
            size: String(filters.size ?? 20),
            sortBy: filters.sortBy || "createdAt",
        });

        if (filters.notebookId) params.append("notebookId", filters.notebookId);
        if (filters.mimeType) params.append("mimeType", filters.mimeType);
        if (filters.uploadedBy) params.append("uploadedBy", filters.uploadedBy);
        if (filters.search) params.append("search", filters.search);

        const response = await fetch(`/admin/files/pending?${params}`, {
            headers: {
            Authorization: `Bearer ${token}`,
            Accept: "application/json",
            },
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        setFiles(data.content);
        setPagination({
            page: data.page,
            size: data.size,
            totalElements: data.totalElements,
            totalPages: data.totalPages,
        });
        setError(null);
        } catch (err) {
        setError(err instanceof Error ? err : new Error("Unknown error"));
        } finally {
        setLoading(false);
        }
    };

    useEffect(() => {
        fetchPendingFiles();
    }, [
        filters.notebookId,
        filters.mimeType,
        filters.uploadedBy,
        filters.search,
        filters.sortBy,
        filters.page,
        filters.size,
    ]);

    return {
        files,
        loading,
        error,
        pagination,
        refetch: fetchPendingFiles,
    };
    }

    // Sử dụng trong component
    function PendingFilesList() {
    const [filters, setFilters] = useState<PendingFileFilters>({
        page: 0,
        size: 20,
        sortBy: "createdAt",
    });

    const { files, loading, error, pagination, refetch } =
        usePendingFiles(filters);

    if (loading) return <div>Đang tải...</div>;
    if (error) return <div>Lỗi: {error.message}</div>;

    return (
        <div>
        <h1>Danh sách Files Pending</h1>
        <div>
            <input
            type="text"
            placeholder="Tìm kiếm..."
            onChange={(e) =>
                setFilters({ ...filters, search: e.target.value, page: 0 })
            }
            />
            <select
            onChange={(e) =>
                setFilters({ ...filters, sortBy: e.target.value, page: 0 })
            }
            >
            <option value="createdAt">Mới nhất</option>
            <option value="createdAtAsc">Cũ nhất</option>
            <option value="originalFilename">Tên A-Z</option>
            <option value="originalFilenameDesc">Tên Z-A</option>
            </select>
        </div>
        <div>
            <p>
            Tổng số: {pagination.totalElements} files | Trang{" "}
            {pagination.page + 1}/{pagination.totalPages}
            </p>
        </div>
        <ul>
            {files.map((file) => (
             <li key={file.id}>
             <h3>{file.originalFilename}</h3>
             <p>
                 Đóng góp bởi: {file.uploadedBy.fullName} ({file.uploadedBy.email})
             </p>
             <p>
                 Notebook: {file.notebook.title} ({file.notebook.type})
             </p>
             <p>Kích thước: {(file.fileSize / 1024).toFixed(2)} KB</p>
             <p>Chunks: {file.chunksCount}</p>
             </li>
            ))}
        </ul>
        <div>
            <button
            disabled={pagination.page === 0}
            onClick={() => setFilters({ ...filters, page: filters.page! - 1 })}
            >
            Trước
            </button>
            <button
            disabled={pagination.page >= pagination.totalPages - 1}
            onClick={() => setFilters({ ...filters, page: filters.page! + 1 })}
            >
            Sau
            </button>
        </div>
        </div>
    );
    }
    ```

    ### TypeScript Types

    ```typescript
    interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    }

     interface NotebookFileResponse {
     id: string;
     originalFilename: string;
     mimeType: string;
     fileSize: number;
     storageUrl: string;
     status:
         | "pending"
         | "approved"
         | "rejected"
         | "processing"
         | "failed"
         | "done";
     pagesCount: number | null;
     ocrDone: boolean;
     embeddingDone: boolean;
     chunkSize: number;
     chunkOverlap: number;
     chunksCount: number;
     uploadedBy: UploaderInfo;
     notebook: NotebookInfo;
     createdAt: string;
     updatedAt: string;
     }

     interface UploaderInfo {
     id: string;
     fullName: string;
     email: string;
     avatarUrl: string | null;
     }

     interface NotebookInfo {
     id: string;
     title: string;
     description: string | null;
     type: "community" | "private_group" | "personal";
     visibility: "public" | "private";
     thumbnailUrl: string | null;
     }
    ```

    ---

    ## Ghi chú quan trọng

    1. **Status luôn là "pending"**: Tất cả files trả về từ API này đều có `status = "pending"` vì API chỉ lấy files đang chờ duyệt.

    2. **NotebookId optional**:

    - Nếu **không truyền** `notebookId`: Trả về **tất cả files pending** trong toàn bộ hệ thống
    - Nếu **có truyền** `notebookId`: Chỉ trả về files pending trong notebook đó

    3. **Phân trang**:

    - `page` bắt đầu từ 0
    - `size` phải từ 1 đến 100

    4. **Tìm kiếm**: Search sẽ tìm kiếm trong tên file (`originalFilename`), không phân biệt hoa thường.

    5. **Sắp xếp mặc định**: Nếu không chỉ định `sortBy`, sẽ sắp xếp theo `createdAt` (mới nhất trước).
