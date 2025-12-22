-- Thêm dữ liệu mẫu cho Teaching Assignment - Môn Giải thuật
-- Chạy: PGPASSWORD=Admin1234 psql -h localhost -U admin -d notebooks -f sample_data.sql

-- 1. Tạo môn Giải thuật
INSERT INTO subjects (id, code, name, credit, is_active, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'INF30087',
    'Cấu trúc dữ liệu và giải thuật',
    3,
    true,
    NOW(),
    NOW()
) ON CONFLICT (code) DO NOTHING;

-- 2. Tạo Teaching Assignment (ID = User ID của giảng viên do @MapsId)
INSERT INTO teaching_assignments (id, term_id, subject_id, teacher_user_id, status, created_at, created_by, approval_status, note)
VALUES (
    '22bfd357-e85d-40a7-8670-5bb5d545af83',
    '7ef2a9a7-cb2a-46f2-8440-fcad43230a61',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    '22bfd357-e85d-40a7-8670-5bb5d545af83',
    'ACTIVE',
    NOW(),
    'ADMIN',
    'APPROVED',
    'Phân công giảng dạy môn CTDL&GT HK1/2018-2019'
) ON CONFLICT (id) DO NOTHING;

-- 3. Tạo 3 lớp học
INSERT INTO classes (id, teaching_assignment_id, class_code, subject_code, subject_name, room, day_of_week, periods, start_date, end_date, is_active, created_at, updated_at)
VALUES 
(
    'c1a1b1c1-1111-2222-3333-444455556666',
    '22bfd357-e85d-40a7-8670-5bb5d545af83',
    'CTDL_01', 'INF30087', 'Cấu trúc dữ liệu và giải thuật',
    'A2.301', 2, '1-3', '2018-09-03', '2018-12-30', true, NOW(), NOW()
),
(
    'c2a2b2c2-2222-3333-4444-555566667777',
    '22bfd357-e85d-40a7-8670-5bb5d545af83',
    'CTDL_02', 'INF30087', 'Cấu trúc dữ liệu và giải thuật',
    'A2.302', 4, '4-6', '2018-09-03', '2018-12-30', true, NOW(), NOW()
),
(
    'c3a3b3c3-3333-4444-5555-666677778888',
    '22bfd357-e85d-40a7-8670-5bb5d545af83',
    'CTDL_03', 'INF30087', 'Cấu trúc dữ liệu và giải thuật',
    'A2.303', 6, '7-9', '2018-09-03', '2018-12-30', true, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- 4. Thêm 12 sinh viên vào các lớp
INSERT INTO class_members (id, class_id, student_code, first_name, last_name, full_name, dob, created_at)
VALUES 
-- Lớp CTDL_01 - 5 sinh viên
('m11a1111-1111-1111-1111-111111111111', 'c1a1b1c1-1111-2222-3333-444455556666', 'SV001', 'Văn', 'Nguyễn', 'Nguyễn Văn A', '2000-01-15', NOW()),
('m12a1111-1111-1111-1111-111111111112', 'c1a1b1c1-1111-2222-3333-444455556666', 'SV002', 'Thị', 'Trần', 'Trần Thị B', '2000-03-20', NOW()),
('m13a1111-1111-1111-1111-111111111113', 'c1a1b1c1-1111-2222-3333-444455556666', 'SV003', 'Hoàng', 'Lê', 'Lê Hoàng C', '2000-05-10', NOW()),
('m14a1111-1111-1111-1111-111111111114', 'c1a1b1c1-1111-2222-3333-444455556666', 'SV004', 'Minh', 'Phạm', 'Phạm Minh D', '2000-07-25', NOW()),
('m15a1111-1111-1111-1111-111111111115', 'c1a1b1c1-1111-2222-3333-444455556666', 'SV005', 'Hương', 'Hoàng', 'Hoàng Hương E', '2000-09-30', NOW()),
-- Lớp CTDL_02 - 4 sinh viên
('m21a2222-2222-2222-2222-222222222221', 'c2a2b2c2-2222-3333-4444-555566667777', 'SV006', 'Tuấn', 'Vũ', 'Vũ Tuấn F', '2000-02-14', NOW()),
('m22a2222-2222-2222-2222-222222222222', 'c2a2b2c2-2222-3333-4444-555566667777', 'SV007', 'Lan', 'Đỗ', 'Đỗ Lan G', '2000-04-08', NOW()),
('m23a2222-2222-2222-2222-222222222223', 'c2a2b2c2-2222-3333-4444-555566667777', 'SV008', 'Hùng', 'Bùi', 'Bùi Hùng H', '2000-06-12', NOW()),
('m24a2222-2222-2222-2222-222222222224', 'c2a2b2c2-2222-3333-4444-555566667777', 'SV009', 'Mai', 'Đặng', 'Đặng Mai I', '2000-08-18', NOW()),
-- Lớp CTDL_03 - 3 sinh viên
('m31a3333-3333-3333-3333-333333333331', 'c3a3b3c3-3333-4444-5555-666677778888', 'SV010', 'Khoa', 'Ngô', 'Ngô Khoa K', '2000-10-05', NOW()),
('m32a3333-3333-3333-3333-333333333332', 'c3a3b3c3-3333-4444-5555-666677778888', 'SV011', 'Linh', 'Phan', 'Phan Linh L', '2000-11-20', NOW()),
('m33a3333-3333-3333-3333-333333333333', 'c3a3b3c3-3333-4444-5555-666677778888', 'SV012', 'Nam', 'Lý', 'Lý Nam M', '2000-12-25', NOW())
ON CONFLICT DO NOTHING;
