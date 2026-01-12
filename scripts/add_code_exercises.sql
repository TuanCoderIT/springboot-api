-- =====================================================
-- CODE EXERCISES TABLES
-- =====================================================

-- 1. Supported Languages (sync từ Piston)
CREATE TABLE IF NOT EXISTS public.supported_languages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    version VARCHAR(20),
    aliases TEXT[],
    runtime VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    last_sync TIMESTAMPTZ DEFAULT now(),
    
    UNIQUE(name, version)
);

COMMENT ON TABLE public.supported_languages IS 'Danh sách ngôn ngữ lập trình được Piston hỗ trợ';

-- 2. Code Exercises (bài tập chính)
CREATE TABLE IF NOT EXISTS public.code_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notebook_id UUID NOT NULL REFERENCES public.notebooks(id) ON DELETE CASCADE,
    notebook_ai_set_id UUID NOT NULL REFERENCES public.notebook_ai_sets(id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES public.supported_languages(id),
    
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty VARCHAR(10) DEFAULT 'medium' CHECK (difficulty IN ('easy', 'medium', 'hard')),
    
    time_limit INTEGER DEFAULT 2,              -- seconds
    memory_limit BIGINT DEFAULT 256000000,     -- bytes (256MB)
    
    order_index INTEGER DEFAULT 0,
    created_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_exercises_notebook ON public.code_exercises(notebook_id);
CREATE INDEX IF NOT EXISTS idx_code_exercises_ai_set ON public.code_exercises(notebook_ai_set_id);

COMMENT ON TABLE public.code_exercises IS 'Bài tập lập trình được AI sinh ra';

-- 3. Code Exercise Files (starter, solution, user code)
CREATE TABLE IF NOT EXISTS public.code_exercise_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES public.code_exercises(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,  -- NULL nếu starter/solution
    
    filename VARCHAR(255) NOT NULL,
    content TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'starter' CHECK (role IN ('starter', 'solution', 'user')),
    is_main BOOLEAN DEFAULT false,
    is_pass BOOLEAN DEFAULT false,  -- true nếu user đã pass bài này
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_exercise_files_exercise ON public.code_exercise_files(exercise_id);
CREATE INDEX IF NOT EXISTS idx_code_exercise_files_user ON public.code_exercise_files(user_id);

COMMENT ON TABLE public.code_exercise_files IS 'Files code: starter (khởi tạo), solution (đáp án), user (code của user)';

-- 4. Code Exercise Test Cases
CREATE TABLE IF NOT EXISTS public.code_exercise_testcases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES public.code_exercises(id) ON DELETE CASCADE,
    
    input TEXT,
    expected_output TEXT,
    is_sample BOOLEAN DEFAULT false,  -- false=test mẫu (hiển thị), true=test ẩn
    order_index INTEGER DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_exercise_testcases_exercise ON public.code_exercise_testcases(exercise_id);

COMMENT ON TABLE public.code_exercise_testcases IS 'Test cases cho bài tập code';
COMMENT ON COLUMN public.code_exercise_testcases.is_sample IS 'false=test mẫu (hiển thị cho user), true=test ẩn';

-- 5. Update constraint cho notebook_ai_sets để thêm code_exercise type
ALTER TABLE public.notebook_ai_sets 
DROP CONSTRAINT IF EXISTS notebook_ai_sets_set_type_check;

ALTER TABLE public.notebook_ai_sets 
ADD CONSTRAINT notebook_ai_sets_set_type_check 
CHECK (set_type IN ('quiz', 'flashcard', 'summary', 'audio', 'mindmap', 'timeline', 'code_exercise'));
