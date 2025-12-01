# Tài liệu Schema Database - Notebook System

Tài liệu này mô tả chi tiết tất cả các bảng và trường trong database của hệ thống Notebook.

## Mục lục

1. [Bảng Core](#bảng-core)
   - [users](#users)
   - [notebooks](#notebooks)
   - [notebook_members](#notebook_members)
2. [Bảng File Management](#bảng-file-management)
   - [notebook_files](#notebook_files)
   - [file_chunks](#file_chunks)
3. [Bảng AI & Processing](#bảng-ai--processing)
   - [ai_tasks](#ai_tasks)
   - [rag_queries](#rag_queries)
4. [Bảng Learning Features](#bảng-learning-features)
   - [flashcards](#flashcards)
   - [flashcard_reviews](#flashcard_reviews)
   - [flashcard_files](#flashcard_files)
   - [quizzes](#quizzes)
   - [quiz_questions](#quiz_questions)
   - [quiz_options](#quiz_options)
   - [quiz_submissions](#quiz_submissions)
   - [quiz_files](#quiz_files)
5. [Bảng Communication](#bảng-communication)
   - [notebook_messages](#notebook_messages)
   - [message_reactions](#message_reactions)
   - [notebook_activity_logs](#notebook_activity_logs)
6. [Bảng Media Assets](#bảng-media-assets)
   - [tts_assets](#tts_assets)
   - [tts_files](#tts_files)
   - [video_assets](#video_assets)
   - [video_asset_files](#video_asset_files)

---

## Bảng Core

### users

Bảng lưu thông tin người dùng trong hệ thống.

| Trường          | Kiểu dữ liệu | Ràng buộc             | Mô tả                                         |
| --------------- | ------------ | --------------------- | --------------------------------------------- |
| `id`            | UUID         | PRIMARY KEY, NOT NULL | ID duy nhất của người dùng (tự động generate) |
| `email`         | VARCHAR(255) | UNIQUE, NOT NULL      | Email đăng nhập (duy nhất)                    |
| `password_hash` | TEXT         | NOT NULL              | Mật khẩu đã được hash bằng BCrypt             |
| `full_name`     | VARCHAR(255) | NULL                  | Tên đầy đủ của người dùng                     |
| `role`          | VARCHAR(50)  | NOT NULL, CHECK       | Vai trò: `STUDENT`, `TEACHER`, `ADMIN`        |
| `avatar_url`    | TEXT         | NULL                  | URL ảnh đại diện                              |
| `avatar`        | VARCHAR      | NULL                  | (Trường dự phòng, có thể không sử dụng)       |
| `created_at`    | TIMESTAMP    | DEFAULT now()         | Thời gian tạo tài khoản                       |
| `updated_at`    | TIMESTAMP    | DEFAULT now()         | Thời gian cập nhật cuối cùng                  |

**Indexes:**

- `idx_users_email`: Index trên `email` để tìm kiếm nhanh

---

### notebooks

Bảng lưu thông tin các notebook (sổ ghi chép).

| Trường          | Kiểu dữ liệu | Ràng buộc                | Mô tả                                          |
| --------------- | ------------ | ------------------------ | ---------------------------------------------- |
| `id`            | UUID         | PRIMARY KEY, NOT NULL    | ID duy nhất của notebook                       |
| `title`         | VARCHAR(255) | NOT NULL                 | Tiêu đề notebook                               |
| `description`   | TEXT         | NULL                     | Mô tả notebook                                 |
| `type`          | VARCHAR(50)  | NOT NULL, CHECK          | Loại: `community`, `private_group`, `personal` |
| `visibility`    | VARCHAR(50)  | NOT NULL, CHECK          | Hiển thị: `public`, `private`                  |
| `created_by`    | UUID         | NOT NULL, FK → users(id) | ID người tạo notebook                          |
| `thumbnail_url` | TEXT         | NULL                     | URL ảnh thumbnail                              |
| `metadata`      | JSONB        | NULL                     | Dữ liệu metadata bổ sung (JSON)                |
| `created_at`    | TIMESTAMPTZ  | DEFAULT now(), NOT NULL  | Thời gian tạo                                  |
| `updated_at`    | TIMESTAMPTZ  | DEFAULT now(), NOT NULL  | Thời gian cập nhật                             |

**Indexes:**

- `idx_notebooks_type_visibility`: Index trên `type, visibility`
- `idx_notebooks_created_by`: Index trên `created_by`

**Foreign Keys:**

- `created_by` → `users(id)` ON DELETE CASCADE

---

### notebook_members

Bảng lưu thông tin thành viên của notebook.

| Trường        | Kiểu dữ liệu | Ràng buộc                    | Mô tả                                                    |
| ------------- | ------------ | ---------------------------- | -------------------------------------------------------- |
| `id`          | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất                                              |
| `notebook_id` | UUID         | NOT NULL, FK → notebooks(id) | ID notebook                                              |
| `user_id`     | UUID         | NOT NULL, FK → users(id)     | ID người dùng                                            |
| `role`        | VARCHAR(50)  | NOT NULL, CHECK              | Vai trò: `owner`, `admin`, `member`                      |
| `status`      | VARCHAR(50)  | NOT NULL, CHECK              | Trạng thái: `pending`, `approved`, `rejected`, `blocked` |
| `joined_at`   | TIMESTAMPTZ  | NULL                         | Thời gian tham gia (khi được approve)                    |
| `created_at`  | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo                                            |
| `updated_at`  | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian cập nhật                                       |

**Indexes:**

- `uq_notebook_members_notebook_user`: UNIQUE trên `notebook_id, user_id` (một user chỉ có một membership)
- `idx_notebook_members_user`: Index trên `user_id`
- `idx_notebook_members_status`: Index trên `status`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE CASCADE

---

## Bảng File Management

### notebook_files

Bảng lưu thông tin các file được upload vào notebook.

| Trường              | Kiểu dữ liệu | Ràng buộc                    | Mô tả                                                                 |
| ------------------- | ------------ | ---------------------------- | --------------------------------------------------------------------- |
| `id`                | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất của file                                                  |
| `notebook_id`       | UUID         | NOT NULL, FK → notebooks(id) | ID notebook chứa file                                                 |
| `uploaded_by`       | UUID         | NOT NULL, FK → users(id)     | ID người upload                                                       |
| `original_filename` | TEXT         | NOT NULL                     | Tên file gốc                                                          |
| `mime_type`         | VARCHAR(255) | NULL                         | MIME type của file (ví dụ: `application/pdf`)                         |
| `file_size`         | BIGINT       | NULL                         | Kích thước file (bytes)                                               |
| `storage_url`       | TEXT         | NOT NULL                     | URL lưu trữ file trên server                                          |
| `status`            | VARCHAR(50)  | NOT NULL, CHECK              | Trạng thái: `pending`, `approved`, `rejected`, `processing`, `failed` |
| `pages_count`       | INTEGER      | NULL                         | Số trang (cho PDF)                                                    |
| `ocr_done`          | BOOLEAN      | DEFAULT false, NOT NULL      | Đã hoàn thành OCR chưa                                                |
| `embedding_done`    | BOOLEAN      | DEFAULT false, NOT NULL      | Đã tạo embedding chưa                                                 |
| `chunk_size`        | INTEGER      | DEFAULT 800                  | Kích thước chunk khi chia nhỏ text                                    |
| `chunk_overlap`     | INTEGER      | DEFAULT 120                  | Độ overlap giữa các chunk                                             |
| `extra_metadata`    | JSONB        | NULL                         | Metadata bổ sung (JSON)                                               |
| `created_at`        | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian upload                                                      |
| `updated_at`        | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian cập nhật                                                    |

**Indexes:**

- `idx_notebook_files_notebook`: Index trên `notebook_id`
- `idx_notebook_files_status`: Index trên `status`
- `idx_notebook_files_uploaded_by`: Index trên `uploaded_by`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `uploaded_by` → `users(id)` ON DELETE SET NULL

---

### file_chunks

Bảng lưu các đoạn text đã được chia nhỏ từ file và embedding vector của chúng.

| Trường        | Kiểu dữ liệu | Ràng buộc                         | Mô tả                                                  |
| ------------- | ------------ | --------------------------------- | ------------------------------------------------------ |
| `id`          | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất                                            |
| `notebook_id` | UUID         | NOT NULL, FK → notebooks(id)      | ID notebook                                            |
| `file_id`     | UUID         | NOT NULL, FK → notebook_files(id) | ID file gốc                                            |
| `chunk_index` | INTEGER      | NOT NULL                          | Thứ tự chunk trong file (bắt đầu từ 0)                 |
| `content`     | TEXT         | NOT NULL                          | Nội dung text của chunk                                |
| `embedding`   | VECTOR(1536) | NOT NULL                          | Vector embedding 1536 chiều (dùng cho semantic search) |
| `metadata`    | JSONB        | NULL                              | Metadata bổ sung                                       |
| `created_at`  | TIMESTAMPTZ  | DEFAULT now(), NOT NULL           | Thời gian tạo                                          |

**Indexes:**

- `idx_file_chunks_file`: Index trên `file_id`
- `idx_file_chunks_notebook`: Index trên `notebook_id`
- `idx_file_chunks_embedding`: Index IVFFlat trên `embedding` (để tìm kiếm vector nhanh)

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE CASCADE

---

## Bảng AI & Processing

### ai_tasks

Bảng lưu các task AI đang xử lý (summary, flashcards, quiz, TTS, video, etc.).

| Trường          | Kiểu dữ liệu | Ràng buộc                     | Mô tả                                                            |
| --------------- | ------------ | ----------------------------- | ---------------------------------------------------------------- |
| `id`            | UUID         | PRIMARY KEY, NOT NULL         | ID duy nhất                                                      |
| `notebook_id`   | UUID         | NOT NULL, FK → notebooks(id)  | ID notebook                                                      |
| `file_id`       | UUID         | NULL, FK → notebook_files(id) | ID file liên quan (nếu có)                                       |
| `user_id`       | UUID         | NULL, FK → users(id)          | ID người dùng tạo task                                           |
| `task_type`     | VARCHAR(50)  | NOT NULL, CHECK               | Loại: `summary`, `flashcards`, `quiz`, `tts`, `video`, `other`   |
| `status`        | VARCHAR(50)  | NOT NULL, CHECK               | Trạng thái: `queued`, `processing`, `done`, `failed`, `canceled` |
| `input_config`  | JSONB        | NULL                          | Cấu hình đầu vào (JSON)                                          |
| `output_data`   | JSONB        | NULL                          | Kết quả đầu ra (JSON)                                            |
| `error_message` | TEXT         | NULL                          | Thông báo lỗi (nếu có)                                           |
| `created_at`    | TIMESTAMPTZ  | DEFAULT now(), NOT NULL       | Thời gian tạo                                                    |
| `updated_at`    | TIMESTAMPTZ  | DEFAULT now(), NOT NULL       | Thời gian cập nhật                                               |

**Indexes:**

- `idx_ai_tasks_notebook`: Index trên `notebook_id, created_at`
- `idx_ai_tasks_status`: Index trên `status`
- `idx_ai_tasks_type_status`: Index trên `task_type, status`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE SET NULL
- `user_id` → `users(id)` ON DELETE SET NULL

---

### rag_queries

Bảng lưu lịch sử các câu hỏi RAG (Retrieval-Augmented Generation) và câu trả lời.

| Trường          | Kiểu dữ liệu | Ràng buộc                    | Mô tả                                             |
| --------------- | ------------ | ---------------------------- | ------------------------------------------------- |
| `id`            | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất                                       |
| `notebook_id`   | UUID         | NOT NULL, FK → notebooks(id) | ID notebook                                       |
| `user_id`       | UUID         | NULL, FK → users(id)         | ID người hỏi                                      |
| `question`      | TEXT         | NOT NULL                     | Câu hỏi                                           |
| `answer`        | TEXT         | NULL                         | Câu trả lời từ AI                                 |
| `source_chunks` | JSONB        | NULL                         | Danh sách các chunk được sử dụng làm nguồn (JSON) |
| `latency_ms`    | INTEGER      | NULL                         | Thời gian xử lý (milliseconds)                    |
| `created_at`    | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo                                     |

**Indexes:**

- `idx_rag_queries_notebook`: Index trên `notebook_id, created_at`
- `idx_rag_queries_user`: Index trên `user_id, created_at`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE SET NULL

---

## Bảng Learning Features

### flashcards

Bảng lưu các flashcard (thẻ ghi nhớ).

| Trường           | Kiểu dữ liệu | Ràng buộc                    | Mô tả                          |
| ---------------- | ------------ | ---------------------------- | ------------------------------ |
| `id`             | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất                    |
| `notebook_id`    | UUID         | NOT NULL, FK → notebooks(id) | ID notebook                    |
| `created_by`     | UUID         | NULL, FK → users(id)         | ID người tạo                   |
| `front_text`     | TEXT         | NOT NULL                     | Nội dung mặt trước (câu hỏi)   |
| `back_text`      | TEXT         | NOT NULL                     | Nội dung mặt sau (câu trả lời) |
| `extra_metadata` | JSONB        | NULL                         | Metadata bổ sung               |
| `created_at`     | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo                  |

**Indexes:**

- `idx_flashcards_notebook`: Index trên `notebook_id`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `created_by` → `users(id)` ON DELETE SET NULL

---

### flashcard_reviews

Bảng lưu lịch sử review flashcard (theo thuật toán spaced repetition).

| Trường          | Kiểu dữ liệu     | Ràng buộc                     | Mô tả                                 |
| --------------- | ---------------- | ----------------------------- | ------------------------------------- |
| `id`            | UUID             | PRIMARY KEY, NOT NULL         | ID duy nhất                           |
| `flashcard_id`  | UUID             | NOT NULL, FK → flashcards(id) | ID flashcard                          |
| `user_id`       | UUID             | NOT NULL, FK → users(id)      | ID người review                       |
| `ease_factor`   | DOUBLE PRECISION | NULL                          | Hệ số dễ dàng (cho spaced repetition) |
| `interval_days` | INTEGER          | NULL                          | Số ngày đến lần review tiếp theo      |
| `quality`       | INTEGER          | NULL                          | Chất lượng review (0-5)               |
| `review_at`     | TIMESTAMPTZ      | DEFAULT now(), NOT NULL       | Thời gian review                      |

**Indexes:**

- `idx_flashcard_reviews_user`: Index trên `user_id, review_at`

**Foreign Keys:**

- `flashcard_id` → `flashcards(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE CASCADE

---

### flashcard_files

Bảng liên kết flashcard với file (many-to-many).

| Trường         | Kiểu dữ liệu | Ràng buộc                         | Mô tả         |
| -------------- | ------------ | --------------------------------- | ------------- |
| `id`           | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất   |
| `flashcard_id` | UUID         | NOT NULL, FK → flashcards(id)     | ID flashcard  |
| `file_id`      | UUID         | NOT NULL, FK → notebook_files(id) | ID file       |
| `created_at`   | TIMESTAMP    | DEFAULT now()                     | Thời gian tạo |

**Foreign Keys:**

- `flashcard_id` → `flashcards(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE CASCADE

---

### quizzes

Bảng lưu các bài quiz.

| Trường        | Kiểu dữ liệu | Ràng buộc                    | Mô tả            |
| ------------- | ------------ | ---------------------------- | ---------------- |
| `id`          | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất      |
| `notebook_id` | UUID         | NOT NULL, FK → notebooks(id) | ID notebook      |
| `title`       | VARCHAR(255) | NOT NULL                     | Tiêu đề quiz     |
| `created_by`  | UUID         | NULL, FK → users(id)         | ID người tạo     |
| `metadata`    | JSONB        | NULL                         | Metadata bổ sung |
| `created_at`  | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo    |

**Indexes:**

- `idx_quizzes_notebook`: Index trên `notebook_id`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `created_by` → `users(id)` ON DELETE SET NULL

---

### quiz_questions

Bảng lưu các câu hỏi trong quiz.

| Trường          | Kiểu dữ liệu | Ràng buộc                           | Mô tả            |
| --------------- | ------------ | ----------------------------------- | ---------------- |
| `id`            | UUID         | PRIMARY KEY, NOT NULL               | ID duy nhất      |
| `quiz_id`       | UUID         | NOT NULL, FK → quizzes(id)          | ID quiz          |
| `question_text` | TEXT         | NOT NULL                            | Nội dung câu hỏi |
| `question_type` | VARCHAR(32)  | DEFAULT 'multiple_choice', NOT NULL | Loại câu hỏi     |
| `metadata`      | JSONB        | NULL                                | Metadata bổ sung |

**Indexes:**

- `idx_quiz_questions_quiz`: Index trên `quiz_id`

**Foreign Keys:**

- `quiz_id` → `quizzes(id)` ON DELETE CASCADE

---

### quiz_options

Bảng lưu các lựa chọn (đáp án) của câu hỏi.

| Trường        | Kiểu dữ liệu | Ràng buộc                         | Mô tả               |
| ------------- | ------------ | --------------------------------- | ------------------- |
| `id`          | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất         |
| `question_id` | UUID         | NOT NULL, FK → quiz_questions(id) | ID câu hỏi          |
| `option_text` | TEXT         | NOT NULL                          | Nội dung lựa chọn   |
| `is_correct`  | BOOLEAN      | DEFAULT false, NOT NULL           | Đáp án đúng hay sai |

**Indexes:**

- `idx_quiz_options_question`: Index trên `question_id`

**Foreign Keys:**

- `question_id` → `quiz_questions(id)` ON DELETE CASCADE

---

### quiz_submissions

Bảng lưu kết quả làm bài của người dùng.

| Trường       | Kiểu dữ liệu     | Ràng buộc                  | Mô tả                          |
| ------------ | ---------------- | -------------------------- | ------------------------------ |
| `id`         | UUID             | PRIMARY KEY, NOT NULL      | ID duy nhất                    |
| `quiz_id`    | UUID             | NOT NULL, FK → quizzes(id) | ID quiz                        |
| `user_id`    | UUID             | NOT NULL, FK → users(id)   | ID người làm bài               |
| `score`      | DOUBLE PRECISION | NULL                       | Điểm số (0.0 - 1.0 hoặc 0-100) |
| `answers`    | JSONB            | NULL                       | Các câu trả lời (JSON format)  |
| `created_at` | TIMESTAMPTZ      | DEFAULT now(), NOT NULL    | Thời gian nộp bài              |

**Indexes:**

- `idx_quiz_submissions_quiz`: Index trên `quiz_id, created_at`
- `idx_quiz_submissions_user`: Index trên `user_id, created_at`

**Foreign Keys:**

- `quiz_id` → `quizzes(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE CASCADE

---

### quiz_files

Bảng liên kết quiz với file (many-to-many).

| Trường       | Kiểu dữ liệu | Ràng buộc                         | Mô tả         |
| ------------ | ------------ | --------------------------------- | ------------- |
| `id`         | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất   |
| `quiz_id`    | UUID         | NOT NULL, FK → quizzes(id)        | ID quiz       |
| `file_id`    | UUID         | NOT NULL, FK → notebook_files(id) | ID file       |
| `created_at` | TIMESTAMP    | DEFAULT now()                     | Thời gian tạo |

**Foreign Keys:**

- `quiz_id` → `quizzes(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE CASCADE

---

## Bảng Communication

### notebook_messages

Bảng lưu các tin nhắn trong notebook (chat).

| Trường                | Kiểu dữ liệu | Ràng buộc                        | Mô tả                                |
| --------------------- | ------------ | -------------------------------- | ------------------------------------ |
| `id`                  | UUID         | PRIMARY KEY, NOT NULL            | ID duy nhất                          |
| `notebook_id`         | UUID         | NOT NULL, FK → notebooks(id)     | ID notebook                          |
| `user_id`             | UUID         | NULL, FK → users(id)             | ID người gửi (NULL nếu là system/AI) |
| `type`                | VARCHAR(50)  | NOT NULL, CHECK                  | Loại: `user`, `system`, `ai`         |
| `content`             | TEXT         | NOT NULL                         | Nội dung tin nhắn                    |
| `reply_to_message_id` | UUID         | NULL, FK → notebook_messages(id) | ID tin nhắn được reply               |
| `ai_context`          | JSONB        | NULL                             | Context cho AI (nếu là tin nhắn AI)  |
| `created_at`          | TIMESTAMPTZ  | DEFAULT now(), NOT NULL          | Thời gian gửi                        |

**Indexes:**

- `idx_notebook_messages_notebook_created`: Index trên `notebook_id, created_at`
- `idx_notebook_messages_user`: Index trên `user_id`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE SET NULL
- `reply_to_message_id` → `notebook_messages(id)` ON DELETE SET NULL

---

### message_reactions

Bảng lưu các reaction (emoji) trên tin nhắn.

| Trường       | Kiểu dữ liệu | Ràng buộc                            | Mô tả                 |
| ------------ | ------------ | ------------------------------------ | --------------------- |
| `id`         | UUID         | PRIMARY KEY, NOT NULL                | ID duy nhất           |
| `message_id` | UUID         | NOT NULL, FK → notebook_messages(id) | ID tin nhắn           |
| `user_id`    | UUID         | NOT NULL, FK → users(id)             | ID người reaction     |
| `emoji`      | VARCHAR(32)  | NOT NULL                             | Emoji (ví dụ: 👍, ❤️) |
| `created_at` | TIMESTAMPTZ  | DEFAULT now(), NOT NULL              | Thời gian reaction    |

**Indexes:**

- `uq_message_reactions`: UNIQUE trên `message_id, user_id, emoji` (một user chỉ reaction một emoji một lần)
- `idx_message_reactions_message`: Index trên `message_id`

**Foreign Keys:**

- `message_id` → `notebook_messages(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE CASCADE

---

### notebook_activity_logs

Bảng lưu log các hoạt động trong notebook.

| Trường        | Kiểu dữ liệu | Ràng buộc                    | Mô tả                                              |
| ------------- | ------------ | ---------------------------- | -------------------------------------------------- |
| `id`          | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất                                        |
| `notebook_id` | UUID         | NOT NULL, FK → notebooks(id) | ID notebook                                        |
| `user_id`     | UUID         | NULL, FK → users(id)         | ID người thực hiện (NULL nếu system)               |
| `action`      | VARCHAR(64)  | NOT NULL                     | Hành động (ví dụ: `file_uploaded`, `member_added`) |
| `target_id`   | UUID         | NULL                         | ID đối tượng liên quan                             |
| `target_type` | VARCHAR(64)  | NULL                         | Loại đối tượng (ví dụ: `file`, `member`)           |
| `metadata`    | JSONB        | NULL                         | Metadata bổ sung                                   |
| `created_at`  | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian log                                      |

**Indexes:**

- `idx_notebook_activity_notebook`: Index trên `notebook_id, created_at`
- `idx_notebook_activity_user`: Index trên `user_id, created_at`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE SET NULL

---

## Bảng Media Assets

### tts_assets

Bảng lưu các file audio TTS (Text-to-Speech).

| Trường             | Kiểu dữ liệu | Ràng buộc                    | Mô tả                        |
| ------------------ | ------------ | ---------------------------- | ---------------------------- |
| `id`               | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất                  |
| `notebook_id`      | UUID         | NOT NULL, FK → notebooks(id) | ID notebook                  |
| `created_by`       | UUID         | NULL, FK → users(id)         | ID người tạo                 |
| `language`         | VARCHAR(16)  | NULL                         | Ngôn ngữ (ví dụ: `vi`, `en`) |
| `voice_name`       | VARCHAR(64)  | NULL                         | Tên giọng nói                |
| `text_source`      | TEXT         | NULL                         | Text nguồn để tạo TTS        |
| `audio_url`        | TEXT         | NOT NULL                     | URL file audio               |
| `duration_seconds` | INTEGER      | NULL                         | Độ dài audio (giây)          |
| `created_at`       | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo                |

**Indexes:**

- `idx_tts_assets_notebook`: Index trên `notebook_id, created_at`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `created_by` → `users(id)` ON DELETE SET NULL

---

### tts_files

Bảng liên kết TTS asset với file (many-to-many).

| Trường       | Kiểu dữ liệu | Ràng buộc                         | Mô tả         |
| ------------ | ------------ | --------------------------------- | ------------- |
| `id`         | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất   |
| `tts_id`     | UUID         | NOT NULL, FK → tts_assets(id)     | ID TTS asset  |
| `file_id`    | UUID         | NOT NULL, FK → notebook_files(id) | ID file       |
| `created_at` | TIMESTAMPTZ  | DEFAULT now()                     | Thời gian tạo |

**Foreign Keys:**

- `tts_id` → `tts_assets(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE CASCADE

---

### video_assets

Bảng lưu các video asset.

| Trường             | Kiểu dữ liệu | Ràng buộc                    | Mô tả               |
| ------------------ | ------------ | ---------------------------- | ------------------- |
| `id`               | UUID         | PRIMARY KEY, NOT NULL        | ID duy nhất         |
| `notebook_id`      | UUID         | NOT NULL, FK → notebooks(id) | ID notebook         |
| `created_by`       | UUID         | NULL, FK → users(id)         | ID người tạo        |
| `language`         | VARCHAR(16)  | NULL                         | Ngôn ngữ            |
| `style`            | VARCHAR(64)  | NULL                         | Phong cách video    |
| `text_source`      | TEXT         | NULL                         | Text nguồn          |
| `video_url`        | TEXT         | NOT NULL                     | URL video           |
| `duration_seconds` | INTEGER      | NULL                         | Độ dài video (giây) |
| `created_at`       | TIMESTAMPTZ  | DEFAULT now(), NOT NULL      | Thời gian tạo       |

**Indexes:**

- `idx_video_assets_notebook`: Index trên `notebook_id, created_at`

**Foreign Keys:**

- `notebook_id` → `notebooks(id)` ON DELETE CASCADE
- `created_by` → `users(id)` ON DELETE SET NULL

---

### video_asset_files

Bảng liên kết video asset với file (many-to-many).

| Trường           | Kiểu dữ liệu | Ràng buộc                         | Mô tả          |
| ---------------- | ------------ | --------------------------------- | -------------- |
| `id`             | UUID         | PRIMARY KEY, NOT NULL             | ID duy nhất    |
| `video_asset_id` | UUID         | NOT NULL, FK → video_assets(id)   | ID video asset |
| `file_id`        | UUID         | NOT NULL, FK → notebook_files(id) | ID file        |
| `created_at`     | TIMESTAMP    | DEFAULT now()                     | Thời gian tạo  |

**Foreign Keys:**

- `video_asset_id` → `video_assets(id)` ON DELETE CASCADE
- `file_id` → `notebook_files(id)` ON DELETE CASCADE

---

## Ghi chú quan trọng

### Extensions được sử dụng

1. **uuid-ossp**: Extension để generate UUID tự động
2. **vector**: Extension pgvector để lưu trữ và tìm kiếm vector embedding (dùng cho semantic search)

### Ràng buộc CHECK

Các bảng có ràng buộc CHECK để đảm bảo dữ liệu hợp lệ:

- `users.role`: Chỉ cho phép `STUDENT`, `TEACHER`, `ADMIN`
- `notebooks.type`: Chỉ cho phép `community`, `private_group`, `personal`
- `notebooks.visibility`: Chỉ cho phép `public`, `private`
- `notebook_files.status`: Chỉ cho phép `pending`, `approved`, `rejected`, `processing`, `failed`
- `notebook_members.role`: Chỉ cho phép `owner`, `admin`, `member`
- `notebook_members.status`: Chỉ cho phép `pending`, `approved`, `rejected`, `blocked`
- `notebook_messages.type`: Chỉ cho phép `user`, `system`, `ai`
- `ai_tasks.task_type`: Chỉ cho phép `summary`, `flashcards`, `quiz`, `tts`, `video`, `other`
- `ai_tasks.status`: Chỉ cho phép `queued`, `processing`, `done`, `failed`, `canceled`

### Foreign Key Actions

- **ON DELETE CASCADE**: Khi xóa parent record, tự động xóa tất cả child records
- **ON DELETE SET NULL**: Khi xóa parent record, set foreign key thành NULL (cho phép NULL)

### Indexes quan trọng

- **Vector Index**: `idx_file_chunks_embedding` sử dụng IVFFlat để tìm kiếm vector nhanh (semantic search)
- **Composite Indexes**: Nhiều index kết hợp để tối ưu query phức tạp
- **Unique Indexes**: Đảm bảo tính duy nhất (ví dụ: một user chỉ có một membership trong một notebook)

---

_Tài liệu được tạo tự động từ schema database. Cập nhật lần cuối: 2025-12-01_
