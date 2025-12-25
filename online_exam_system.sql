-- =====================================================
-- ONLINE EXAM SYSTEM SCHEMA FOR POSTGRESQL
-- =====================================================
-- Thiết kế hệ thống thi online tích hợp với DB hiện có
-- Hỗ trợ đa dạng loại câu hỏi và mở rộng trong tương lai
-- =====================================================

-- =====================================================
-- 1. EXAM MANAGEMENT TABLES
-- =====================================================

-- Bảng quản lý kỳ thi
CREATE TABLE exams (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    
    -- Cấu hình thời gian
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    
    -- Cấu hình thi
    total_questions INTEGER NOT NULL DEFAULT 0,
    total_points DECIMAL(10,2) NOT NULL DEFAULT 0,
    passing_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    
    -- Cài đặt thi
    shuffle_questions BOOLEAN DEFAULT true,
    shuffle_options BOOLEAN DEFAULT true,
    show_results_immediately BOOLEAN DEFAULT false,
    allow_review BOOLEAN DEFAULT true,
    max_attempts INTEGER DEFAULT 1 CHECK (max_attempts > 0),
    
    -- Anti-cheat settings (mở rộng tương lai)
    enable_proctoring BOOLEAN DEFAULT false,
    enable_lockdown BOOLEAN DEFAULT false,
    enable_plagiarism_check BOOLEAN DEFAULT false,
    
    -- Metadata và trạng thái
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_exam_time_valid CHECK (end_time > start_time),
    CONSTRAINT chk_passing_score_valid CHECK (passing_score >= 0 AND passing_score <= total_points)
);

-- =====================================================
-- 2. QUESTION MANAGEMENT TABLES
-- =====================================================

-- Bảng câu hỏi (đa hình, hỗ trợ nhiều loại)
CREATE TABLE exam_questions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    
    -- Thông tin câu hỏi
    question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('MCQ', 'ESSAY', 'CODING', 'TRUE_FALSE', 'FILL_BLANK', 'MATCHING')),
    question_text TEXT NOT NULL,
    question_image_url TEXT,
    question_audio_url TEXT,
    
    -- Điểm số và thứ tự
    points DECIMAL(10,2) NOT NULL DEFAULT 1.0 CHECK (points > 0),
    order_index INTEGER NOT NULL,
    
    -- Cấu hình câu hỏi
    time_limit_seconds INTEGER, -- Giới hạn thời gian cho câu hỏi (optional)
    difficulty_level VARCHAR(10) DEFAULT 'MEDIUM' CHECK (difficulty_level IN ('EASY', 'MEDIUM', 'HARD')),
    
    -- Metadata cho các loại câu hỏi khác nhau
    question_config JSONB DEFAULT '{}', -- Cấu hình riêng cho từng loại câu hỏi
    correct_answer JSONB, -- Đáp án đúng (format khác nhau theo loại câu hỏi)
    explanation TEXT, -- Giải thích đáp án
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint
    UNIQUE(exam_id, order_index)
);

-- Bảng lựa chọn cho câu hỏi trắc nghiệm
CREATE TABLE exam_question_options (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES exam_questions(id) ON DELETE CASCADE,
    
    -- Nội dung lựa chọn
    option_text TEXT NOT NULL,
    option_image_url TEXT,
    option_audio_url TEXT,
    
    -- Thứ tự và trạng thái
    order_index INTEGER NOT NULL,
    is_correct BOOLEAN DEFAULT false,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint
    UNIQUE(question_id, order_index)
);

-- =====================================================
-- 3. EXAM ATTEMPT MANAGEMENT
-- =====================================================

-- Bảng lượt thi của sinh viên
CREATE TABLE exam_attempts (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Thông tin lượt thi
    attempt_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'CANCELLED', 'GRADED')),
    
    -- Thời gian
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    submitted_at TIMESTAMP WITH TIME ZONE,
    time_spent_seconds INTEGER DEFAULT 0,
    
    -- Điểm số
    total_score DECIMAL(10,2) DEFAULT 0,
    percentage_score DECIMAL(5,2) DEFAULT 0,
    is_passed BOOLEAN,
    
    -- Cấu hình lượt thi (snapshot tại thời điểm thi)
    exam_snapshot JSONB NOT NULL, -- Snapshot của exam config
    questions_snapshot JSONB NOT NULL, -- Snapshot của questions và options
    
    -- Anti-cheat data (mở rộng tương lai)
    browser_info JSONB DEFAULT '{}',
    ip_address VARCHAR(45), -- Thay đổi từ INET thành VARCHAR để tương thích với entity
    user_agent TEXT,
    proctoring_data JSONB DEFAULT '{}',
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint
    UNIQUE(exam_id, student_id, attempt_number)
);

-- =====================================================
-- 4. ANSWER MANAGEMENT
-- =====================================================

-- Bảng câu trả lời của sinh viên
CREATE TABLE exam_answers (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES exam_questions(id) ON DELETE CASCADE,
    
    -- Câu trả lời (đa hình theo loại câu hỏi)
    answer_type VARCHAR(20) NOT NULL CHECK (answer_type IN ('MCQ', 'ESSAY', 'CODING', 'TRUE_FALSE', 'FILL_BLANK', 'MATCHING')),
    answer_data JSONB NOT NULL, -- Dữ liệu câu trả lời (format khác nhau theo loại)
    
    -- Chấm điểm
    is_correct BOOLEAN,
    points_earned DECIMAL(10,2) DEFAULT 0,
    auto_graded BOOLEAN DEFAULT false,
    
    -- Thời gian
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    time_spent_seconds INTEGER DEFAULT 0,
    
    -- AI grading (mở rộng tương lai)
    ai_feedback TEXT,
    ai_confidence_score DECIMAL(3,2), -- 0.00 - 1.00
    
    -- Manual grading
    manual_feedback TEXT,
    graded_by UUID REFERENCES users(id),
    graded_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint
    UNIQUE(attempt_id, question_id)
);

-- =====================================================
-- 5. GRADING AND FEEDBACK
-- =====================================================

-- Bảng rubric chấm điểm (cho câu tự luận, coding)
CREATE TABLE exam_grading_rubrics (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES exam_questions(id) ON DELETE CASCADE,
    
    -- Tiêu chí chấm điểm
    criteria_name VARCHAR(200) NOT NULL,
    criteria_description TEXT,
    max_points DECIMAL(10,2) NOT NULL CHECK (max_points > 0),
    
    -- Thang điểm chi tiết
    grading_levels JSONB NOT NULL, -- Array of {level, points, description}
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Bảng chi tiết chấm điểm theo rubric
CREATE TABLE exam_answer_rubric_scores (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    answer_id UUID NOT NULL REFERENCES exam_answers(id) ON DELETE CASCADE,
    rubric_id UUID NOT NULL REFERENCES exam_grading_rubrics(id) ON DELETE CASCADE,
    
    -- Điểm số
    points_awarded DECIMAL(10,2) NOT NULL DEFAULT 0,
    feedback TEXT,
    
    -- Người chấm
    graded_by UUID REFERENCES users(id),
    graded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Unique constraint
    UNIQUE(answer_id, rubric_id)
);

-- =====================================================
-- 6. ANALYTICS AND REPORTING
-- =====================================================

-- Bảng thống kê kỳ thi
CREATE TABLE exam_analytics (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    
    -- Thống kê tổng quan
    total_students INTEGER DEFAULT 0,
    completed_attempts INTEGER DEFAULT 0,
    average_score DECIMAL(5,2) DEFAULT 0,
    highest_score DECIMAL(10,2) DEFAULT 0,
    lowest_score DECIMAL(10,2) DEFAULT 0,
    pass_rate DECIMAL(5,2) DEFAULT 0,
    
    -- Thống kê thời gian
    average_completion_time INTEGER DEFAULT 0, -- seconds
    
    -- Thống kê câu hỏi
    question_analytics JSONB DEFAULT '{}', -- Per-question statistics
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit fields
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint
    UNIQUE(exam_id)
);

-- =====================================================
-- 7. SYSTEM CONFIGURATION
-- =====================================================

-- Bảng cấu hình hệ thống thi
CREATE TABLE exam_system_settings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value JSONB NOT NULL,
    description TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- 8. INDEXES FOR PERFORMANCE
-- =====================================================

-- Indexes cho exams
CREATE INDEX idx_exams_class_id ON exams(class_id);
CREATE INDEX idx_exams_status ON exams(status);
CREATE INDEX idx_exams_start_time ON exams(start_time);
CREATE INDEX idx_exams_created_by ON exams(created_by);

-- Indexes cho exam_questions
CREATE INDEX idx_exam_questions_exam_id ON exam_questions(exam_id);
CREATE INDEX idx_exam_questions_type ON exam_questions(question_type);
CREATE INDEX idx_exam_questions_order ON exam_questions(exam_id, order_index);

-- Indexes cho exam_question_options
CREATE INDEX idx_exam_question_options_question_id ON exam_question_options(question_id);
CREATE INDEX idx_exam_question_options_order ON exam_question_options(question_id, order_index);

-- Indexes cho exam_attempts
CREATE INDEX idx_exam_attempts_exam_id ON exam_attempts(exam_id);
CREATE INDEX idx_exam_attempts_student_id ON exam_attempts(student_id);
CREATE INDEX idx_exam_attempts_status ON exam_attempts(status);
CREATE INDEX idx_exam_attempts_started_at ON exam_attempts(started_at);
CREATE INDEX idx_exam_attempts_exam_student ON exam_attempts(exam_id, student_id);

-- Indexes cho exam_answers
CREATE INDEX idx_exam_answers_attempt_id ON exam_answers(attempt_id);
CREATE INDEX idx_exam_answers_question_id ON exam_answers(question_id);
CREATE INDEX idx_exam_answers_type ON exam_answers(answer_type);
CREATE INDEX idx_exam_answers_graded_by ON exam_answers(graded_by);

-- Indexes cho grading
CREATE INDEX idx_exam_grading_rubrics_question_id ON exam_grading_rubrics(question_id);
CREATE INDEX idx_exam_answer_rubric_scores_answer_id ON exam_answer_rubric_scores(answer_id);
CREATE INDEX idx_exam_answer_rubric_scores_rubric_id ON exam_answer_rubric_scores(rubric_id);

-- Indexes cho analytics
CREATE INDEX idx_exam_analytics_exam_id ON exam_analytics(exam_id);

-- =====================================================
-- 9. COMMENTS FOR DOCUMENTATION
-- =====================================================

-- Table comments
COMMENT ON TABLE exams IS 'Bảng quản lý kỳ thi - liên kết với lớp học phần';
COMMENT ON TABLE exam_questions IS 'Bảng câu hỏi đa hình - hỗ trợ MCQ, Essay, Coding, etc.';
COMMENT ON TABLE exam_question_options IS 'Bảng lựa chọn cho câu hỏi trắc nghiệm';
COMMENT ON TABLE exam_attempts IS 'Bảng lượt thi của sinh viên - lưu snapshot câu hỏi';
COMMENT ON TABLE exam_answers IS 'Bảng câu trả lời đa hình - hỗ trợ nhiều loại câu hỏi';
COMMENT ON TABLE exam_grading_rubrics IS 'Bảng rubric chấm điểm cho câu tự luận/coding';
COMMENT ON TABLE exam_answer_rubric_scores IS 'Bảng điểm chi tiết theo rubric';
COMMENT ON TABLE exam_analytics IS 'Bảng thống kê kết quả thi';
COMMENT ON TABLE exam_system_settings IS 'Bảng cấu hình hệ thống thi';

-- Column comments for key fields
COMMENT ON COLUMN exam_attempts.exam_snapshot IS 'Snapshot cấu hình kỳ thi tại thời điểm sinh viên bắt đầu thi';
COMMENT ON COLUMN exam_attempts.questions_snapshot IS 'Snapshot câu hỏi và đáp án tại thời điểm thi';
COMMENT ON COLUMN exam_questions.question_config IS 'Cấu hình riêng cho từng loại câu hỏi (JSON)';
COMMENT ON COLUMN exam_questions.correct_answer IS 'Đáp án đúng (format khác nhau theo loại câu hỏi)';
COMMENT ON COLUMN exam_answers.answer_data IS 'Dữ liệu câu trả lời (format khác nhau theo loại câu hỏi)';
COMMENT ON COLUMN exam_grading_rubrics.grading_levels IS 'Thang điểm chi tiết cho rubric (JSON array)';

-- =====================================================
-- 10. SAMPLE CONFIGURATION DATA
-- =====================================================

-- Cấu hình mặc định cho hệ thống
INSERT INTO exam_system_settings (setting_key, setting_value, description) VALUES
('default_exam_duration', '120', 'Thời gian thi mặc định (phút)'),
('max_attempts_per_exam', '3', 'Số lần thi tối đa cho mỗi kỳ thi'),
('auto_submit_buffer', '300', 'Thời gian buffer trước khi tự động nộp bài (giây)'),
('enable_anti_cheat', 'false', 'Bật tính năng chống gian lận'),
('proctoring_providers', '["manual", "ai_proctoring"]', 'Danh sách nhà cung cấp giám sát'),
('supported_question_types', '["MCQ", "ESSAY", "CODING", "TRUE_FALSE", "FILL_BLANK"]', 'Các loại câu hỏi được hỗ trợ'),
('grading_modes', '["auto", "manual", "ai_assisted"]', 'Các chế độ chấm điểm'),
('coding_languages', '["java", "python", "javascript", "cpp", "csharp"]', 'Ngôn ngữ lập trình hỗ trợ cho câu hỏi coding');

-- =====================================================
-- END OF SCHEMA
-- =====================================================