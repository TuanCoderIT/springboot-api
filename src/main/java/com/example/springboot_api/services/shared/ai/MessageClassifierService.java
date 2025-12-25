package com.example.springboot_api.services.shared.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service phân loại câu hỏi cho chat học tập.
 * Mục tiêu: Tối ưu hóa việc gọi RAG/Search bằng cách phân loại câu hỏi.
 */
@Service
@RequiredArgsConstructor
public class MessageClassifierService {

    private static final Logger log = LoggerFactory.getLogger(MessageClassifierService.class);
    private final AIModelService aiModelService;

    // =========================
    // KEYWORDS
    // =========================

    private static final List<String> SMALL_TALK = Arrays.asList(
            // VN
            "chao", "xin chao", "chao ban", "cam on", "cam on ban", "cam on nhe",
            "ok", "oke", "okay", "okie", "duoc", "duoc roi", "dc roi", "hieu roi",
            "da hieu", "ro roi", "tot", "hay", "tuyet", "tuyet voi", "haha", "hihi",
            "lol", "ok thanks", "nice thanks",
            // EN
            "hello", "hi", "hey", "yo", "thanks", "thank", "thank you", "tks", "ty",
            "nice", "great", "good", "awesome", "amazing", "perfect", "got it",
            "i see", "alright", "fine", "sure", "yep", "yup", "yeah", "bye",
            "goodbye", "see you", "later", "okay got it");

    private static final List<String> FOLLOW_UP = Arrays.asList(
            // VN
            "y do", "y tren", "y nay", "doan do", "doan tren", "cai do", "cai nay",
            "phan do", "phan tren", "noi ro hon", "noi ro", "noi them", "noi tiep",
            "tiep di", "tiep tuc", "them nua", "gi nua", "giai thich them",
            "giai thich ro hon", "giai thich ro", "giai thich lai", "giai thich ki hon",
            "chi tiet hon", "chi tiet them", "cu the hon", "cu the", "ro hon",
            "vi du them", "them vi du", "cho vi du", "minh chua hieu", "chua hieu",
            "khong hieu", "ko hieu", "chua ro", "khong ro", "tai sao vay", "sao vay",
            "the la sao", "nghia la gi", "co nghia gi", "lam sao", "bang cach nao",
            "nhu the nao", "the nao", "tuc la sao", "tuc la", "roi sao", "roi sao nua",
            "tiep i", "tiep tuc i",
            // EN
            "explain more", "more detail", "more details", "more details please",
            "elaborate", "can you explain", "tell me more", "go on", "continue",
            "what do you mean", "what does that mean", "i dont understand",
            "i don't understand", "dont get it", "dont understand", "why is that",
            "why so", "how so", "how come", "for example", "give example",
            "example please", "another example", "clarify", "be more specific",
            "specifically");

    private static final Set<String> AMBIGUOUS_SHORT_SET = new HashSet<>(Arrays.asList(
            // VN
            "sao", "sao?", "ha", "ha?", "gi", "gi?", "the", "do", "roi sao",
            "vay sao", "sao ta", "gi vay", "gi the", "gi day",
            // EN
            "why", "why?", "what", "what?", "how", "how?", "huh", "hmm", "uh", "uhh",
            // symbols
            "?", "??", "???", "!?", "!!", "!"));

    // =========================
    // PUBLIC METHODS
    // =========================

    /**
     * Phân loại message thành NO_SEARCH | REUSE | SEARCH.
     * 
     * @param message          Câu hỏi của user
     * @param hasPrevContext   Có context từ lần search trước không
     * @param previousMessages List các message trước đó (để LLM hiểu ngữ cảnh)
     * @return ClassifierMode
     */
    public ClassifierMode classifyMessage(String message, boolean hasPrevContext, List<String> previousMessages) {
        // 1. Rule-based classification (nhanh, rẻ)
        ClassifierMode mode = ruleDecide(message, hasPrevContext);
        if (mode != null) {
            log.debug("Classified by rule: {} -> {}", message, mode);
            return mode;
        }

        // 2. Fallback to LLM (chậm hơn, tốn tiền, nhưng thông minh hơn)
        log.debug("Rule ambiguous, fallback to LLM for: {}", message);
        return llmClassifyMode(message, hasPrevContext, previousMessages);
    }

    /**
     * Chuẩn hóa text để so keyword.
     */
    public String normalizeText(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }

        s = s.trim().toLowerCase();

        // Xử lý các ký tự đặc biệt phổ biến
        s = s.replace("c++", "cpp").replace("c#", "csharp");
        s = s.replace("đ", "d").replace("Đ", "d");

        // Bỏ dấu tiếng Việt
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");

        // Giữ a-z 0-9 space ? !
        s = s.replaceAll("[^a-z0-9\\s?!]", " ");
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }

    // =========================
    // PRIVATE HELPERS
    // =========================

    private boolean matchKeyword(String text, String kw) {
        kw = kw.trim();
        if (kw.isEmpty()) {
            return false;
        }

        // Keyword chỉ toàn dấu ?! thì match chứa là đủ
        boolean allSymbols = true;
        for (char c : kw.toCharArray()) {
            if (c != '?' && c != '!' && c != ' ') {
                allSymbols = false;
                break;
            }
        }

        if (allSymbols) {
            return text.contains(kw);
        }

        // Match "whole word" boundary regex
        // (?<!\w)kw(?!\w)
        String regex = "(?<!\\w)" + Pattern.quote(kw) + "(?!\\w)";
        return Pattern.compile(regex).matcher(text).find();
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (matchKeyword(text, kw)) {
                return true;
            }
        }
        return false;
    }

    private ClassifierMode ruleDecide(String message, boolean hasPrevContext) {
        String m = normalizeText(message);
        if (m.isEmpty()) {
            return ClassifierMode.NO_SEARCH;
        }

        int noScore = 0;
        int reuseScore = 0;
        int searchScore = 0;

        // 1) Giao tiếp: câu rất ngắn + từ xã giao
        if (m.length() <= 25 && containsAny(m, SMALL_TALK)) {
            noScore += 4;
        }

        // 2) Dấu hiệu "hỏi tiếp"
        // (a) Từ chỉ trỏ / tham chiếu
        if (Pattern.compile("(?<!\\w)(do|day|nay|kia|tren|duoi|doan|phan|cai)(?!\\w)").matcher(m).find()) {
            reuseScore += 2;
        }

        // (b) Cụm giải thích
        if (Pattern.compile(
                "(giai thich|noi ro|y la|tuc la|noi them|chi tiet|cu the|chua hieu|khong hieu|ko hieu|vi du|explain|elaborate|more detail|tell me more|what do you mean|dont understand|y ban la|y cua ban)")
                .matcher(m).find()) {
            reuseScore += 4;
        }

        // (c) Câu chỉ toàn ?!
        if (m.matches("[?!]+")) {
            if (hasPrevContext) {
                reuseScore += 4;
            } else {
                noScore += 3;
            }
        }

        // (d) Câu rất ngắn + có ?
        if (m.length() <= 10 && m.contains("?")) {
            reuseScore += 2;
        }

        // (e) Ambiguous short keywords
        if (AMBIGUOUS_SHORT_SET.contains(m)) {
            reuseScore += 2;
        }

        // (f) Follow-up keywords
        if (containsAny(m, FOLLOW_UP)) {
            reuseScore += 5; // Tăng điểm follow up để thắng search score (3 -> 5)
        }

        // 3) Dấu hiệu "hỏi kiến thức mới"
        // Có từ để hỏi + nội dung
        if (Pattern.compile(
                "(la gi|dinh nghia|cong thuc|huong dan|cai dat|how to|how does|what is|define|explain.*architecture|compare|so sanh)")
                .matcher(m).find()) {
            searchScore += 4;
        }

        // Có từ liên quan video/lesson
        if (Pattern.compile(
                "(video|bai hoc|giang vien|tom tat|phut thu|noi dung chinh|y chinh|diem quan trong|doan phut|phut \\d|concept at|lesson|instructor|summarize|main topic|this video|cover|demo)")
                .matcher(m).find()) {
            searchScore += 4;
        }

        // Có từ liên quan công văn/quy chế (REGULATION)
        if (Pattern.compile(
                "(cong van|nghi dinh|thong tu|quyet dinh|luat|bo luat|hien phap|chi thi|nguyen tac|dieu|khoan|chuong|muc|phu luc|van ban|quy che|quy dinh|huong dan)")
                .matcher(m).find()) {
            searchScore += 4;
        }

        // Có nhiều từ kỹ thuật (Heuristic)
        String techRegex = "(react|node|python|javascript|typescript|docker|kubernetes|sql|nosql|mysql|postgresql|" +
                "api|database|async|await|hook|component|java|spring|golang|redis|mongodb|" +
                "flask|django|fastapi|angular|vue|nestjs|express|graphql|microservices|" +
                "aws|azure|gcp|machine learning|deep learning|neural network|tensorflow|pytorch|" +
                "rust|cpp|csharp|laravel|php|ruby|rails|next|nuxt|webpack|vite|git|linux|devops|" +
                "rest|grpc|websocket|kafka|rabbitmq|elasticsearch|nginx|apache)";
        if (Pattern.compile(techRegex).matcher(m).find()) {
            searchScore += 3;
            // Có tech term + không context -> rất có thể hỏi mới
            if (!hasPrevContext) {
                searchScore += 2;
            }
        }

        // Ký tự đặc biệt code
        if (Pattern.compile("[_::\\(\\)\\[\\]]").matcher(m).find()) {
            searchScore += 1;
        }

        // Câu dài (>= 18 chars) mà không có dấu hiệu REUSE
        if (m.length() >= 18 && reuseScore == 0 && noScore == 0) {
            searchScore += 2;
        }

        // ====== Quyết định ======

        // Nếu không có context thì REUSE khó đúng -> giảm mạnh
        if (!hasPrevContext) {
            reuseScore = Math.max(0, reuseScore - 3);
        }

        // Ưu tiên mạnh: nếu REUSE cao và có context
        if (hasPrevContext && reuseScore >= 3 && reuseScore > searchScore) {
            return ClassifierMode.REUSE;
        }

        // NO_SEARCH nếu điểm cao
        if (noScore >= 3 && noScore > searchScore) {
            return ClassifierMode.NO_SEARCH;
        }

        // SEARCH nếu điểm cao
        if (searchScore >= 3) {
            return ClassifierMode.SEARCH;
        }

        // Mơ hồ -> fallback LLM
        return null;
    }

    private ClassifierMode llmClassifyMode(String message, boolean hasPrevContext, List<String> previousMessages) {
        try {
            String history = (previousMessages == null || previousMessages.isEmpty()) ? "(khong co)"
                    : String.join("\n", previousMessages);

            // Chỉ lấy 2 message cuối cùng
            if (previousMessages != null && previousMessages.size() > 2) {
                history = String.join("\n",
                        previousMessages.subList(previousMessages.size() - 2, previousMessages.size()));
            }

            String prompt = String.format("""
                    Ban la bo phan loai cau hoi cho he thong hoc tap.

                    Ngu canh chat truoc:
                    %s

                    Cau hoi hien tai:
                    %s

                    Chon DUY NHAT 1 nhan:
                    - NO_SEARCH: chao hoi, cam on, giao tiep xa giao, khong can tim tai lieu
                    - REUSE: hoi tiep y cau truoc, muon giai thich them/vi du them, dung lai context cu
                    - SEARCH: hoi kien thuc moi, can tim tai lieu moi

                    Chi tra ve dung 1 nhan (NO_SEARCH hoac REUSE hoac SEARCH). KHONG giai thich.
                    """, history, message);

            // Dùng gemini model vì nhanh và rẻ
            String result = aiModelService.callGeminiModel(prompt);
            String out = result.trim().toUpperCase();

            if (out.contains("NO_SEARCH"))
                return ClassifierMode.NO_SEARCH;
            if (out.contains("REUSE"))
                return ClassifierMode.REUSE;
            if (out.contains("SEARCH"))
                return ClassifierMode.SEARCH;

            return ClassifierMode.SEARCH; // Default safe option

        } catch (Exception e) {
            log.error("LLM classifier failed: {}", e.getMessage());
            return ClassifierMode.SEARCH; // Fallback an toàn nhất
        }
    }
}
