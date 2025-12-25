--
-- PostgreSQL database dump
--
-- Dumped from database version 18.1 (Postgres.app)
-- Dumped by pg_dump version 18.1 (Postgres.app)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';

--
-- Name: rag_search_chunks(uuid, public.vector, uuid[]); Type: FUNCTION; Schema: public; Owner: admin
--

CREATE FUNCTION public.rag_search_chunks(p_notebook_id uuid, p_query_embedding public.vector, p_file_ids uuid[]) RETURNS TABLE(file_id uuid, chunk_index integer, content text, similarity double precision, distance double precision)
    LANGUAGE sql
    AS $$
    SELECT 
        ranked.file_id,
        ranked.chunk_index,
        ranked.content,
        ranked.similarity,
        ranked.distance
    FROM (
        SELECT 
            fc.file_id,
            fc.chunk_index,
            fc.content,
            1 - (fc.embedding <=> p_query_embedding) AS similarity,
            (fc.embedding <=> p_query_embedding) AS distance
        FROM public.file_chunks fc
        WHERE fc.notebook_id = p_notebook_id
          AND fc.file_id = ANY(p_file_ids)
    ) AS ranked
    WHERE ranked.similarity >= 0.7          -- hoặc 0.80 nếu bạn muốn >80%
    ORDER BY ranked.similarity DESC          -- sắp xếp đúng theo độ tương đồng
    LIMIT 2;                               -- top 100 chuẩn thứ tự
$$;


ALTER FUNCTION public.rag_search_chunks(p_notebook_id uuid, p_query_embedding public.vector, p_file_ids uuid[]) OWNER TO admin;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: chapter_items; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.chapter_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    chapter_id uuid NOT NULL,
    item_type text NOT NULL,
    ref_id uuid,
    title text,
    sort_order integer DEFAULT 0 NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    visible_in_lesson boolean DEFAULT true NOT NULL,
    visible_in_notebook boolean DEFAULT true NOT NULL
);


ALTER TABLE public.chapter_items OWNER TO admin;

--
-- Name: COLUMN chapter_items.visible_in_lesson; Type: COMMENT; Schema: public; Owner: admin
--

COMMENT ON COLUMN public.chapter_items.visible_in_lesson IS 'Hiển thị item trong bài học cho sinh viên';

--
-- Name: COLUMN chapter_items.visible_in_notebook; Type: COMMENT; Schema: public; Owner: admin
--

COMMENT ON COLUMN public.chapter_items.visible_in_notebook IS 'Hiển thị item trong notebook khi ôn tập';

--
-- Name: class_members; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.class_members (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    class_id uuid NOT NULL,
    student_code text NOT NULL,
    first_name text,
    last_name text,
    full_name text NOT NULL,
    dob date,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.class_members OWNER TO admin;

--
-- Name: classes; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.classes (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    teaching_assignment_id uuid NOT NULL,
    class_code text NOT NULL,
    subject_code text NOT NULL,
    subject_name text NOT NULL,
    room text,
    day_of_week integer,
    periods text,
    start_date date,
    end_date date,
    note text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.classes OWNER TO admin;

--
-- Name: file_chunks; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.file_chunks (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    file_id uuid NOT NULL,
    chunk_index integer NOT NULL,
    content text NOT NULL,
    embedding public.vector(1536) NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.file_chunks OWNER TO admin;

--
-- Name: flashcards; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.flashcards (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    created_by uuid,
    front_text text NOT NULL,
    back_text text NOT NULL,
    extra_metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    notebook_ai_sets_id uuid,
    hint text,
    example text,
    image_url text,
    audio_url text
);

ALTER TABLE public.flashcards OWNER TO admin;

--
-- Name: llm_models; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.llm_models (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    provider text NOT NULL,
    display_name text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.llm_models OWNER TO admin;

--
-- Name: major_subjects; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.major_subjects (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    major_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    knowledge_block text,
    term_no integer,
    is_required boolean DEFAULT true NOT NULL,
    period_split text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.major_subjects OWNER TO admin;

--
-- Name: majors; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.majors (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    org_unit_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.majors OWNER TO admin;

--
-- Name: message_reactions; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.message_reactions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    message_id uuid NOT NULL,
    user_id uuid NOT NULL,
    emoji character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.message_reactions OWNER TO admin;

--
-- Name: notebook_activity_logs; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_activity_logs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    user_id uuid,
    action character varying(64) NOT NULL,
    target_id uuid,
    target_type character varying(64),
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_activity_logs OWNER TO admin;

--
-- Name: notebook_ai_set_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_ai_set_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    ai_set_id uuid NOT NULL,
    file_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_ai_set_files OWNER TO admin;

--
-- Name: notebook_ai_set_suggestions; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_ai_set_suggestions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_ai_set_id uuid NOT NULL,
    suggestions jsonb NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_ai_set_suggestions OWNER TO admin;

--
-- Name: notebook_ai_sets; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_ai_sets (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    created_by uuid,
    set_type text NOT NULL,
    status text DEFAULT 'queued'::text NOT NULL,
    error_message text,
    model_code text,
    provider text,
    llm_model_id uuid,
    title text,
    description text,
    input_config jsonb,
    output_stats jsonb,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    updated_at timestamp with time zone,
    CONSTRAINT notebook_ai_sets_set_type_check CHECK ((set_type = ANY (ARRAY['quiz'::text, 'summary'::text, 'flashcard'::text, 'tts'::text, 'mindmap'::text, 'suggestion'::text, 'video'::text]))),
    CONSTRAINT notebook_ai_sets_status_check CHECK ((status = ANY (ARRAY['queued'::text, 'processing'::text, 'done'::text, 'failed'::text, 'canceled'::text])))
);


ALTER TABLE public.notebook_ai_sets OWNER TO admin;

--
-- Name: notebook_ai_summaries; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_ai_summaries (
    id uuid NOT NULL,
    content_md text NOT NULL,
    script_tts text,
    language text DEFAULT 'vi'::text NOT NULL,
    audio_url text,
    audio_format text,
    audio_duration_ms integer,
    tts_provider text,
    tts_model text,
    voice_id text,
    voice_label text,
    voice_speed real,
    voice_pitch real,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    create_by uuid
);


ALTER TABLE public.notebook_ai_summaries OWNER TO admin;

--
-- Name: notebook_bot_conversation_states; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_bot_conversation_states (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    notebook_id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    last_opened_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata jsonb
);


ALTER TABLE public.notebook_bot_conversation_states OWNER TO admin;

--
-- Name: notebook_bot_conversations; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_bot_conversations (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    created_by uuid,
    title text,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.notebook_bot_conversations OWNER TO admin;

--
-- Name: notebook_bot_message_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_bot_message_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    message_id uuid NOT NULL,
    file_type text,
    file_url text,
    mime_type text,
    file_name text,
    ocr_text text,
    caption text,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_bot_message_files OWNER TO admin;

--
-- Name: notebook_bot_message_sources; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_bot_message_sources (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    message_id uuid NOT NULL,
    source_type text NOT NULL,
    file_id uuid,
    chunk_index integer,
    title text,
    url text,
    snippet text,
    provider text,
    web_index integer,
    score double precision,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notebook_bot_message_sources_source_type_check CHECK ((source_type = ANY (ARRAY['RAG'::text, 'WEB'::text])))
);


ALTER TABLE public.notebook_bot_message_sources OWNER TO admin;

--
-- Name: notebook_bot_messages; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_bot_messages (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    user_id uuid,
    role character varying(16) NOT NULL,
    content text NOT NULL,
    mode character varying(16),
    context jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata jsonb,
    llm_model_id uuid,
    CONSTRAINT notebook_bot_messages_mode_check CHECK (((mode)::text = ANY ((ARRAY['RAG'::character varying, 'WEB'::character varying, 'HYBRID'::character varying, 'LLM_ONLY'::character varying, 'AUTO'::character varying])::text[]))),
    CONSTRAINT notebook_bot_messages_role_check CHECK (((role)::text = ANY ((ARRAY['user'::character varying, 'assistant'::character varying, 'system'::character varying])::text[])))
);


ALTER TABLE public.notebook_bot_messages OWNER TO admin;

--
-- Name: notebook_chapters; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_chapters (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    title text NOT NULL,
    description text,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_chapters OWNER TO admin;

--
-- Name: notebook_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    uploaded_by uuid NOT NULL,
    original_filename text NOT NULL,
    mime_type character varying(255),
    file_size bigint,
    storage_url text NOT NULL,
    status character varying(50) NOT NULL,
    pages_count integer,
    ocr_done boolean DEFAULT false NOT NULL,
    embedding_done boolean DEFAULT false NOT NULL,
    extra_metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    chunk_size integer DEFAULT 800,
    chunk_overlap integer DEFAULT 120,
    CONSTRAINT chk_notebook_file_status CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'processing'::character varying, 'done'::character varying, 'failed'::character varying, 'rejected'::character varying])::text[])))
);


ALTER TABLE public.notebook_files OWNER TO admin;

--
-- Name: notebook_members; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_members (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    joined_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_notebook_member_role CHECK (((role)::text = ANY ((ARRAY['owner'::character varying, 'admin'::character varying, 'member'::character varying])::text[]))),
    CONSTRAINT chk_notebook_member_status CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying, 'blocked'::character varying])::text[])))
);


ALTER TABLE public.notebook_members OWNER TO admin;

--
-- Name: notebook_messages; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_messages (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    user_id uuid,
    type character varying(50) NOT NULL,
    content text NOT NULL,
    reply_to_message_id uuid,
    ai_context jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_notebook_message_type CHECK (((type)::text = ANY ((ARRAY['user'::character varying, 'system'::character varying, 'ai'::character varying])::text[])))
);


ALTER TABLE public.notebook_messages OWNER TO admin;

--
-- Name: notebook_mindmaps; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_mindmaps (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    title text NOT NULL,
    mindmap jsonb NOT NULL,
    layout jsonb,
    source_ai_set_id uuid,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_mindmaps OWNER TO admin;

--
-- Name: notebook_quiz_options; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_quiz_options (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    quiz_id uuid NOT NULL,
    text text NOT NULL,
    is_correct boolean DEFAULT false NOT NULL,
    feedback text,
    "position" integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notebook_quiz_options OWNER TO admin;

--
-- Name: notebook_quizzes; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebook_quizzes (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    question text NOT NULL,
    explanation text,
    difficulty_level smallint,
    created_by uuid,
    embedding public.vector(1536),
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    notebook_ai_sets_id uuid
);


ALTER TABLE public.notebook_quizzes OWNER TO admin;

--
-- Name: notebooks; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notebooks (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    type character varying(50) NOT NULL,
    visibility character varying(50) NOT NULL,
    created_by uuid NOT NULL,
    thumbnail_url text,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_notebook_type CHECK (((type)::text = ANY (ARRAY['community'::text, 'private_group'::text, 'personal'::text, 'assignment'::text, 'regulation'::text]))),
    CONSTRAINT chk_notebook_visibility CHECK (((visibility)::text = ANY ((ARRAY['public'::character varying, 'private'::character varying])::text[])))
);


ALTER TABLE public.notebooks OWNER TO admin;

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.notifications (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    title text NOT NULL,
    content text,
    url text,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    action character varying(50) DEFAULT NULL::character varying,
    role_target text[] DEFAULT '{}'::text[],
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notifications OWNER TO admin;

--
-- Name: org_units; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.org_units (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    type text,
    parent_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.org_units OWNER TO admin;

--
-- Name: regulation_chat_analytics; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.regulation_chat_analytics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    notebook_id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    message_id uuid,
    user_id uuid NOT NULL,
    mode character varying(20) DEFAULT 'RAG'::character varying NOT NULL,
    status character varying(30) DEFAULT 'OK'::character varying NOT NULL,
    rating smallint,
    feedback_type character varying(20),
    feedback_text text,
    latency_ms integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    query_text text,
    query_hash character(64),
    query_language character varying(5),
    avg_source_score numeric(6,5),
    CONSTRAINT chk_avg_source_score CHECK (((avg_source_score IS NULL) OR ((avg_source_score >= (0)::numeric) AND (avg_source_score <= (1)::numeric)))),
    CONSTRAINT chk_feedback_type CHECK (((feedback_type IS NULL) OR ((feedback_type)::text = ANY ((ARRAY['LIKE'::character varying, 'DISLIKE'::character varying, 'REPORT'::character varying, 'NONE'::character varying])::text[])))),
    CONSTRAINT chk_latency_ms CHECK (((latency_ms IS NULL) OR (latency_ms >= 0))),
    CONSTRAINT chk_mode CHECK (((mode)::text = ANY ((ARRAY['RAG'::character varying, 'WEB'::character varying, 'LLM_ONLY'::character varying, 'HYBRID'::character varying, 'AUTO'::character varying])::text[]))),
    CONSTRAINT chk_rating CHECK (((rating IS NULL) OR ((rating >= 1) AND (rating <= 5)))),
    CONSTRAINT chk_status CHECK (((status)::text = ANY ((ARRAY['OK'::character varying, 'ERROR'::character varying, 'NO_CONTEXT'::character varying, 'TIMEOUT'::character varying, 'BLOCKED'::character varying])::text[])))
);


ALTER TABLE public.regulation_chat_analytics OWNER TO admin;

--
-- Name: subjects; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.subjects (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    credit integer,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.subjects OWNER TO admin;

--
-- Name: teaching_assignments; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.teaching_assignments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    term_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    status text DEFAULT 'ACTIVE'::text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by text DEFAULT 'ADMIN'::text NOT NULL,
    approval_status text DEFAULT 'APPROVED'::text NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    note text,
    lecturer_id uuid,
    notebook_id uuid
);


ALTER TABLE public.teaching_assignments OWNER TO admin;

--
-- Name: terms; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.terms (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    start_date date,
    end_date date,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.terms OWNER TO admin;

--
-- Name: tts_assets; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.tts_assets (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    created_by uuid,
    language character varying(16),
    voice_name character varying(64),
    text_source text,
    audio_url text NOT NULL,
    duration_seconds integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    notebook_ai_sets uuid
);


ALTER TABLE public.tts_assets OWNER TO admin;

--
-- Name: tts_voices; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.tts_voices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    voice_id character varying(100) NOT NULL,
    voice_name character varying(100) NOT NULL,
    description text,
    provider character varying(50) DEFAULT 'gemini'::character varying NOT NULL,
    gender character varying(20),
    language character varying(10) DEFAULT 'en'::character varying,
    accent character varying(50),
    style character varying(50),
    age_group character varying(20),
    use_case character varying(100),
    sample_audio_url character varying(500),
    sample_text text,
    sample_duration_ms integer,
    default_speed double precision DEFAULT 1.0,
    default_pitch double precision DEFAULT 0.0,
    is_active boolean DEFAULT true,
    is_premium boolean DEFAULT false,
    sort_order integer DEFAULT 0,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.tts_voices OWNER TO admin;

--
-- Name: TABLE tts_voices; Type: COMMENT; Schema: public; Owner: admin
--

COMMENT ON TABLE public.tts_voices IS 'Danh sách voice TTS từ các provider (Gemini, ElevenLabs, OpenAI, etc.)';


--
-- Name: users; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    full_name character varying(255),
    role character varying(50) NOT NULL,
    avatar_url text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    avatar character varying,
    student_code character varying(50),
    cohort_year integer,
    program character varying(255),
    class_code character varying(50),
    major_id uuid,
    lecturer_code character varying(50),
    primary_org_unit_id uuid,
    academic_degree character varying(255),
    academic_rank character varying(255),
    specialization character varying(255),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'TEACHER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO admin;

--
-- Name: video_assets; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.video_assets (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    created_by uuid,
    language character varying(16),
    style character varying(64),
    text_source text,
    video_url text NOT NULL,
    duration_seconds integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    notebook_ai_sets_id uuid
);


ALTER TABLE public.video_assets OWNER TO admin;