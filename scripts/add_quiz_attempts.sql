-- Quiz Attempts - Lưu lịch sử làm quiz
CREATE TABLE IF NOT EXISTS public.quiz_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    notebook_ai_set_id UUID NOT NULL REFERENCES public.notebook_ai_sets(id) ON DELETE CASCADE,
    
    -- Score
    score INTEGER,
    total_questions INTEGER,
    correct_count INTEGER,
    
    -- Time
    time_spent_seconds INTEGER,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    
    -- AI Analysis
    analysis_json JSONB,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Quiz Attempt Answers - Chi tiết từng câu trả lời
CREATE TABLE IF NOT EXISTS public.quiz_attempt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES public.quiz_attempts(id) ON DELETE CASCADE,
    quiz_id UUID NOT NULL REFERENCES public.notebook_quizzes(id) ON DELETE CASCADE,
    selected_option_id UUID REFERENCES public.notebook_quiz_options(id) ON DELETE SET NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user ON public.quiz_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_ai_set ON public.quiz_attempts(notebook_ai_set_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempt_answers_attempt ON public.quiz_attempt_answers(attempt_id);

-- Comments
COMMENT ON TABLE public.quiz_attempts IS 'Lưu lịch sử làm quiz của user';
COMMENT ON TABLE public.quiz_attempt_answers IS 'Chi tiết từng câu trả lời trong một lần làm quiz';
COMMENT ON COLUMN public.quiz_attempts.analysis_json IS 'AI analysis: strengths, weaknesses, recommendations';
