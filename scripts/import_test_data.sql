-- =====================================================
-- IMPORT LỚP HỌC VÀ SINH VIÊN ẢO
-- Run: PGPASSWORD=Admin1234 psql -h localhost -U admin -d notebooks -f scripts/import_test_data.sql
-- =====================================================

-- Lấy teaching_assignment_id đầu tiên để tạo class
DO $$
DECLARE
    v_assignment_id UUID;
    v_class_id_1 UUID := uuid_generate_v4();
    v_class_id_2 UUID := uuid_generate_v4();
BEGIN
    -- Lấy assignment đầu tiên
    SELECT id INTO v_assignment_id FROM teaching_assignments LIMIT 1;
    
    IF v_assignment_id IS NULL THEN
        RAISE EXCEPTION 'Không có teaching_assignment nào trong database!';
    END IF;

    -- =====================================================
    -- TẠO 2 LỚP HỌC ẢO
    -- =====================================================
    INSERT INTO classes (id, teaching_assignment_id, class_code, subject_code, subject_name, room, day_of_week, periods, start_date, end_date, is_active, created_at, updated_at)
    VALUES 
        (v_class_id_1, v_assignment_id, 'CTDL-01', 'CT001', 'Cấu trúc dữ liệu', 'A301', 2, '1-3', '2025-01-06', '2025-05-30', true, NOW(), NOW()),
        (v_class_id_2, v_assignment_id, 'CTDL-02', 'CT001', 'Cấu trúc dữ liệu', 'B205', 4, '7-9', '2025-01-06', '2025-05-30', true, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- =====================================================
    -- TẠO 20 SINH VIÊN ẢO CHO LỚP 1
    -- =====================================================
    INSERT INTO class_members (id, class_id, student_code, first_name, last_name, full_name, dob, created_at)
    VALUES
        (uuid_generate_v4(), v_class_id_1, '22571101001', 'Văn A', 'Nguyễn', 'Nguyễn Văn A', '2004-03-15', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101002', 'Thị B', 'Trần', 'Trần Thị B', '2004-07-22', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101003', 'Văn C', 'Lê', 'Lê Văn C', '2004-01-10', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101004', 'Thị D', 'Phạm', 'Phạm Thị D', '2004-09-05', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101005', 'Văn E', 'Hoàng', 'Hoàng Văn E', '2004-12-20', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101006', 'Thị F', 'Huỳnh', 'Huỳnh Thị F', '2004-04-18', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101007', 'Văn G', 'Vũ', 'Vũ Văn G', '2004-06-25', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101008', 'Thị H', 'Võ', 'Võ Thị H', '2004-08-30', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101009', 'Văn I', 'Đặng', 'Đặng Văn I', '2004-02-14', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101010', 'Thị K', 'Bùi', 'Bùi Thị K', '2004-11-08', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101011', 'Văn L', 'Đỗ', 'Đỗ Văn L', '2004-05-12', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101012', 'Thị M', 'Hồ', 'Hồ Thị M', '2004-10-03', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101013', 'Văn N', 'Ngô', 'Ngô Văn N', '2004-03-28', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101014', 'Thị O', 'Dương', 'Dương Thị O', '2004-07-17', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101015', 'Văn P', 'Lý', 'Lý Văn P', '2004-01-25', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101016', 'Thị Q', 'Trương', 'Trương Thị Q', '2004-09-14', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101017', 'Văn R', 'Đinh', 'Đinh Văn R', '2004-12-07', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101018', 'Thị S', 'Phan', 'Phan Thị S', '2004-04-22', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101019', 'Văn T', 'Mai', 'Mai Văn T', '2004-06-11', NOW()),
        (uuid_generate_v4(), v_class_id_1, '22571101020', 'Thị U', 'Tô', 'Tô Thị U', '2004-08-19', NOW())
    ON CONFLICT DO NOTHING;

    -- =====================================================
    -- TẠO 15 SINH VIÊN ẢO CHO LỚP 2
    -- =====================================================
    INSERT INTO class_members (id, class_id, student_code, first_name, last_name, full_name, dob, created_at)
    VALUES
        (uuid_generate_v4(), v_class_id_2, '22571102001', 'Minh Anh', 'Nguyễn', 'Nguyễn Minh Anh', '2003-05-20', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102002', 'Quốc Bảo', 'Trần', 'Trần Quốc Bảo', '2003-11-15', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102003', 'Hoàng Việt', 'Lê', 'Lê Hoàng Việt', '2003-02-28', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102004', 'Thanh Hương', 'Phạm', 'Phạm Thanh Hương', '2003-08-08', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102005', 'Đức Minh', 'Hoàng', 'Hoàng Đức Minh', '2003-04-12', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102006', 'Thùy Linh', 'Huỳnh', 'Huỳnh Thùy Linh', '2003-10-25', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102007', 'Tuấn Kiệt', 'Vũ', 'Vũ Tuấn Kiệt', '2003-06-30', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102008', 'Ngọc Lan', 'Võ', 'Võ Ngọc Lan', '2003-12-18', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102009', 'Văn Khôi', 'Đặng', 'Đặng Văn Khôi', '2003-03-05', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102010', 'Thu Hà', 'Bùi', 'Bùi Thu Hà', '2003-09-22', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102011', 'Anh Tuấn', 'Đỗ', 'Đỗ Anh Tuấn', '2003-01-14', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102012', 'Phương Mai', 'Hồ', 'Hồ Phương Mai', '2003-07-07', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102013', 'Đình Phúc', 'Ngô', 'Ngô Đình Phúc', '2003-05-01', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102014', 'Kim Chi', 'Dương', 'Dương Kim Chi', '2003-11-28', NOW()),
        (uuid_generate_v4(), v_class_id_2, '22571102015', 'Hải Long', 'Lý', 'Lý Hải Long', '2003-02-10', NOW())
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Import thành công! Đã tạo 2 lớp và 35 sinh viên.';
    RAISE NOTICE 'Class 1 ID: %', v_class_id_1;
    RAISE NOTICE 'Class 2 ID: %', v_class_id_2;
END $$;

-- Kiểm tra kết quả
SELECT 
    c.id,
    c.class_code,
    c.subject_name,
    c.room,
    (SELECT COUNT(*) FROM class_members cm WHERE cm.class_id = c.id) as student_count
FROM classes c
ORDER BY c.created_at DESC
LIMIT 5;
