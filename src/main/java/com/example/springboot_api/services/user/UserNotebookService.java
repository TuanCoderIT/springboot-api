package com.example.springboot_api.services.user;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.shared.ai.WebSearchItem;
import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.example.springboot_api.dto.user.notebook.CreatePersonalNotebookRequest;
import com.example.springboot_api.dto.user.notebook.MyMembershipResponse;
import com.example.springboot_api.dto.user.notebook.NotebookMemberItem;
import com.example.springboot_api.dto.user.notebook.NotebookMembersResponse;
import com.example.springboot_api.dto.user.notebook.PersonalNotebookResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.WebSearchService;
import com.example.springboot_api.utils.UrlNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserNotebookService {

    private static final Logger log = LoggerFactory.getLogger(UserNotebookService.class);
    private static final int MIN_WORDS_FOR_AUTO_GENERATE = 10;

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final UrlNormalizer urlNormalizer;
    private final WebSearchService webSearchService;
    private final AIModelService aiModelService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo notebook cá nhân mới - Hỗ trợ 2 mode:
     * 
     * MODE 1 (Manual): autoGenerate = false
     * - Yêu cầu title (bắt buộc) + thumbnail (bắt buộc)
     * - description (không bắt buộc)
     * 
     * MODE 2 (Auto): autoGenerate = true
     * - Yêu cầu description (bắt buộc, ≥10 từ)
     * - Hệ thống search web + call LLM để generate:
     * + title
     * + description (markdown chi tiết)
     * + imageUrl
     */
    @Transactional
    public PersonalNotebookResponse createPersonalNotebook(
            CreatePersonalNotebookRequest req,
            MultipartFile thumbnail,
            UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        String title;
        String description;
        String thumbnailUrl;

        if (req.isAutoGenerate()) {
            // MODE 2: Auto-generate bằng AI
            AiGeneratedContent aiContent = handleAutoGenerate(req.description());
            title = aiContent.title();
            description = aiContent.description();
            thumbnailUrl = aiContent.imageUrl();
        } else {
            // MODE 1: Manual
            ManualContent manualContent = handleManual(req, thumbnail);
            title = manualContent.title();
            description = req.description();
            thumbnailUrl = manualContent.thumbnailUrl();
        }

        Notebook notebook = new Notebook();
        notebook.setTitle(title);
        notebook.setDescription(description);
        notebook.setType("personal");
        notebook.setVisibility("private");
        notebook.setCreatedBy(user);
        notebook.setThumbnailUrl(thumbnailUrl);
        notebook.setCreatedAt(OffsetDateTime.now());
        notebook.setUpdatedAt(OffsetDateTime.now());

        Notebook saved = notebookRepository.save(notebook);

        // Tự động thêm user làm owner của notebook
        NotebookMember ownerMember = NotebookMember.builder()
                .notebook(saved)
                .user(user)
                .role("owner")
                .status("approved")
                .joinedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        memberRepository.save(ownerMember);

        return mapToResponse(saved);
    }

    /**
     * Xử lý MODE 1: Manual input
     * - title bắt buộc
     * - thumbnail bắt buộc
     */
    private ManualContent handleManual(CreatePersonalNotebookRequest req, MultipartFile thumbnail) {
        // Validate title
        if (req.title() == null || req.title().trim().isEmpty()) {
            throw new BadRequestException("Tiêu đề là bắt buộc khi tạo notebook thủ công");
        }

        // Validate thumbnail
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new BadRequestException("Thumbnail là bắt buộc khi tạo notebook thủ công");
        }

        try {
            String thumbnailUrl = fileStorageService.storeFile(thumbnail);
            return new ManualContent(req.title().trim(), thumbnailUrl);
        } catch (IOException e) {
            throw new RuntimeException("Không thể upload thumbnail", e);
        }
    }

    /**
     * Xử lý MODE 2: Auto-generate bằng AI
     * 1. Validate description (≥10 từ)
     * 2. Search web để lấy context
     * 3. Call LLM với context để generate title, description (markdown), imageUrl
     */
    private AiGeneratedContent handleAutoGenerate(String userInput) {
        // Validate description
        if (userInput == null || userInput.trim().isEmpty()) {
            throw new BadRequestException("Mô tả là bắt buộc khi sử dụng chế độ tự động tạo");
        }

        String[] words = userInput.trim().split("\\s+");
        if (words.length < MIN_WORDS_FOR_AUTO_GENERATE) {
            throw new BadRequestException(
                    String.format("Mô tả phải có ít nhất %d từ để sử dụng chế độ tự động tạo (hiện tại: %d từ)",
                            MIN_WORDS_FOR_AUTO_GENERATE, words.length));
        }

        try {
            // 1. Search web để lấy context
            log.info("Searching web for context: {}", userInput);
            WebSearchResult searchResult = webSearchService.search(userInput);

            // 2. Tạo context từ kết quả search
            String webContext = buildWebContext(searchResult);

            // 3. Tạo prompt cho LLM
            String prompt = buildAiPrompt(userInput, webContext);

            // 4. Call LLM
            log.info("Calling AI to generate notebook content...");
            String llmResponse = aiModelService.callGeminiModel(prompt);

            // 5. Parse response
            return parseAiResponse(llmResponse, userInput);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating notebook content with AI", e);
            throw new BadRequestException("Có lỗi khi tạo nội dung tự động. Vui lòng thử lại hoặc tạo thủ công.");
        }
    }

    /**
     * Tạo context từ kết quả web search
     */
    private String buildWebContext(WebSearchResult searchResult) {
        if (searchResult == null || searchResult.items() == null || searchResult.items().isEmpty()) {
            return "Không có thông tin bổ sung từ web.";
        }

        StringBuilder context = new StringBuilder();
        int maxItems = Math.min(5, searchResult.items().size());

        for (int i = 0; i < maxItems; i++) {
            WebSearchItem item = searchResult.items().get(i);
            context.append(String.format("- %s: %s (URL: %s)\n",
                    item.title(),
                    item.snippet() != null ? item.snippet() : "",
                    item.link()));
        }

        return context.toString();
    }

    /**
     * Tạo prompt cho LLM để generate notebook content
     */
    private String buildAiPrompt(String userInput, String webContext) {
        return """
                Bạn là một trợ lý AI giúp tạo notebook cá nhân. Dựa trên yêu cầu của người dùng và thông tin từ web, hãy tạo nội dung cho notebook.

                YÊU CẦU CỦA NGƯỜI DÙNG:
                %s

                THÔNG TIN BỔ SUNG TỪ WEB:
                %s

                HÃY TRẢ VỀ JSON VỚI CẤU TRÚC SAU (CHỈ JSON, KHÔNG CÓ MARKDOWN CODE BLOCK):
                {
                    "title": "Tiêu đề ngắn gọn, hấp dẫn (tối đa 100 ký tự)",
                    "description": "Mô tả chi tiết bằng Markdown. Bao gồm: tổng quan, các điểm chính, lợi ích. Độ dài 200-500 từ.",
                    "imageUrl": "URL hình ảnh liên quan (lấy từ kết quả web search nếu có, hoặc để null nếu không tìm được)"
                }

                LƯU Ý:
                - title: Ngắn gọn, súc tích, thu hút
                - description: Viết bằng Markdown, có heading, bullet points, định dạng đẹp
                - imageUrl: Ưu tiên lấy từ URL trong kết quả web search, phải là URL hình ảnh hợp lệ (jpg, png, webp). Nếu không có, trả về null
                """
                .formatted(userInput, webContext);
    }

    /**
     * Parse response từ LLM
     */
    private AiGeneratedContent parseAiResponse(String llmResponse, String userInput) {
        try {
            // Clean response - loại bỏ markdown code block nếu có
            String cleanJson = llmResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode json = objectMapper.readTree(cleanJson);

            String title = json.has("title") && !json.get("title").isNull()
                    ? json.get("title").asText()
                    : generateFallbackTitle(userInput);

            String description = json.has("description") && !json.get("description").isNull()
                    ? json.get("description").asText()
                    : userInput;

            String imageUrl = null;
            if (json.has("imageUrl") && !json.get("imageUrl").isNull()) {
                String url = json.get("imageUrl").asText();
                if (url != null && !url.isEmpty() && !url.equals("null")) {
                    imageUrl = url;
                }
            }

            return new AiGeneratedContent(title, description, imageUrl);

        } catch (Exception e) {
            log.warn("Failed to parse AI response, using fallback: {}", e.getMessage());
            // Fallback nếu không parse được JSON
            return new AiGeneratedContent(
                    generateFallbackTitle(userInput),
                    userInput,
                    null);
        }
    }

    /**
     * Tạo title fallback từ user input
     */
    private String generateFallbackTitle(String userInput) {
        String trimmed = userInput.trim();
        if (trimmed.length() <= 100) {
            return trimmed;
        }
        String truncated = trimmed.substring(0, 100);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0) {
            return truncated.substring(0, lastSpace) + "...";
        }
        return truncated + "...";
    }

    /**
     * Xóa notebook cá nhân
     * - Chỉ owner mới được phép xóa
     */
    @Transactional
    public void deletePersonalNotebook(UUID notebookId, UUID userId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        // Kiểm tra quyền owner
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập notebook này"));

        if (!"owner".equals(member.getRole())) {
            throw new ForbiddenException("Chỉ chủ sở hữu mới có thể xóa notebook");
        }

        if (!"personal".equals(notebook.getType())) {
            throw new BadRequestException("Đây không phải là notebook cá nhân");
        }

        // Xóa thumbnail nếu có (chỉ xóa nếu là file local)
        String thumbnailUrl = notebook.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            fileStorageService.deleteFile(thumbnailUrl);
        }

        notebookRepository.deleteById(notebookId);
    }

    /**
     * Lấy danh sách notebook cá nhân của user
     */
    public PagedResponse<PersonalNotebookResponse> getMyPersonalNotebooks(
            UUID userId,
            String keyword,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        String sortByField = Optional.ofNullable(sortBy).orElse("createdAt");
        String sortDirection = Optional.ofNullable(sortDir).orElse("desc");

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortByField).ascending()
                : Sort.by(sortByField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Notebook> result = notebookRepository.findPersonalNotebooksByUserId(userId, q, pageable);

        List<PersonalNotebookResponse> content = result.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PagedResponse<>(
                content,
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    private PersonalNotebookResponse mapToResponse(Notebook notebook) {
        Long fileCount = fileRepository.countByNotebookId(notebook.getId());
        String thumbnailUrl = notebook.getThumbnailUrl();

        // Chỉ normalize nếu là file local
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            thumbnailUrl = urlNormalizer.normalizeToFull(thumbnailUrl);
        }

        return new PersonalNotebookResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getType(),
                notebook.getVisibility(),
                thumbnailUrl,
                fileCount,
                notebook.getCreatedAt(),
                notebook.getUpdatedAt());
    }

    /**
     * Lấy thông tin quyền của user trong notebook
     */
    public MyMembershipResponse getMyMembership(UUID notebookId, UUID userId) {
        // Kiểm tra notebook tồn tại
        notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        // Lấy thông tin membership
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotFoundException("Bạn không phải thành viên của notebook này"));

        return MyMembershipResponse.of(
                member.getId(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt());
    }

    /**
     * Lấy danh sách thành viên của notebook với cursor-based pagination
     * - Chỉ trả về members có status = approved
     * - Hỗ trợ tìm kiếm theo tên/email
     * - Phân trang bằng cursor (dựa trên joinedAt)
     */
    public NotebookMembersResponse getNotebookMembers(
            UUID notebookId,
            UUID userId,
            String keyword,
            String cursor,
            int limit) {

        // Kiểm tra notebook tồn tại
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        // Kiểm tra user có quyền truy cập notebook
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập notebook này"));

        if (!"approved".equals(member.getStatus())) {
            throw new ForbiddenException("Bạn chưa được duyệt vào notebook này");
        }

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;

        // Parse cursor thành OffsetDateTime
        OffsetDateTime cursorJoinedAt = null;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                cursorJoinedAt = OffsetDateTime.parse(cursor);
            } catch (Exception e) {
                throw new BadRequestException("Cursor không hợp lệ");
            }
        }

        // Lấy limit + 1 để biết có còn dữ liệu không
        Pageable pageable = PageRequest.of(0, limit + 1);

        // Gọi query phù hợp dựa trên có cursor hay không
        List<NotebookMember> members;
        if (cursorJoinedAt == null) {
            members = memberRepository.findMembersFirstPage(notebookId, q, pageable);
        } else {
            members = memberRepository.findMembersAfterCursor(notebookId, q, cursorJoinedAt, pageable);
        }

        boolean hasMore = members.size() > limit;
        if (hasMore) {
            members = members.subList(0, limit);
        }

        // Đếm tổng số members
        long total = memberRepository.countMembersWithSearch(notebookId, q);

        // Map to response
        List<NotebookMemberItem> items = members.stream()
                .map(this::mapToMemberItem)
                .toList();

        // cursorNext = joinedAt của member cuối cùng
        String cursorNext = null;
        if (!members.isEmpty() && hasMore) {
            NotebookMember lastMember = members.get(members.size() - 1);
            cursorNext = lastMember.getJoinedAt().toString();
        }

        return new NotebookMembersResponse(items, cursorNext, hasMore, total);
    }

    private NotebookMemberItem mapToMemberItem(NotebookMember member) {
        User user = member.getUser();
        String avatarUrl = user.getAvatarUrl();

        // Normalize avatar URL nếu là file local
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = urlNormalizer.normalizeToFull(avatarUrl);
        }

        return new NotebookMemberItem(
                member.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                avatarUrl,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt());
    }

    // Internal records
    private record ManualContent(String title, String thumbnailUrl) {
    }

    private record AiGeneratedContent(String title, String description, String imageUrl) {
    }
}
