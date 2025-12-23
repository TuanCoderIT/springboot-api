-- Migration script cho tính năng quản lý lớp học phần
-- Chạy script này nếu database chưa có các bảng cần thiết

-- Kiểm tra và tạo bảng classes nếu chưa có
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teaching_assignment_id UUID NOT NULL,
    class_code TEXT NOT NULL,
    subject_code TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    room TEXT,
    day_of_week INTEGER,
    periods TEXT,
    start_date DATE,
    end_date DATE,
    note TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_classes_teaching_assignment 
        FOREIGN KEY (teaching_assignment_id) 
        REFERENCES teaching_assignments(id) 
        ON DELETE CASCADE,
    CONSTRAINT uq_classes_assignment_classcode 
        UNIQUE (teaching_assignment_id, class_code)
);

-- Tạo indexes cho classes
CREATE INDEX IF NOT EXISTS idx_classes_assignment ON classes(teaching_assignment_id);
CREATE INDEX IF NOT EXISTS idx_classes_subject_code ON classes(subject_code);
CREATE INDEX IF NOT EXISTS idx_classes_time ON classes(start_date, end_date);

-- Kiểm tra và tạo bảng class_members nếu chưa có
CREATE TABLE IF NOT EXISTS class_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    student_code TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    full_name TEXT NOT NULL,
    dob DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_class_members_class 
        FOREIGN KEY (class_id) 
        REFERENCES classes(id) 
        ON DELETE CASCADE,
    CONSTRAINT uq_class_student 
        UNIQUE (class_id, student_code)
);

-- Tạo indexes cho class_members
CREATE INDEX IF NOT EXISTS idx_class_members_class ON class_members(class_id);
CREATE INDEX IF NOT EXISTS idx_class_members_student_code ON class_members(student_code);

-- Thêm cột notebook_id vào teaching_assignments nếu chưa có
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teaching_assignments' 
        AND column_name = 'notebook_id'
    ) THEN
        ALTER TABLE teaching_assignments 
        ADD COLUMN notebook_id UUID,
        ADD CONSTRAINT fk_teaching_assignments_notebook 
            FOREIGN KEY (notebook_id) 
            REFERENCES notebooks(id) 
            ON DELETE SET NULL;
        
        CREATE INDEX idx_teaching_assignments_notebook 
        ON teaching_assignments(notebook_id);
    END IF;
END $$;

-- Thêm index cho student_code trong users nếu chưa có
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_student_code 
ON users(student_code) 
WHERE student_code IS NOT NULL;

-- Thêm index cho lecturer_code trong users nếu chưa có
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_lecturer_code 
ON users(lecturer_code) 
WHERE lecturer_code IS NOT NULL;

-- Comments cho documentation
COMMENT ON TABLE classes IS 'Bảng lưu thông tin các lớp học phần';
COMMENT ON TABLE class_members IS 'Bảng lưu danh sách sinh viên trong từng lớp học phần';
COMMENT ON COLUMN teaching_assignments.notebook_id IS 'Notebook cộng đồng được gắn với phân công giảng dạy';
COMMENT ON COLUMN class_members.student_code IS 'Mã sinh viên (MSSV)';

-- Tạo function để tự động update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Tạo trigger cho classes
DROP TRIGGER IF EXISTS update_classes_updated_at ON classes;
CREATE TRIGGER update_classes_updated_at 
    BEFORE UPDATE ON classes 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();