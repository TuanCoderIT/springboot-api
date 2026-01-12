-- Timeline Events Table
-- Lưu các sự kiện trong timeline được AI sinh ra

CREATE TABLE IF NOT EXISTS public.timeline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notebook_id UUID NOT NULL REFERENCES public.notebooks(id) ON DELETE CASCADE,
    notebook_ai_sets_id UUID REFERENCES public.notebook_ai_sets(id) ON DELETE SET NULL,
    created_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    
    -- Event data
    event_order INTEGER NOT NULL,
    date VARCHAR(50),                        -- "1945" hoặc "1945-08-19"
    date_precision VARCHAR(20) DEFAULT 'unknown', -- year, month, day, unknown
    title VARCHAR(500) NOT NULL,
    description TEXT,
    importance VARCHAR(20) DEFAULT 'normal', -- minor, normal, major, critical
    icon VARCHAR(50),                        -- history, network, milestone, etc.
    
    -- Metadata
    extra_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_importance CHECK (importance IN ('minor', 'normal', 'major', 'critical')),
    CONSTRAINT chk_date_precision CHECK (date_precision IN ('year', 'month', 'day', 'unknown'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_timeline_events_notebook ON public.timeline_events(notebook_id);
CREATE INDEX IF NOT EXISTS idx_timeline_events_ai_set ON public.timeline_events(notebook_ai_sets_id);
CREATE INDEX IF NOT EXISTS idx_timeline_events_order ON public.timeline_events(notebook_ai_sets_id, event_order);

-- Comments
COMMENT ON TABLE public.timeline_events IS 'Lưu các sự kiện trong timeline AI-generated';
COMMENT ON COLUMN public.timeline_events.event_order IS 'Thứ tự sự kiện trong timeline';
COMMENT ON COLUMN public.timeline_events.date_precision IS 'Độ chính xác của date: year, month, day, unknown';
COMMENT ON COLUMN public.timeline_events.importance IS 'Mức độ quan trọng: minor, normal, major, critical';
