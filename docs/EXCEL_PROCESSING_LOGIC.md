# Logic Xử Lý Excel Linh Hoạt

## Tổng Quan
Logic mới được thiết kế để xử lý các file Excel có cấu trúc khác nhau mà không cần hardcode index dòng/cột.

## Các Bước Xử Lý

### 1. Detect Header Row
- Duyệt từ dòng 0 đến dòng 10 (tối đa)
- Tìm dòng chứa ít nhất 2 trong 3 pattern:
  - `Mã sinh viên` hoặc `MSSV`
  - `Họ và tên`, `Họ`, hoặc `Tên`
  - `Ngày sinh`
- Bỏ qua toàn bộ dòng phía trên header

### 2. Column Mapping
Sau khi tìm được header row, map các cột theo tên:
- `studentCodeCol` ← cột chứa "Mã sinh viên" hoặc "MSSV"
- `dateOfBirthCol` ← cột chứa "Ngày sinh"
- Xử lý họ tên theo 3 case:

### 3. Xử Lý Họ Tên (3 Cases)

#### Case 1: Separate Columns
- Có 2 header riêng: "Họ" và "Tên"
- `lastName` ← cột "Họ"
- `firstName` ← cột "Tên"
- `fullName` ← lastName + " " + firstName

#### Case 2: Merged Header Split Data
- Header "Họ và tên" bị merge ≥ 2 cột
- Dữ liệu thực tế tách ra 2 cột
- `lastName` ← cột bên trái
- `firstName` ← cột bên phải
- `fullName` ← lastName + " " + firstName

#### Case 3: Single Column
- "Họ và tên" thực sự là 1 cột
- `fullName` ← giá trị cell
- `firstName` ← từ cuối cùng
- `lastName` ← phần còn lại

### 4. Validation & Normalization
- Chỉ xử lý dòng có:
  - `studentCode` không rỗng và hợp lệ (số/chữ/ký tự đặc biệt)
  - `fullName` không rỗng
  - `dateOfBirth` parse được (optional)
- Chuẩn hóa: trim string, gộp nhiều space

## Tại Sao Logic Này Không Phụ Thuộc File Mẫu?

### 1. Dynamic Header Detection
- Không hardcode "header ở dòng 1"
- Tự động tìm dòng chứa thông tin sinh viên
- Hoạt động với file có nhiều dòng hành chính phía trên

### 2. Pattern-Based Column Mapping
- Sử dụng regex pattern thay vì index cố định
- Hỗ trợ nhiều cách viết: "Mã sinh viên", "MSSV", "Họ và tên", v.v.
- Không quan tâm thứ tự cột

### 3. Intelligent Name Handling
- Tự động detect cách tổ chức họ tên
- Xử lý được cả merged cell và separate columns
- Fallback logic cho các trường hợp edge case

### 4. Flexible Data Structure
- Không giả định cấu trúc cố định
- Kiểm tra merged regions động
- Validate dữ liệu thực tế để quyết định logic

## Ví Dụ Các File Được Hỗ Trợ

### File Type 1: Standard
```
| Mã sinh viên | Họ và tên | Ngày sinh |
| 2021001     | Nguyễn Văn A | 01/01/2000 |
```

### File Type 2: Separate Name Columns
```
| MSSV    | Họ      | Tên | Ngày sinh |
| 2021001 | Nguyễn Văn | A   | 01/01/2000 |
```

### File Type 3: Merged Header (như file mẫu)
```
| Mã sinh viên | Họ và tên      | Ngày sinh |
|              | (merged cell)  |           |
| 2021001     | Nguyễn Văn | A | 01/01/2000 |
```

### File Type 4: With Administrative Rows
```
Trường Đại học ABC
Khoa Công nghệ Thông tin
Danh sách sinh viên lớp IT01

| Mã sinh viên | Họ và tên | Ngày sinh |
| 2021001     | Nguyễn Văn A | 01/01/2000 |
```

Tất cả các file trên đều được xử lý chính xác bởi logic mới.