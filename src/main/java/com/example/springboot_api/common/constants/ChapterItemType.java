package com.example.springboot_api.common.constants;

/**
 * Các loại item trong Chapter.
 * Mỗi loại tham chiếu đến một bảng dữ liệu khác nhau qua ref_id.
 */
public final class ChapterItemType {

    /** File tài liệu (PDF, Word, PPT) - ref_id → notebook_files.id */
    public static final String FILE = "FILE";

    /** Bài giảng/Lecture - ref_id → lectures.id (nếu có) hoặc null */
    public static final String LECTURE = "LECTURE";

    /** Quiz/Câu hỏi trắc nghiệm - ref_id → notebook_quizzes.id */
    public static final String QUIZ = "QUIZ";

    /** Bài tập - ref_id → assignments.id (nếu có) */
    public static final String ASSIGNMENT = "ASSIGNMENT";

    /** Ghi chú/Note - không cần ref_id, nội dung trong metadata */
    public static final String NOTE = "NOTE";

    /** Video - ref_id → video_assets.id */
    public static final String VIDEO = "VIDEO";

    /** Flashcard set - ref_id → flashcard_sets.id hoặc notebook_ai_sets.id */
    public static final String FLASHCARD = "FLASHCARD";

    private ChapterItemType() {
        // Prevent instantiation
    }

    public static boolean isValid(String type) {
        return FILE.equals(type)
                || LECTURE.equals(type)
                || QUIZ.equals(type)
                || ASSIGNMENT.equals(type)
                || NOTE.equals(type)
                || VIDEO.equals(type)
                || FLASHCARD.equals(type);
    }
}
