--
-- PostgreSQL database dump
--

\restrict dQATifj5thH8esgbfqdUnH0EtSReaMXRHhjW6htMS9WT8VPZhr2vpH7vNJx3ODL

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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_tasks; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.ai_tasks (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    file_id uuid,
    user_id uuid,
    task_type character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    input_config jsonb,
    output_data jsonb,
    error_message text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_ai_task_status CHECK (((status)::text = ANY ((ARRAY['queued'::character varying, 'processing'::character varying, 'done'::character varying, 'failed'::character varying, 'canceled'::character varying])::text[]))),
    CONSTRAINT chk_ai_task_type CHECK (((task_type)::text = ANY ((ARRAY['summary'::character varying, 'flashcards'::character varying, 'quiz'::character varying, 'tts'::character varying, 'video'::character varying, 'other'::character varying])::text[])))
);


ALTER TABLE public.ai_tasks OWNER TO admin;

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
-- Name: flashcard_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.flashcard_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    flashcard_id uuid NOT NULL,
    file_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.flashcard_files OWNER TO admin;

--
-- Name: flashcard_reviews; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.flashcard_reviews (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    flashcard_id uuid NOT NULL,
    user_id uuid NOT NULL,
    ease_factor double precision,
    interval_days integer,
    quality integer,
    review_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.flashcard_reviews OWNER TO admin;

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
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.flashcards OWNER TO admin;

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
    CONSTRAINT chk_notebook_file_status CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying, 'processing'::character varying, 'failed'::character varying])::text[])))
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
    CONSTRAINT chk_notebook_type CHECK (((type)::text = ANY ((ARRAY['community'::character varying, 'private_group'::character varying, 'personal'::character varying])::text[]))),
    CONSTRAINT chk_notebook_visibility CHECK (((visibility)::text = ANY ((ARRAY['public'::character varying, 'private'::character varying])::text[])))
);


ALTER TABLE public.notebooks OWNER TO admin;

--
-- Name: quiz_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quiz_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    quiz_id uuid NOT NULL,
    file_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.quiz_files OWNER TO admin;

--
-- Name: quiz_options; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quiz_options (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    question_id uuid NOT NULL,
    option_text text NOT NULL,
    is_correct boolean DEFAULT false NOT NULL
);


ALTER TABLE public.quiz_options OWNER TO admin;

--
-- Name: quiz_questions; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quiz_questions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    quiz_id uuid NOT NULL,
    question_text text NOT NULL,
    question_type character varying(32) DEFAULT 'multiple_choice'::character varying NOT NULL,
    metadata jsonb
);


ALTER TABLE public.quiz_questions OWNER TO admin;

--
-- Name: quiz_submissions; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quiz_submissions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    quiz_id uuid NOT NULL,
    user_id uuid NOT NULL,
    score double precision,
    answers jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.quiz_submissions OWNER TO admin;

--
-- Name: quizzes; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quizzes (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    created_by uuid,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.quizzes OWNER TO admin;

--
-- Name: rag_queries; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.rag_queries (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    notebook_id uuid NOT NULL,
    user_id uuid,
    question text NOT NULL,
    answer text,
    source_chunks jsonb,
    latency_ms integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.rag_queries OWNER TO admin;

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
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.tts_assets OWNER TO admin;

--
-- Name: tts_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.tts_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    tts_id uuid NOT NULL,
    file_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.tts_files OWNER TO admin;

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
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'TEACHER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO admin;

--
-- Name: video_asset_files; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.video_asset_files (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    video_asset_id uuid NOT NULL,
    file_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.video_asset_files OWNER TO admin;

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
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.video_assets OWNER TO admin;

--
-- Data for Name: ai_tasks; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.ai_tasks (id, notebook_id, file_id, user_id, task_type, status, input_config, output_data, error_message, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: file_chunks; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.file_chunks (id, notebook_id, file_id, chunk_index, content, embedding, metadata, created_at) FROM stdin;
\.


--
-- Data for Name: flashcard_files; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.flashcard_files (id, flashcard_id, file_id, created_at) FROM stdin;
\.


--
-- Data for Name: flashcard_reviews; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.flashcard_reviews (id, flashcard_id, user_id, ease_factor, interval_days, quality, review_at) FROM stdin;
\.


--
-- Data for Name: flashcards; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.flashcards (id, notebook_id, created_by, front_text, back_text, extra_metadata, created_at) FROM stdin;
\.


--
-- Data for Name: message_reactions; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.message_reactions (id, message_id, user_id, emoji, created_at) FROM stdin;
\.


--
-- Data for Name: notebook_activity_logs; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.notebook_activity_logs (id, notebook_id, user_id, action, target_id, target_type, metadata, created_at) FROM stdin;
\.


--
-- Data for Name: notebook_files; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.notebook_files (id, notebook_id, uploaded_by, original_filename, mime_type, file_size, storage_url, status, pages_count, ocr_done, embedding_done, extra_metadata, created_at, updated_at, chunk_size, chunk_overlap) FROM stdin;
683e3fac-f458-4958-87c5-c08fb26da89c	c3a7f558-faa7-4218-ae41-4ef57f976f34	991c40a1-c2b1-4e62-972a-33deafd708ff	1761191230157-8g0t85ryb2n.pdf	application/pdf	308489	/uploads/9ceb7a81-0ef0-442b-b673-6a10cf70ee86.pdf	failed	\N	f	f	\N	2025-12-01 19:58:39.318307+07	2025-12-01 19:58:39.791116+07	800	120
\.


--
-- Data for Name: notebook_members; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.notebook_members (id, notebook_id, user_id, role, status, joined_at, created_at, updated_at) FROM stdin;
3538fd10-025c-4cbe-a82a-084ae881177d	95f69db9-e3e4-45d9-83ed-fe8d0cda70ba	991c40a1-c2b1-4e62-972a-33deafd708ff	owner	approved	2025-11-30 18:39:40.283697+07	2025-11-30 18:39:40.28377+07	2025-11-30 18:39:40.283774+07
d6f3d2c6-4ba4-44f7-ac4d-c5c53e46494a	51eb25f7-1cc3-481a-8380-429abaaa5493	991c40a1-c2b1-4e62-972a-33deafd708ff	owner	approved	2025-11-30 18:39:56.068563+07	2025-11-30 18:39:56.068573+07	2025-11-30 18:39:56.068575+07
4de3ec6f-143c-49fc-b3ca-bc5184730c7b	a19e6b78-b1b0-4c51-83e3-5359ca09996d	991c40a1-c2b1-4e62-972a-33deafd708ff	owner	approved	2025-11-30 18:40:41.473548+07	2025-11-30 18:40:41.473761+07	2025-11-30 18:40:41.47377+07
6e74945d-2ade-4d51-a744-c2732afc15e0	c3a7f558-faa7-4218-ae41-4ef57f976f34	991c40a1-c2b1-4e62-972a-33deafd708ff	owner	approved	2025-11-30 18:25:08.887545+07	2025-11-30 18:25:08.887621+07	2025-11-30 18:25:08.887632+07
792828ef-0375-46f6-9da1-5508c6e9af55	74c7507d-f09c-4bfe-9ce0-0ec21b5c6847	991c40a1-c2b1-4e62-972a-33deafd708ff	owner	approved	2025-11-30 18:27:05.585252+07	2025-11-30 18:27:05.585293+07	2025-11-30 18:27:05.585296+07
2b160321-ac5b-4cc0-8cf9-f282848d0a44	95f69db9-e3e4-45d9-83ed-fe8d0cda70ba	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-11-30 23:32:51.837619+07	2025-11-30 23:32:51.837636+07	2025-11-30 23:32:51.83764+07
d10c6b1a-d6f5-4dda-8049-b4cda0f0ebc0	51eb25f7-1cc3-481a-8380-429abaaa5493	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-11-30 23:32:54.498289+07	2025-11-30 23:32:54.498304+07	2025-11-30 23:32:54.498305+07
fe786224-f245-4129-8081-fa462114fedf	a19e6b78-b1b0-4c51-83e3-5359ca09996d	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-11-30 23:33:00.783148+07	2025-11-30 23:33:00.783189+07	2025-11-30 23:33:00.783192+07
00ce6c86-3bca-497f-92de-609509e95502	74c7507d-f09c-4bfe-9ce0-0ec21b5c6847	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-11-30 23:33:02.803579+07	2025-11-30 23:33:02.803609+07	2025-11-30 23:33:02.803612+07
da5c366f-063d-4669-8755-266a435cbb4c	c3a7f558-faa7-4218-ae41-4ef57f976f34	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-11-30 23:32:49.650775+07	2025-11-30 23:32:49.65079+07	2025-11-30 23:35:46.724885+07
b038cdf5-cfee-4bb5-b1ab-b75e22c167fe	d8cf4170-01b7-4cc4-8954-69bf82e33dbc	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-12-01 00:48:47.926467+07	2025-11-30 23:32:56.38183+07	2025-12-01 00:48:47.926483+07
e06bc3bd-92cf-464d-89ed-0a644394e5f2	19443f99-a3b9-42ff-95f6-af01c7206704	2b569515-99dd-48d1-a767-8f524a005338	member	approved	2025-12-01 01:04:26.137909+07	2025-11-30 23:32:58.919939+07	2025-12-01 01:04:26.137909+07
8a699f0b-7448-4d26-8723-98dfbb156431	d8cf4170-01b7-4cc4-8954-69bf82e33dbc	7710814e-22b0-40c0-8d32-5bf9f74a14de	member	approved	2025-12-01 01:04:33.719032+07	2025-11-30 21:27:07.869437+07	2025-12-01 01:04:33.719032+07
1bcbbd05-e25f-42e0-9ed5-fe6605a8d8b3	d8cf4170-01b7-4cc4-8954-69bf82e33dbc	991c40a1-c2b1-4e62-972a-33deafd708ff	member	approved	2025-12-01 10:44:07.224404+07	2025-12-01 10:43:42.684831+07	2025-12-01 10:44:07.224404+07
9416dd7b-21a7-42ad-b5bd-1518b85a9627	19443f99-a3b9-42ff-95f6-af01c7206704	991c40a1-c2b1-4e62-972a-33deafd708ff	member	approved	2025-12-01 10:44:07.224404+07	2025-12-01 10:43:44.930829+07	2025-12-01 10:44:07.224404+07
afd4e365-d0bc-42f2-91da-ff0a01beac21	c3a7f558-faa7-4218-ae41-4ef57f976f34	3442f856-80ee-4615-a569-2f1783a8ba7b	member	approved	2025-12-01 19:55:14.25871+07	2025-12-01 19:55:14.258775+07	2025-12-01 19:55:14.258778+07
\.


--
-- Data for Name: notebook_messages; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.notebook_messages (id, notebook_id, user_id, type, content, reply_to_message_id, ai_context, created_at) FROM stdin;
\.


--
-- Data for Name: notebooks; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.notebooks (id, title, description, type, visibility, created_by, thumbnail_url, metadata, created_at, updated_at) FROM stdin;
d8cf4170-01b7-4cc4-8954-69bf82e33dbc	ok gái xinh 123	huỳnh	community	private	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/b10bf94e-97ab-48c5-a731-7136124e9b55.jpeg	\N	2025-11-29 23:28:51.856012+07	2025-11-29 23:28:51.856012+07
51eb25f7-1cc3-481a-8380-429abaaa5493	huỳnh cuto	huỳnh cuto	community	public	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/d1fee20c-3994-4eb3-b708-50ac2f079931.png	\N	2025-11-29 23:46:19.666344+07	2025-11-29 23:46:19.666344+07
a19e6b78-b1b0-4c51-83e3-5359ca09996d	dsadas	đâs	community	public	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/12a6a4b9-edf5-49f9-b4e7-9fc89166bf13.jpg	\N	2025-11-29 23:55:43.791741+07	2025-11-29 23:55:43.791741+07
19443f99-a3b9-42ff-95f6-af01c7206704	fsdfsdfs	hgfds	community	private	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/f384df5d-250c-4308-8494-bfbae63777f4.jpeg	\N	2025-11-29 23:31:54.443811+07	2025-11-29 23:59:24.023826+07
95f69db9-e3e4-45d9-83ed-fe8d0cda70ba	dsds	dsds	community	public	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/cb331c02-cff5-4668-a9c0-40dc44313fb2.png	\N	2025-11-29 23:58:37.695738+07	2025-11-30 00:01:08.544101+07
74c7507d-f09c-4bfe-9ce0-0ec21b5c6847	fsdfsd	fsdfsd	community	public	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/fdd7fb73-6548-4971-8ea7-f0f4d3309d6f.jpg	\N	2025-11-30 00:04:02.112118+07	2025-11-30 00:04:02.112118+07
c3a7f558-faa7-4218-ae41-4ef57f976f34	dsds	dsds	community	public	991c40a1-c2b1-4e62-972a-33deafd708ff	/uploads/1e1b8d0a-3f9f-402d-9ee7-52ef62ded42c.jpeg	\N	2025-11-30 00:04:32.044568+07	2025-11-30 00:04:52.015776+07
\.


--
-- Data for Name: quiz_files; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quiz_files (id, quiz_id, file_id, created_at) FROM stdin;
\.


--
-- Data for Name: quiz_options; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quiz_options (id, question_id, option_text, is_correct) FROM stdin;
\.


--
-- Data for Name: quiz_questions; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quiz_questions (id, quiz_id, question_text, question_type, metadata) FROM stdin;
\.


--
-- Data for Name: quiz_submissions; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quiz_submissions (id, quiz_id, user_id, score, answers, created_at) FROM stdin;
\.


--
-- Data for Name: quizzes; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quizzes (id, notebook_id, title, created_by, metadata, created_at) FROM stdin;
\.


--
-- Data for Name: rag_queries; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.rag_queries (id, notebook_id, user_id, question, answer, source_chunks, latency_ms, created_at) FROM stdin;
\.


--
-- Data for Name: tts_assets; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.tts_assets (id, notebook_id, created_by, language, voice_name, text_source, audio_url, duration_seconds, created_at) FROM stdin;
\.


--
-- Data for Name: tts_files; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.tts_files (id, tts_id, file_id, created_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.users (id, email, password_hash, full_name, role, avatar_url, created_at, updated_at, avatar) FROM stdin;
bd11d69b-8264-4eb7-ac45-3ad7c901e1b2	nguyenhuynhdt22@gmail.com	$2a$10$jC4usbtxPLWH75EW3aNGgeTNiUR/Rw728z1d/3U/sbqa47nOGekom	string	STUDENT	\N	2025-11-29 08:13:09.08651	2025-11-29 08:13:09.086513	\N
b26bbbd2-5074-4a93-a10e-6320b65d8ef2	string@gmail.com	$2a$10$OP7hPwYRhETJrFfbac3IkOxr5Nuy9r38C/xsNA0cz9i6vpLVeOiSe	string 123	STUDENT	\N	2025-11-29 08:19:55.810092	2025-11-29 08:30:05.225211	\N
7710814e-22b0-40c0-8d32-5bf9f74a14de	admin_f8@gmail.com	$2a$10$Tss7uLsIWjz1fmTebWY1UeJ5hrV/iAojADkg4LWupkrhQjCMKo3xa	Natasha Black	STUDENT	\N	\N	\N	\N
991c40a1-c2b1-4e62-972a-33deafd708ff	nguyenhuynhdt37@gmail.com	$2a$10$CIhWJWY3x2LEA8lRe8n4oO.B5AgS6NytOzjVTTAu4C9ScITjKP4VG	Đẹp zai ha	ADMIN	/uploads/05fba76f-e0c5-4049-aa32-f66c81837434.jpg	2025-11-28 13:57:12.9733	2025-12-01 04:16:36.406449	\N
2b569515-99dd-48d1-a767-8f524a005338	nguyenhuynhtk37@gmail.com	$2a$10$5jmdqOKgsSQO8eBwHMtlvON0r1qdh9TiRsxPVs.9EosZblSngqnF2	Huỳnh Cu Bự	STUDENT	/uploads/3b56b24b-b3d1-44bc-9a6b-334e792d5d2e.png	\N	2025-12-01 04:17:22.378108	\N
3442f856-80ee-4615-a569-2f1783a8ba7b	nguyenhuynhtk371@gmail.com	$2a$10$QeYn/J3i/teCugy8snEzSO8I819sL/SipHJ58P11IdwZ1OmFB5JXC	Natasha Black	STUDENT	\N	\N	\N	\N
\.


--
-- Data for Name: video_asset_files; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.video_asset_files (id, video_asset_id, file_id, created_at) FROM stdin;
\.


--
-- Data for Name: video_assets; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.video_assets (id, notebook_id, created_by, language, style, text_source, video_url, duration_seconds, created_at) FROM stdin;
\.


--
-- Name: ai_tasks ai_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ai_tasks
    ADD CONSTRAINT ai_tasks_pkey PRIMARY KEY (id);


--
-- Name: file_chunks file_chunks_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_chunks
    ADD CONSTRAINT file_chunks_pkey PRIMARY KEY (id);


--
-- Name: flashcard_files flashcard_files_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_files
    ADD CONSTRAINT flashcard_files_pkey PRIMARY KEY (id);


--
-- Name: flashcard_reviews flashcard_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_reviews
    ADD CONSTRAINT flashcard_reviews_pkey PRIMARY KEY (id);


--
-- Name: flashcards flashcards_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcards
    ADD CONSTRAINT flashcards_pkey PRIMARY KEY (id);


--
-- Name: message_reactions message_reactions_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.message_reactions
    ADD CONSTRAINT message_reactions_pkey PRIMARY KEY (id);


--
-- Name: notebook_activity_logs notebook_activity_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_activity_logs
    ADD CONSTRAINT notebook_activity_logs_pkey PRIMARY KEY (id);


--
-- Name: notebook_files notebook_files_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_files
    ADD CONSTRAINT notebook_files_pkey PRIMARY KEY (id);


--
-- Name: notebook_members notebook_members_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_members
    ADD CONSTRAINT notebook_members_pkey PRIMARY KEY (id);


--
-- Name: notebook_messages notebook_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_messages
    ADD CONSTRAINT notebook_messages_pkey PRIMARY KEY (id);


--
-- Name: notebooks notebooks_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebooks
    ADD CONSTRAINT notebooks_pkey PRIMARY KEY (id);


--
-- Name: quiz_files quiz_files_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_files
    ADD CONSTRAINT quiz_files_pkey PRIMARY KEY (id);


--
-- Name: quiz_options quiz_options_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_options
    ADD CONSTRAINT quiz_options_pkey PRIMARY KEY (id);


--
-- Name: quiz_questions quiz_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_questions
    ADD CONSTRAINT quiz_questions_pkey PRIMARY KEY (id);


--
-- Name: quiz_submissions quiz_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_submissions
    ADD CONSTRAINT quiz_submissions_pkey PRIMARY KEY (id);


--
-- Name: quizzes quizzes_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT quizzes_pkey PRIMARY KEY (id);


--
-- Name: rag_queries rag_queries_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.rag_queries
    ADD CONSTRAINT rag_queries_pkey PRIMARY KEY (id);


--
-- Name: tts_assets tts_assets_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_assets
    ADD CONSTRAINT tts_assets_pkey PRIMARY KEY (id);


--
-- Name: tts_files tts_files_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_files
    ADD CONSTRAINT tts_files_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: video_asset_files video_asset_files_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_asset_files
    ADD CONSTRAINT video_asset_files_pkey PRIMARY KEY (id);


--
-- Name: video_assets video_assets_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_assets
    ADD CONSTRAINT video_assets_pkey PRIMARY KEY (id);


--
-- Name: idx_ai_tasks_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_ai_tasks_notebook ON public.ai_tasks USING btree (notebook_id, created_at);


--
-- Name: idx_ai_tasks_status; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_ai_tasks_status ON public.ai_tasks USING btree (status);


--
-- Name: idx_ai_tasks_type_status; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_ai_tasks_type_status ON public.ai_tasks USING btree (task_type, status);


--
-- Name: idx_file_chunks_embedding; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_file_chunks_embedding ON public.file_chunks USING ivfflat (embedding public.vector_cosine_ops) WITH (lists='100');


--
-- Name: idx_file_chunks_file; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_file_chunks_file ON public.file_chunks USING btree (file_id);


--
-- Name: idx_file_chunks_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_file_chunks_notebook ON public.file_chunks USING btree (notebook_id);


--
-- Name: idx_flashcard_reviews_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_flashcard_reviews_user ON public.flashcard_reviews USING btree (user_id, review_at);


--
-- Name: idx_flashcards_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_flashcards_notebook ON public.flashcards USING btree (notebook_id);


--
-- Name: idx_message_reactions_message; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_message_reactions_message ON public.message_reactions USING btree (message_id);


--
-- Name: idx_notebook_activity_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_activity_notebook ON public.notebook_activity_logs USING btree (notebook_id, created_at);


--
-- Name: idx_notebook_activity_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_activity_user ON public.notebook_activity_logs USING btree (user_id, created_at);


--
-- Name: idx_notebook_files_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_files_notebook ON public.notebook_files USING btree (notebook_id);


--
-- Name: idx_notebook_files_status; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_files_status ON public.notebook_files USING btree (status);


--
-- Name: idx_notebook_files_uploaded_by; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_files_uploaded_by ON public.notebook_files USING btree (uploaded_by);


--
-- Name: idx_notebook_members_status; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_members_status ON public.notebook_members USING btree (status);


--
-- Name: idx_notebook_members_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_members_user ON public.notebook_members USING btree (user_id);


--
-- Name: idx_notebook_messages_notebook_created; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_messages_notebook_created ON public.notebook_messages USING btree (notebook_id, created_at);


--
-- Name: idx_notebook_messages_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebook_messages_user ON public.notebook_messages USING btree (user_id);


--
-- Name: idx_notebooks_created_by; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebooks_created_by ON public.notebooks USING btree (created_by);


--
-- Name: idx_notebooks_type_visibility; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_notebooks_type_visibility ON public.notebooks USING btree (type, visibility);


--
-- Name: idx_quiz_options_question; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_quiz_options_question ON public.quiz_options USING btree (question_id);


--
-- Name: idx_quiz_questions_quiz; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_quiz_questions_quiz ON public.quiz_questions USING btree (quiz_id);


--
-- Name: idx_quiz_submissions_quiz; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_quiz_submissions_quiz ON public.quiz_submissions USING btree (quiz_id, created_at);


--
-- Name: idx_quiz_submissions_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_quiz_submissions_user ON public.quiz_submissions USING btree (user_id, created_at);


--
-- Name: idx_quizzes_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_quizzes_notebook ON public.quizzes USING btree (notebook_id);


--
-- Name: idx_rag_queries_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_rag_queries_notebook ON public.rag_queries USING btree (notebook_id, created_at);


--
-- Name: idx_rag_queries_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_rag_queries_user ON public.rag_queries USING btree (user_id, created_at);


--
-- Name: idx_tts_assets_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_tts_assets_notebook ON public.tts_assets USING btree (notebook_id, created_at);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_video_assets_notebook; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_video_assets_notebook ON public.video_assets USING btree (notebook_id, created_at);


--
-- Name: uq_message_reactions; Type: INDEX; Schema: public; Owner: admin
--

CREATE UNIQUE INDEX uq_message_reactions ON public.message_reactions USING btree (message_id, user_id, emoji);


--
-- Name: uq_notebook_members_notebook_user; Type: INDEX; Schema: public; Owner: admin
--

CREATE UNIQUE INDEX uq_notebook_members_notebook_user ON public.notebook_members USING btree (notebook_id, user_id);


--
-- Name: ai_tasks ai_tasks_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ai_tasks
    ADD CONSTRAINT ai_tasks_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE SET NULL;


--
-- Name: ai_tasks ai_tasks_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ai_tasks
    ADD CONSTRAINT ai_tasks_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: ai_tasks ai_tasks_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ai_tasks
    ADD CONSTRAINT ai_tasks_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: file_chunks file_chunks_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_chunks
    ADD CONSTRAINT file_chunks_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE CASCADE;


--
-- Name: file_chunks file_chunks_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_chunks
    ADD CONSTRAINT file_chunks_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: flashcard_files flashcard_files_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_files
    ADD CONSTRAINT flashcard_files_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE CASCADE;


--
-- Name: flashcard_files flashcard_files_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_files
    ADD CONSTRAINT flashcard_files_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: flashcard_reviews flashcard_reviews_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_reviews
    ADD CONSTRAINT flashcard_reviews_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: flashcard_reviews flashcard_reviews_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcard_reviews
    ADD CONSTRAINT flashcard_reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: flashcards flashcards_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcards
    ADD CONSTRAINT flashcards_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: flashcards flashcards_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flashcards
    ADD CONSTRAINT flashcards_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: message_reactions message_reactions_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.message_reactions
    ADD CONSTRAINT message_reactions_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.notebook_messages(id) ON DELETE CASCADE;


--
-- Name: message_reactions message_reactions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.message_reactions
    ADD CONSTRAINT message_reactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: notebook_activity_logs notebook_activity_logs_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_activity_logs
    ADD CONSTRAINT notebook_activity_logs_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: notebook_activity_logs notebook_activity_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_activity_logs
    ADD CONSTRAINT notebook_activity_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: notebook_files notebook_files_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_files
    ADD CONSTRAINT notebook_files_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: notebook_files notebook_files_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_files
    ADD CONSTRAINT notebook_files_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: notebook_members notebook_members_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_members
    ADD CONSTRAINT notebook_members_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: notebook_members notebook_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_members
    ADD CONSTRAINT notebook_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: notebook_messages notebook_messages_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_messages
    ADD CONSTRAINT notebook_messages_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: notebook_messages notebook_messages_reply_to_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_messages
    ADD CONSTRAINT notebook_messages_reply_to_message_id_fkey FOREIGN KEY (reply_to_message_id) REFERENCES public.notebook_messages(id) ON DELETE SET NULL;


--
-- Name: notebook_messages notebook_messages_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebook_messages
    ADD CONSTRAINT notebook_messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: notebooks notebooks_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.notebooks
    ADD CONSTRAINT notebooks_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quiz_files quiz_files_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_files
    ADD CONSTRAINT quiz_files_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE CASCADE;


--
-- Name: quiz_files quiz_files_quiz_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_files
    ADD CONSTRAINT quiz_files_quiz_id_fkey FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id) ON DELETE CASCADE;


--
-- Name: quiz_options quiz_options_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_options
    ADD CONSTRAINT quiz_options_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.quiz_questions(id) ON DELETE CASCADE;


--
-- Name: quiz_questions quiz_questions_quiz_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_questions
    ADD CONSTRAINT quiz_questions_quiz_id_fkey FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id) ON DELETE CASCADE;


--
-- Name: quiz_submissions quiz_submissions_quiz_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_submissions
    ADD CONSTRAINT quiz_submissions_quiz_id_fkey FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id) ON DELETE CASCADE;


--
-- Name: quiz_submissions quiz_submissions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quiz_submissions
    ADD CONSTRAINT quiz_submissions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quizzes quizzes_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT quizzes_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: quizzes quizzes_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT quizzes_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: rag_queries rag_queries_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.rag_queries
    ADD CONSTRAINT rag_queries_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: rag_queries rag_queries_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.rag_queries
    ADD CONSTRAINT rag_queries_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: tts_assets tts_assets_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_assets
    ADD CONSTRAINT tts_assets_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: tts_assets tts_assets_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_assets
    ADD CONSTRAINT tts_assets_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- Name: tts_files tts_files_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_files
    ADD CONSTRAINT tts_files_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE CASCADE;


--
-- Name: tts_files tts_files_tts_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.tts_files
    ADD CONSTRAINT tts_files_tts_id_fkey FOREIGN KEY (tts_id) REFERENCES public.tts_assets(id) ON DELETE CASCADE;


--
-- Name: video_asset_files video_asset_files_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_asset_files
    ADD CONSTRAINT video_asset_files_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.notebook_files(id) ON DELETE CASCADE;


--
-- Name: video_asset_files video_asset_files_video_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_asset_files
    ADD CONSTRAINT video_asset_files_video_asset_id_fkey FOREIGN KEY (video_asset_id) REFERENCES public.video_assets(id) ON DELETE CASCADE;


--
-- Name: video_assets video_assets_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_assets
    ADD CONSTRAINT video_assets_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: video_assets video_assets_notebook_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.video_assets
    ADD CONSTRAINT video_assets_notebook_id_fkey FOREIGN KEY (notebook_id) REFERENCES public.notebooks(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict dQATifj5thH8esgbfqdUnH0EtSReaMXRHhjW6htMS9WT8VPZhr2vpH7vNJx3ODL

