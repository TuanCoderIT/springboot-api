package com.example.springboot_api.services.user;

import java.io.IOException;
import java.sql.Array;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.example.springboot_api.dto.user.chatbot.ChatMode;
import com.example.springboot_api.dto.user.chatbot.ChatRequest;
import com.example.springboot_api.dto.user.chatbot.ChatResponse;
import com.example.springboot_api.dto.user.chatbot.ConversationItem;
import com.example.springboot_api.dto.user.chatbot.FileResponse;
import com.example.springboot_api.dto.user.chatbot.ListConversationsResponse;
import com.example.springboot_api.dto.user.chatbot.ListMessagesResponse;
import com.example.springboot_api.dto.user.chatbot.LlmModelResponse;
import com.example.springboot_api.dto.user.chatbot.ModelResponse;
import com.example.springboot_api.dto.user.chatbot.SourceResponse;
import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookBotConversation;
import com.example.springboot_api.models.NotebookBotConversationState;
import com.example.springboot_api.models.NotebookBotMessage;
import com.example.springboot_api.models.NotebookBotMessageFile;
import com.example.springboot_api.models.NotebookBotMessageSource;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.LlmModelRepository;
import com.example.springboot_api.repositories.shared.NotebookBotConversationRepository;
import com.example.springboot_api.repositories.shared.NotebookBotConversationStateRepository;
import com.example.springboot_api.repositories.shared.NotebookBotMessageFileRepository;
import com.example.springboot_api.repositories.shared.NotebookBotMessageRepository;
import com.example.springboot_api.repositories.shared.NotebookBotMessageSourceRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.EmbeddingService;
import com.example.springboot_api.services.shared.ai.ImageExtractionService;
import com.example.springboot_api.services.shared.ai.OcrService;
import com.example.springboot_api.services.shared.ai.WebSearchService;
import com.example.springboot_api.utils.UrlNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Service xử lý chat bot với RAG, Web Search và Conversation.
 */
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final WebSearchService webSearchService;
    private final OcrService ocrService;
    private final FileStorageService fileStorageService;
    private final AIModelService aiModelService;
    private final ImageExtractionService imageExtractionService;
    private final ObjectMapper objectMapper;
    private final NotebookBotConversationRepository conversationRepository;
    private final NotebookBotConversationStateRepository conversationStateRepository;
    private final NotebookBotMessageRepository messageRepository;
    private final NotebookBotMessageFileRepository messageFileRepository;
    private final NotebookBotMessageSourceRepository messageSourceRepository;
    private final LlmModelRepository llmModelRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final UrlNormalizer urlNormalizer;

    /**
     * Record chứa kết quả RAG chunk.
     */
    public record RagChunk(
            UUID fileId,
            int chunkIndex,
            String content,
            double similarity,
            double distance) {
    }

    /**
     * Truy vấn RAG sử dụng hàm SQL rag_search_chunks.
     * 
     * @param notebookId Notebook ID
     * @param queryText  Text câu hỏi để tạo embedding
     * @param fileIds    Danh sách file IDs để search
     * @return Danh sách RagChunk
     */
    public List<RagChunk> searchRagChunks(UUID notebookId, String queryText, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }

        if (queryText == null || queryText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Tạo embedding từ query text
        List<Double> embedding = embeddingService.embedGoogleNormalized(queryText);

        // Convert embedding sang PostgreSQL vector format
        String vectorString = convertEmbeddingToVectorString(embedding);

        // Gọi SQL function rag_search_chunks
        return jdbcTemplate.query(con -> {
            Array fileIdArray = con.createArrayOf("uuid", fileIds.toArray());

            var ps = con.prepareCall(
                    "SELECT file_id, chunk_index, content, similarity, distance " +
                            "FROM public.rag_search_chunks(?, ?::vector, ?)");

            ps.setObject(1, notebookId);
            ps.setString(2, vectorString); // PostgreSQL sẽ tự convert string sang vector
            ps.setArray(3, fileIdArray);

            return ps;
        }, (rs, rowNum) -> new RagChunk(
                (UUID) rs.getObject("file_id"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("similarity"),
                rs.getDouble("distance")));
    }

    /**
     * Search web sử dụng WebSearchService.
     * 
     * @param queryText Text câu hỏi để search
     * @return WebSearchResult chứa danh sách kết quả web search
     */
    public WebSearchResult searchWeb(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return new WebSearchResult("", 0, Collections.emptyList());
        }

        return webSearchService.search(queryText.trim());
    }

    /**
     * Tạo conversation mới với bot.
     * 
     * @param notebookId Notebook ID
     * @param userId     User ID
     * @param title      Conversation title (optional)
     * @return ConversationItem
     */
    @Transactional
    public ConversationItem createConversation(UUID notebookId, UUID userId, String title) {
        // Validate notebook exists
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new RuntimeException("Notebook not found: " + notebookId));

        // Get user entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create conversation
        NotebookBotConversation conversation = NotebookBotConversation.builder()
                .notebook(notebook)
                .createdBy(user)
                .title(title != null && !title.trim().isEmpty() ? title.trim() : "New Conversation")
                .createdAt(OffsetDateTime.now())
                .build();

        conversation = conversationRepository.save(conversation);

        // Conversation mới tạo chưa có tin nhắn nào
        return new ConversationItem(
                conversation.getId(),
                conversation.getTitle(),
                notebookId,
                conversation.getCreatedAt(),
                null, // updatedAt
                null, // firstMessage - chưa có tin nhắn
                0L); // totalMessages - chưa có tin nhắn
    }

    public ListConversationsResponse listConversations(
            UUID notebookId,
            UUID userId,
            UUID cursorNext) {

        Pageable pageable = PageRequest.of(0, 11); // Lấy 11 để check hasMore

        List<NotebookBotConversation> conversations = conversationRepository
                .findByNotebookIdAndUserIdWithCursor(notebookId, userId, cursorNext, pageable);

        boolean hasMore = conversations.size() > 10;
        if (hasMore) {
            conversations = conversations.subList(0, 10);
        }

        List<ConversationItem> items = conversations.stream()
                .map(conv -> {
                    // Lấy tin nhắn đầu tiên
                    String firstMessage = messageRepository
                            .findByConversationIdOrderByCreatedAtAsc(conv.getId(), PageRequest.of(0, 1))
                            .stream()
                            .findFirst()
                            .map(msg -> msg.getContent())
                            .orElse(null);

                    // Đếm tổng số tin nhắn
                    long totalMessages = messageRepository.countByConversationId(conv.getId());

                    return new ConversationItem(
                            conv.getId(),
                            conv.getTitle(),
                            conv.getNotebook().getId(),
                            conv.getCreatedAt(),
                            null, // updatedAt không có trong DB
                            firstMessage,
                            totalMessages);
                })
                .collect(Collectors.toList());

        String nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getId().toString();

        return new ListConversationsResponse(items, nextCursor, hasMore);
    }

    /**
     * Lấy danh sách các LLM models đang active.
     * Sắp xếp: model mặc định trước, sau đó theo displayName A-Z.
     *
     * @return Danh sách LlmModelResponse
     */
    public List<LlmModelResponse> listModels() {
        List<LlmModel> models = llmModelRepository.findByIsActiveTrueOrderByIsDefaultDescDisplayNameAsc();

        return models.stream()
                .map(model -> new LlmModelResponse(
                        model.getId(),
                        model.getCode(),
                        model.getProvider(),
                        model.getDisplayName(),
                        model.getIsActive(),
                        model.getIsDefault(),
                        model.getMetadata(),
                        model.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Convert List<Double> embedding to PostgreSQL vector string format:
     * "[0.1,0.2,0.3,...]"
     */
    private String convertEmbeddingToVectorString(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    @Transactional
    public ChatResponse chat(UUID notebookId, UUID userId, ChatRequest req, List<MultipartFile> files)
            throws IOException {
        // Normalize files
        if (files == null) {
            files = Collections.emptyList();
        }

        // Validate và lấy các entity cần thiết
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new RuntimeException("Notebook not found: " + notebookId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Lấy conversation (bắt buộc phải có)
        NotebookBotConversation conversation = conversationRepository.findById(req.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + req.getConversationId()));

        // Lấy LlmModel nếu có
        LlmModel llmModel = null;
        if (req.getModelId() != null) {
            llmModel = llmModelRepository.findById(req.getModelId())
                    .orElse(null); // Optional: không throw nếu không tìm thấy
        }

        // Lưu message của user vào database
        NotebookBotMessage userMessage = NotebookBotMessage.builder()
                .notebook(notebook)
                .conversation(conversation)
                .user(user)
                .role("user")
                .content(req.getMessage() != null ? req.getMessage() : "")
                .mode(req.getMode() != null ? req.getMode().name() : null)
                .llmModel(llmModel)
                .createdAt(OffsetDateTime.now())
                .build();

        userMessage = messageRepository.save(userMessage);

        // Cập nhật conversation: updatedAt và title nếu là title mặc định
        updateConversationAfterMessage(conversation, userMessage.getContent());

        // Thu thập OCR text từ danh sách hình ảnh
        StringBuilder imageTextsBuilder = new StringBuilder();
        List<String> fileTypes = new ArrayList<>();
        int imageCount = 0;

        // Xử lý upload và OCR cho danh sách hình ảnh nếu có
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {
                    // Lưu file trước (tham khảo AdminNotebookFileService)
                    String storageUrl = fileStorageService.storeFile(file);

                    if (storageUrl == null || storageUrl.trim().isEmpty()) {
                        continue;
                    }

                    // Thực hiện OCR để lấy text từ hình ảnh
                    String ocrText = ocrService.extract(storageUrl);

                    // Thu thập OCR text để tiền xử lý
                    appendOcrText(ocrText, imageTextsBuilder);

                    // Xác định file type và mime type từ file
                    String fileType = determineFileTypeFromFile(file);
                    String mimeType = file.getContentType();
                    String fileName = file.getOriginalFilename();

                    // Thu thập thông tin file type và đếm số lượng hình ảnh
                    if (fileType != null && !fileType.isEmpty()) {
                        fileTypes.add(fileType);
                        if ("image".equals(fileType)) {
                            imageCount++;
                        }
                    }

                    // Lưu thông tin file và OCR text vào database
                    NotebookBotMessageFile messageFile = NotebookBotMessageFile.builder()
                            .message(userMessage)
                            .fileType(fileType)
                            .fileUrl(storageUrl)
                            .mimeType(mimeType)
                            .fileName(fileName)
                            .ocrText(ocrText)
                            .createdAt(OffsetDateTime.now())
                            .build();

                    messageFileRepository.save(messageFile);
                } catch (Exception e) {
                    // Xử lý lỗi OCR: vẫn lưu file nhưng với OCR text là error message
                    handleOcrError(file, userMessage, fileTypes, e);
                }
            }
        }

        // Tiền xử lý: Tạo 2 biến text từ câu hỏi và text từ hình ảnh
        String queryText = req.getMessage() != null ? req.getMessage().trim() : "";
        String imageTexts = imageTextsBuilder.toString().trim();

        // Bắt buộc phải có câu hỏi
        if (queryText.isEmpty()) {
            throw new BadRequestException("Câu hỏi là bắt buộc. Vui lòng nhập câu hỏi.");
        }

        // Kết hợp queryText và imageTexts thành prompt
        // Format giống chat history để LLM hiểu rõ hình ảnh là câu hỏi bổ sung
        String combinedQueryText;
        if (!imageTexts.isEmpty()) {
            // Có cả câu hỏi và hình ảnh: format như chat history
            combinedQueryText = String.format(
                    "%s\n\n[Câu hỏi bổ sung từ hình ảnh: %s]",
                    queryText, imageTexts);
        } else {
            // Chỉ có câu hỏi
            combinedQueryText = queryText;
        }

        // Biến tổng hợp để lưu tất cả dữ liệu từ các switch case làm đầu vào cho model
        // LLM
        Map<String, Object> llmInputData = new HashMap<>();

        ChatResponse resp = new ChatResponse();

        if (req.getMode() == null) {
            resp.setContent("Mode is required");
            return resp;
        }

        ChatMode actualMode = req.getMode();

        // Lưu mode và queryText chung cho tất cả các case
        llmInputData.put("mode", actualMode.name());
        llmInputData.put("queryText", combinedQueryText);
        llmInputData.put("originalQueryText", queryText);
        llmInputData.put("imageTexts", imageTextsBuilder.toString().trim());

        // Xử lý theo mode
        switch (actualMode) {
            case RAG:
                processRagMode(notebookId, combinedQueryText, req.getRagFileIds(), llmInputData);
                break;

            case WEB:
                processWebMode(combinedQueryText, llmInputData);
                break;

            case HYBRID:
                processHybridMode(notebookId, combinedQueryText, req.getRagFileIds(), llmInputData);
                break;

            case LLM_ONLY:
                // LLM_ONLY mode: Chỉ sử dụng LLM, không cần RAG hay WEB
                // Dữ liệu đã có sẵn trong llmInputData
                break;

            default:
                // Unknown mode - không xử lý gì
                break;
        }

        // Lấy chat history (10 messages mới nhất) để bổ sung ngữ cảnh
        String chatHistory = getChatHistory(conversation.getId(), userId);

        // Chuẩn bị prompt từ llmInputData và chat history để gọi LLM
        String llmPrompt = buildLlmPrompt(llmInputData, chatHistory);

        // Gọi LLM model dựa trên model code
        String llmResponse;
        if (llmModel == null) {
            throw new BadRequestException("Model ID là bắt buộc. Vui lòng chọn model.");
        }

        String modelCode = llmModel.getCode();
        if (modelCode == null || modelCode.trim().isEmpty()) {
            throw new BadRequestException("Model code không hợp lệ.");
        }

        // Gọi model dựa trên code
        if ("gemini".equalsIgnoreCase(modelCode)) {
            llmResponse = aiModelService.callGeminiModel(llmPrompt);
        } else if ("groq".equalsIgnoreCase(modelCode)) {
            llmResponse = aiModelService.callGroqModel(llmPrompt);
        } else {
            throw new BadRequestException(
                    "Model code không được hỗ trợ: " + modelCode
                            + ". Hỗ trợ 'gemini' hoặc 'groq'.");
        }

        // Parse JSON response từ LLM (loại bỏ code block markdown nếu có)
        Map<String, Object> llmResponseJson = parseLlmResponse(llmResponse);

        // Extract answer và sources từ JSON
        Object answerObj = llmResponseJson.get("answer");
        String answer = "";
        if (answerObj != null) {
            if (answerObj instanceof String) {
                answer = (String) answerObj;
            } else {
                // Nếu answer không phải String, convert sang String
                answer = answerObj.toString();
            }
        }

        // Tạo NotebookBotMessage với role = "assistant"
        NotebookBotMessage assistantMessage = NotebookBotMessage.builder()
                .notebook(notebook)
                .conversation(conversation)
                .user(null) // Assistant message không có user
                .role("assistant")
                .content(answer)
                .mode(req.getMode().toString())
                .llmModel(llmModel)
                .createdAt(OffsetDateTime.now())
                .build();

        assistantMessage = messageRepository.save(assistantMessage);

        // Lưu sources vào NotebookBotMessageSource
        saveSources(assistantMessage, llmResponseJson, llmInputData);

        // Flush để đảm bảo dữ liệu được ghi vào database trước khi query lại
        entityManager.flush();

        // Query sources trực tiếp từ database (không cần reload toàn bộ message)
        List<NotebookBotMessageSource> savedSources = messageSourceRepository
                .findByMessageId(assistantMessage.getId());

        // Set tất cả các trường từ NotebookBotMessage
        resp.setId(assistantMessage.getId());
        resp.setContent(answer);
        resp.setMode(actualMode.name());
        resp.setRole(assistantMessage.getRole());
        resp.setContext(assistantMessage.getContext());
        resp.setCreatedAt(assistantMessage.getCreatedAt());
        resp.setMetadata(assistantMessage.getMetadata());

        // Set model object
        if (llmModel != null) {
            resp.setModel(ModelResponse.builder()
                    .id(llmModel.getId())
                    .code(llmModel.getCode())
                    .provider(llmModel.getProvider())
                    .build());
        }

        // Set sources (RAG và WEB) - lấy từ database (đã query ở trên) và llmInputData
        // Convert List sang Set để match với signature của buildSourcesResponse
        Set<NotebookBotMessageSource> sourcesSet = savedSources != null
                ? new LinkedHashSet<>(savedSources)
                : new LinkedHashSet<>();
        resp.setSources(buildSourcesResponse(sourcesSet, llmInputData));

        // Set files (từ user message nếu có) - chỉ query khi user message có files
        if (userMessage.getNotebookBotMessageFiles() != null && !userMessage.getNotebookBotMessageFiles().isEmpty()) {
            // Reload user message với files nếu chưa có
            NotebookBotMessage userMessageWithFiles = userMessage;
            if (userMessage.getNotebookBotMessageFiles().isEmpty()) {
                userMessageWithFiles = messageRepository
                        .findByIdWithSourcesAndFiles(userMessage.getId())
                        .orElse(userMessage);
            }
            resp.setFiles(convertToFileResponseList(userMessageWithFiles.getNotebookBotMessageFiles()));
        } else {
            resp.setFiles(new ArrayList<>());
        }

        return resp;
    }

    public String getChatHistory(UUID conversationId, UUID userId) {
        try {
            // Get 4 messages to ensure we have 2 pairs (2 user + 2 assistant)
            // Use WithFiles to load OCR text from files
            Pageable pageable = PageRequest.of(0, 4);
            List<NotebookBotMessage> recentMessages = messageRepository
                    .findRecentMessagesByConversationIdAndUserIdWithFiles(conversationId, userId, null, pageable);

            if (recentMessages == null || recentMessages.isEmpty()) {
                return "";
            }

            // Sort by time ascending to maintain chronological order
            recentMessages.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

            // Group messages into pairs (user-assistant)
            List<List<NotebookBotMessage>> chatPairs = new ArrayList<>();
            NotebookBotMessage currentUserMessage = null;

            for (NotebookBotMessage msg : recentMessages) {
                if ("user".equalsIgnoreCase(msg.getRole())) {
                    // If we have a previous user message without assistant, add it as incomplete
                    // pair
                    if (currentUserMessage != null) {
                        List<NotebookBotMessage> incompletePair = new ArrayList<>();
                        incompletePair.add(currentUserMessage);
                        chatPairs.add(incompletePair);
                    }
                    currentUserMessage = msg;
                } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                    // Complete pair: user + assistant
                    List<NotebookBotMessage> pair = new ArrayList<>();
                    if (currentUserMessage != null) {
                        pair.add(currentUserMessage);
                    }
                    pair.add(msg);
                    chatPairs.add(pair);
                    currentUserMessage = null;
                }
            }

            // Add last incomplete pair if exists (user without assistant)
            if (currentUserMessage != null) {
                List<NotebookBotMessage> incompletePair = new ArrayList<>();
                incompletePair.add(currentUserMessage);
                chatPairs.add(incompletePair);
            }

            // Get only 2 most recent pairs
            int startIndex = Math.max(0, chatPairs.size() - 2);
            List<List<NotebookBotMessage>> recentPairs = chatPairs.subList(startIndex, chatPairs.size());

            if (recentPairs.isEmpty()) {
                return "";
            }

            // Format into string
            StringBuilder historyBuilder = new StringBuilder();
            int totalPairs = recentPairs.size();

            for (int i = 0; i < recentPairs.size(); i++) {
                List<NotebookBotMessage> pair = recentPairs.get(i);
                boolean isNewest = (i == totalPairs - 1); // Last pair is newest

                for (NotebookBotMessage msg : pair) {
                    String content = msg.getContent() != null ? msg.getContent() : "";
                    String mode = msg.getMode() != null ? msg.getMode() : "";
                    String prefix = "";

                    if ("user".equalsIgnoreCase(msg.getRole())) {
                        // Collect OCR text from files for user messages
                        StringBuilder ocrTextsBuilder = new StringBuilder();
                        if (msg.getNotebookBotMessageFiles() != null && !msg.getNotebookBotMessageFiles().isEmpty()) {
                            for (NotebookBotMessageFile file : msg.getNotebookBotMessageFiles()) {
                                if (file.getOcrText() != null && !file.getOcrText().trim().isEmpty()) {
                                    // Skip error messages from OCR
                                    if (!file.getOcrText().startsWith("OCR failed:")) {
                                        if (ocrTextsBuilder.length() > 0) {
                                            ocrTextsBuilder.append("\n\n");
                                        }
                                        ocrTextsBuilder.append(file.getOcrText());
                                    }
                                }
                            }
                        }

                        // Join OCR text to content if exists
                        String ocrText = ocrTextsBuilder.toString().trim();
                        if (!ocrText.isEmpty()) {
                            if (!content.isEmpty()) {
                                content = String.format("%s\n\n[Câu hỏi bổ sung từ hình ảnh: %s]", content, ocrText);
                            } else {
                                content = String.format("[Câu hỏi từ hình ảnh: %s]", ocrText);
                            }
                        }

                        // Build prefix with mode
                        if (isNewest) {
                            prefix = mode.isEmpty() ? "[MỚI NHẤT] Người dùng: "
                                    : String.format("[MỚI NHẤT] Người dùng [%s]: ", mode);
                        } else {
                            prefix = mode.isEmpty() ? "Người dùng: "
                                    : String.format("Người dùng [%s]: ", mode);
                        }
                    } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                        // Build prefix with mode for assistant
                        if (isNewest) {
                            prefix = mode.isEmpty() ? "[MỚI NHẤT] Trợ lý: "
                                    : String.format("[MỚI NHẤT] Trợ lý [%s]: ", mode);
                        } else {
                            prefix = mode.isEmpty() ? "Trợ lý: "
                                    : String.format("Trợ lý [%s]: ", mode);
                        }
                    } else {
                        continue; // Skip unknown role
                    }

                    // Truncate content if longer than 1000 characters
                    if (content.length() > 1000) {
                        content = content.substring(0, 1000) + "...";
                    }

                    historyBuilder.append(prefix).append(content).append("\n\n");
                }
            }

            return historyBuilder.toString().trim();

        } catch (Exception e) {
            // Return empty string on error
            return "";
        }
    }

    private String buildLlmPrompt(Map<String, Object> llmInputData, String chatHistory) {
        // Convert llmInputData sang JSON string
        String jsonInput;
        try {
            jsonInput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(llmInputData);
        } catch (Exception e) {
            jsonInput = "{}";
        }

        String prompt = String.format(
                """
                        Bạn là trợ lý AI phân tích câu hỏi, tài liệu nội bộ (RAG), kết quả web, và OCR từ hình ảnh.

                        NGUYÊN TẮC:
                        - RAG: kiến thức nền tảng từ tài liệu nội bộ.
                        - WEB: thông tin cập nhật từ internet.
                        - ChatHistory: BẮT BUỘC sử dụng để hiểu ngữ cảnh liên tục, trả lời follow-up questions.
                        - OCR text (imageTexts): QUAN TRỌNG NHẤT khi có hình ảnh. PHẢI dùng từ khóa từ OCR để tìm trong RAG/WEB.
                        - HYBRID: RAG có trọng số cao hơn WEB. Nếu RAG thỏa mãn → RAG chính, WEB bổ sung. Nếu RAG không thỏa mãn → WEB chính, RAG tham khảo.

                        QUAN TRỌNG VỀ OCR TEXT:
                        - queryText format: "câu hỏi chính\n\n[Câu hỏi bổ sung từ hình ảnh: OCR text]"
                        - Khi tìm kiếm: PHẢI dùng CẢ từ khóa từ câu hỏi chính VÀ từ khóa từ OCR text.
                        - Ưu tiên các kết quả có chứa từ khóa từ OCR text.

                        TẠO CODE:
                        - Nếu RAG/WEB có code sẵn → dùng code đó.
                        - Nếu chỉ có mô tả bước/yêu cầu → BẮT BUỘC tạo code dựa trên mô tả, tuân thủ đúng yêu cầu.

                        ------------------------------------------------------------------
                        CHATHISTORY:
                        ------------------------------------------------------------------
                        %s
                        - BẮT BUỘC dùng chatHistory để hiểu follow-up questions và ngữ cảnh liên tục.

                        ------------------------------------------------------------------
                        DỮ LIỆU ĐẦU VÀO:
                        ------------------------------------------------------------------
                        %s

                        CẤU TRÚC:
                        - mode: "RAG" | "WEB" | "HYBRID" | "LLM_ONLY"
                        - queryText: câu hỏi đã kết hợp (có thể có [Câu hỏi bổ sung từ hình ảnh: ...])
                        - originalQueryText: câu hỏi gốc (chỉ text)
                        - imageTexts: OCR text từ hình ảnh
                        - ragChunks: [{file_id, chunk_index, content, similarity, distance}] (RAG/HYBRID)
                        - webResults: [{title, link, snippet, provider, imageUrl}] (WEB/HYBRID)

                        QUY TẮC THEO MODE:

                        1) RAG:
                        - CHỈ dùng ragChunks + chatHistory. KHÔNG dùng webResults.
                        - Khi có imageTexts: PHẢI dùng từ khóa từ OCR text để tìm trong ragChunks.
                        - Nếu không có ragChunks liên quan: "Tôi không tìm thấy thông tin phù hợp trong tài liệu nội bộ."
                        - sources = RAG sources thực sự được dùng.

                        2) WEB:
                        - Dùng webResults + chatHistory. KHÔNG dùng ragChunks.
                        - Nếu yêu cầu code: tạo code dựa trên webResults hoặc mô tả.
                        - ƯU TIÊN hiển thị ảnh: Nếu webResults có imageUrl, PHẢI hiển thị ảnh trong answer bằng markdown ![mô tả](imageUrl).
                        - Hiển thị ảnh từ các webResults có imageUrl để làm rõ nội dung.
                        - sources = WEB sources thực sự được dùng.

                        3) HYBRID:
                        - Có cả RAG và WEB. RAG có trọng số cao hơn.
                        - Nếu RAG thỏa mãn (liên quan, similarity >= 0.7): RAG chính, WEB bổ sung.
                        - Nếu RAG không thỏa mãn: WEB chính, RAG tham khảo.
                        - Khi có imageTexts: PHẢI kiểm tra ragChunks có chứa từ khóa từ OCR text không.
                        - ƯU TIÊN hiển thị ảnh: Nếu webResults có imageUrl, PHẢI hiển thị ảnh trong answer bằng markdown ![mô tả](imageUrl).
                        - Hiển thị ảnh từ các webResults có imageUrl để làm rõ nội dung.
                        - sources = RAG sources (nếu dùng RAG) + WEB sources (nếu dùng WEB).

                        4) LLM_ONLY:
                        - Dùng kiến thức LLM + chatHistory. KHÔNG dùng ragChunks/webResults.
                        - sources = []

                        ------------------------------------------------------------------
                        JSON ĐẦU RA (BẮT BUỘC):
                        ------------------------------------------------------------------
                        {
                          "answer": "<markdown tiếng Việt. Link web: [text](url). Hình ảnh: ![mô tả](url)>",
                          "sources": [
                            {"source_type": "RAG", "file_id": "<uuid>", "chunk_index": <int>, "score": <0.00-1.00>, "provider": "rag"},
                            {"source_type": "WEB", "web_index": <int>, "url": "<url>", "title": "<string>", "snippet": "<string>", "score": <0.00-1.00>, "provider": "<string>"}
                          ]
                        }

                        QUY TẮC NGHIÊM NGẶT:
                        - CHỈ trả về sources THỰC SỰ được dùng. Sort theo score giảm dần.
                        - Link web: Dùng markdown format [text](url). Frontend sẽ tự động mở tab mới khi click.
                        - Hình ảnh: Dùng markdown format ![mô tả](imageUrl). Ưu tiên hiển thị ảnh từ webResults.imageUrl khi có (chế độ WEB/HYBRID).
                        - RAG: file_id, chunk_index từ ragChunks. provider = "rag"
                        - WEB: web_index, url, title, snippet từ webResults[web_index]. provider từ webResults. Nếu có imageUrl, ưu tiên hiển thị trong answer.
                        - score: 0.00-1.00, 2 số sau dấu phẩy
                        - JSON hợp lệ, không có text ngoài JSON, không codeblock markdown (```json hoặc ```).

                        QUAN TRỌNG:
                        - TRẢ VỀ CHỈ JSON, KHÔNG CÓ TEXT NÀO KHÁC TRƯỚC HOẶC SAU JSON.
                        - KHÔNG DÙNG codeblock markdown (```json ... ```).
                        - BẮT ĐẦU NGAY BẰNG DẤU { VÀ KẾT THÚC BẰNG DẤU }.
                        - ĐẢM BẢO JSON HỢP LỆ, CÓ THỂ PARSE ĐƯỢC.
                        """,
                chatHistory != null && !chatHistory.isEmpty() ? chatHistory : "(Không có lịch sử trò chuyện trước đó)",
                jsonInput);

        return prompt;
    }

    /**
     * Xử lý RAG mode: Tìm kiếm RAG chunks và lưu vào llmInputData.
     */
    private void processRagMode(UUID notebookId, String queryText, List<UUID> ragFileIds,
            Map<String, Object> llmInputData) {
        List<RagChunk> ragChunks = searchRagChunks(notebookId, queryText, ragFileIds);
        List<Map<String, Object>> ragChunksJson = convertRagChunksToJson(ragChunks);
        boolean hasRagContext = !ragChunksJson.isEmpty();

        llmInputData.put("ragChunks", ragChunksJson);
        llmInputData.put("hasRagContext", hasRagContext);
    }

    /**
     * Xử lý WEB mode: Tìm kiếm web và lưu vào llmInputData.
     */
    private void processWebMode(String queryText, Map<String, Object> llmInputData) {
        WebSearchResult webResult = searchWeb(queryText);
        List<Map<String, Object>> webResultsJson = convertWebResultToJson(webResult);
        boolean hasWebResults = !webResultsJson.isEmpty();

        llmInputData.put("webResults", webResultsJson);
        llmInputData.put("webQuery", webResult != null ? webResult.query() : queryText);
        llmInputData.put("webSearchTimeMs", webResult != null ? webResult.searchTimeMs() : 0L);
        llmInputData.put("hasWebResults", hasWebResults);
    }

    /**
     * Xử lý HYBRID mode: Kết hợp RAG + WEB.
     */
    private void processHybridMode(UUID notebookId, String queryText, List<UUID> ragFileIds,
            Map<String, Object> llmInputData) {
        // Xử lý RAG
        processRagMode(notebookId, queryText, ragFileIds, llmInputData);
        // Xử lý WEB
        processWebMode(queryText, llmInputData);
    }

    /**
     * Convert List<RagChunk> sang format JSON (List<Map>).
     */
    private List<Map<String, Object>> convertRagChunksToJson(List<RagChunk> ragChunks) {
        if (ragChunks == null || ragChunks.isEmpty()) {
            return new ArrayList<>();
        }

        return ragChunks.stream()
                .map(chunk -> {
                    Map<String, Object> chunkMap = new HashMap<>();
                    chunkMap.put("file_id", chunk.fileId().toString());
                    chunkMap.put("chunk_index", chunk.chunkIndex());
                    chunkMap.put("content", chunk.content());
                    chunkMap.put("similarity", chunk.similarity());
                    chunkMap.put("distance", chunk.distance());
                    return chunkMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert WebSearchResult sang format JSON (List<Map>).
     * Extract hình ảnh từ mỗi URL và thêm vào item.
     */
    private List<Map<String, Object>> convertWebResultToJson(WebSearchResult webResult) {
        if (webResult == null || webResult.items() == null || webResult.items().isEmpty()) {
            return new ArrayList<>();
        }

        return webResult.items().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("title", item.title());
                    itemMap.put("link", item.link());
                    itemMap.put("snippet", item.snippet());
                    itemMap.put("provider", "google"); // Hiện tại chỉ dùng Google Search

                    // Extract hình ảnh từ URL
                    String imageUrl = imageExtractionService.extractImageUrl(item.link());
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        itemMap.put("imageUrl", imageUrl);
                    }

                    return itemMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Xử lý lỗi OCR: lưu file với OCR text là error message.
     */
    private void handleOcrError(MultipartFile file, NotebookBotMessage userMessage,
            List<String> fileTypes, Exception e) {
        String storageUrl = null;
        try {
            storageUrl = fileStorageService.storeFile(file);
        } catch (Exception ex) {
            // Nếu không lưu được file thì skip
            return;
        }

        String fileType = determineFileTypeFromFile(file);
        if (fileType != null && !fileType.isEmpty()) {
            fileTypes.add(fileType);
        }

        NotebookBotMessageFile messageFile = NotebookBotMessageFile.builder()
                .message(userMessage)
                .fileType(fileType)
                .fileUrl(storageUrl)
                .mimeType(file.getContentType())
                .fileName(file.getOriginalFilename())
                .ocrText("OCR failed: " + e.getMessage())
                .createdAt(OffsetDateTime.now())
                .build();

        messageFileRepository.save(messageFile);
    }

    /**
     * Thêm OCR text vào StringBuilder.
     */
    private void appendOcrText(String ocrText, StringBuilder imageTextsBuilder) {
        if (ocrText != null && !ocrText.trim().isEmpty()) {
            if (imageTextsBuilder.length() > 0) {
                imageTextsBuilder.append("\n\n");
            }
            imageTextsBuilder.append(ocrText);
        }
    }

    /**
     * Extract JSON từ response của LLM (có thể có text thêm ngoài JSON hoặc
     * codeblock markdown).
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Empty response from LLM");
        }

        // Loại bỏ codeblock markdown nếu có (```json ... ``` hoặc ``` ... ```)
        String cleaned = response.trim();

        // Loại bỏ markdown codeblock
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf("\n");
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            // Loại bỏ closing ```
            int lastBacktick = cleaned.lastIndexOf("```");
            if (lastBacktick != -1) {
                cleaned = cleaned.substring(0, lastBacktick).trim();
            }
        }

        // Tìm JSON trong response (có thể là object {} hoặc array [])
        String trimmed = cleaned.trim();

        // Tìm vị trí bắt đầu của JSON (object hoặc array)
        int objectStart = trimmed.indexOf("{");
        int arrayStart = trimmed.indexOf("[");

        int startIdx = -1;
        boolean isArray = false;

        if (arrayStart != -1 && (objectStart == -1 || arrayStart < objectStart)) {
            // Array xuất hiện trước hoặc chỉ có array
            startIdx = arrayStart;
            isArray = true;
        } else if (objectStart != -1) {
            // Object xuất hiện trước hoặc chỉ có object
            startIdx = objectStart;
            isArray = false;
        } else {
            throw new RuntimeException("No JSON found in response: " + response);
        }

        // Tìm vị trí kết thúc của JSON (tìm dấu } hoặc ] tương ứng)
        int braceCount = 0;
        int bracketCount = 0;
        int endIdx = -1;
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = startIdx; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (c == '"' && !escapeNext) {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && !isArray) {
                        endIdx = i;
                        break;
                    }
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0 && isArray) {
                        endIdx = i;
                        break;
                    }
                }
            }
        }

        if (endIdx == -1) {
            throw new RuntimeException("Invalid JSON in response: " + response);
        }

        return trimmed.substring(startIdx, endIdx + 1);
    }

    /**
     * Parse JSON response từ LLM (loại bỏ code block markdown nếu có).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseLlmResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            throw new RuntimeException("Empty response from LLM");
        }

        try {
            // Extract JSON từ response (có thể có code block markdown)
            String jsonString = extractJsonFromResponse(llmResponse);

            // Parse JSON
            Map<String, Object> result = objectMapper.readValue(jsonString, Map.class);

            // Validate result có answer và sources
            if (!result.containsKey("answer")) {
                throw new RuntimeException("JSON response missing 'answer' field");
            }
            if (!result.containsKey("sources")) {
                result.put("sources", new ArrayList<>());
            }

            return result;
        } catch (Exception e) {
            // Log error để debug
            System.err.println("Error parsing LLM response: " + e.getMessage());
            System.err.println("Response: " + llmResponse);
            e.printStackTrace();

            // Fallback: trả về answer là toàn bộ response
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", llmResponse);
            fallback.put("sources", new ArrayList<>());
            return fallback;
        }
    }

    /**
     * Lưu sources vào NotebookBotMessageSource.
     */
    @SuppressWarnings("unchecked")
    private void saveSources(NotebookBotMessage assistantMessage, Map<String, Object> llmResponseJson,
            Map<String, Object> llmInputData) {
        Object sourcesObj = llmResponseJson.get("sources");
        if (sourcesObj == null) {
            System.out.println("No sources found in llmResponseJson");
            return;
        }

        List<Map<String, Object>> sources;
        if (sourcesObj instanceof List) {
            sources = (List<Map<String, Object>>) sourcesObj;
        } else {
            System.out.println("Sources is not a List, type: " + sourcesObj.getClass().getName());
            return;
        }

        if (sources.isEmpty()) {
            System.out.println("Sources list is empty");
            return;
        }

        System.out.println("Saving " + sources.size() + " sources");

        // Lấy ragChunks và webResults từ llmInputData để lấy thông tin đầy đủ
        List<Map<String, Object>> ragChunks = (List<Map<String, Object>>) llmInputData.get("ragChunks");
        if (ragChunks == null) {
            ragChunks = new ArrayList<>();
        }

        List<Map<String, Object>> webResults = (List<Map<String, Object>>) llmInputData.get("webResults");
        if (webResults == null) {
            webResults = new ArrayList<>();
        }

        for (Map<String, Object> source : sources) {
            try {
                String sourceType = (String) source.get("source_type");
                if (sourceType == null) {
                    continue;
                }

                Object scoreObj = source.get("score");
                Double score = scoreObj != null
                        ? (scoreObj instanceof Double ? (Double) scoreObj : ((Number) scoreObj).doubleValue())
                        : null;

                NotebookBotMessageSource messageSource;

                if ("RAG".equalsIgnoreCase(sourceType)) {
                    // RAG source
                    String fileIdStr = (String) source.get("file_id");
                    Object chunkIndexObj = source.get("chunk_index");
                    String provider = (String) source.get("provider");

                    if (fileIdStr == null || chunkIndexObj == null) {
                        continue;
                    }

                    UUID fileId = UUID.fromString(fileIdStr);
                    Integer chunkIndex = chunkIndexObj instanceof Integer ? (Integer) chunkIndexObj
                            : ((Number) chunkIndexObj).intValue();

                    messageSource = NotebookBotMessageSource.builder()
                            .message(assistantMessage)
                            .sourceType("RAG")
                            .fileId(fileId)
                            .chunkIndex(chunkIndex)
                            .score(score)
                            .provider(provider != null ? provider : "rag")
                            .createdAt(OffsetDateTime.now())
                            .build();

                } else if ("WEB".equalsIgnoreCase(sourceType)) {
                    // WEB source
                    Object webIndexObj = source.get("web_index");
                    String url = (String) source.get("url");
                    String title = (String) source.get("title");
                    String snippet = (String) source.get("snippet");
                    String provider = (String) source.get("provider");

                    if (webIndexObj == null) {
                        continue;
                    }

                    Integer webIndex = webIndexObj instanceof Integer ? (Integer) webIndexObj
                            : ((Number) webIndexObj).intValue();

                    // Lấy thông tin từ webResults nếu có
                    if (webIndex >= 0 && webIndex < webResults.size()) {
                        Map<String, Object> webResult = webResults.get(webIndex);
                        if (url == null) {
                            url = (String) webResult.get("link");
                        }
                        if (title == null) {
                            title = (String) webResult.get("title");
                        }
                        if (snippet == null) {
                            snippet = (String) webResult.get("snippet");
                        }
                        if (provider == null) {
                            provider = (String) webResult.get("provider");
                        }
                    }

                    messageSource = NotebookBotMessageSource.builder()
                            .message(assistantMessage)
                            .sourceType("WEB")
                            .webIndex(webIndex)
                            .url(url)
                            .title(title)
                            .snippet(snippet)
                            .score(score)
                            .provider(provider != null ? provider : "web")
                            .createdAt(OffsetDateTime.now())
                            .build();

                } else {
                    // Unknown source type - skip
                    continue;
                }

                messageSourceRepository.save(messageSource);
                System.out.println("Saved source: " + sourceType + " for message: " + assistantMessage.getId());
            } catch (Exception e) {
                // Log error nhưng tiếp tục với các sources khác
                System.err.println("Error saving source: " + e.getMessage());
                System.err.println("Source data: " + source);
                e.printStackTrace();
            }
        }

        System.out.println("Finished saving sources");
    }

    /**
     * Xác định file type từ MultipartFile.
     */
    private String determineFileTypeFromFile(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return "image";
        }

        String filename = file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                filename.endsWith(".png") || filename.endsWith(".gif")) {
            return "image";
        } else if (filename.endsWith(".pdf")) {
            return "document";
        } else if (filename.endsWith(".docx") || filename.endsWith(".doc")) {
            return "document";
        }

        return "image"; // default
    }

    /**
     * Xác định file type từ file path/URL.
     */
    private String determineFileType(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "image";
        }

        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image";
        } else if (lowerPath.endsWith(".png")) {
            return "image";
        } else if (lowerPath.endsWith(".gif")) {
            return "image";
        } else if (lowerPath.endsWith(".pdf")) {
            return "document";
        } else if (lowerPath.endsWith(".docx") || lowerPath.endsWith(".doc")) {
            return "document";
        }

        return "image"; // default
    }

    /**
     * Xác định MIME type từ file path/URL.
     */
    private String determineMimeType(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "image/jpeg";
        }

        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerPath.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerPath.endsWith(".doc")) {
            return "application/msword";
        }

        return "image/jpeg"; // default
    }

    /**
     * Extract file name từ file path/URL.
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "unknown";
        }

        // Xử lý cả full URL và relative path
        String path = filePath;
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf("/") + 1);
        }

        // Loại bỏ query parameters nếu có
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }

        return path.isEmpty() ? "unknown" : path;
    }

    /**
     * Build List<SourceResponse> từ database sources và llmInputData (gộp RAG và
     * WEB).
     */
    @SuppressWarnings("unchecked")
    private List<SourceResponse> buildSourcesResponse(Set<NotebookBotMessageSource> dbSources,
            Map<String, Object> llmInputData) {
        List<SourceResponse> allSources = new ArrayList<>();

        if (dbSources == null || dbSources.isEmpty()) {
            return allSources;
        }

        // Lấy ragChunks và webResults từ llmInputData để có thông tin đầy đủ
        List<Map<String, Object>> ragChunks = (List<Map<String, Object>>) llmInputData.get("ragChunks");
        if (ragChunks == null) {
            ragChunks = new ArrayList<>();
        }

        List<Map<String, Object>> webResults = (List<Map<String, Object>>) llmInputData.get("webResults");
        if (webResults == null) {
            webResults = new ArrayList<>();
        }

        for (NotebookBotMessageSource source : dbSources) {
            if ("RAG".equalsIgnoreCase(source.getSourceType())) {
                // Tìm chunk tương ứng trong ragChunks để lấy content, similarity, distance
                Map<String, Object> matchingChunk = ragChunks.stream()
                        .filter(chunk -> source.getFileId() != null
                                && source.getFileId().toString().equals(chunk.get("file_id"))
                                && source.getChunkIndex() != null
                                && source.getChunkIndex().equals(chunk.get("chunk_index")))
                        .findFirst()
                        .orElse(null);

                SourceResponse ragSource = SourceResponse.builder()
                        .sourceType("RAG")
                        .fileId(source.getFileId())
                        .chunkIndex(source.getChunkIndex())
                        .score(source.getScore())
                        .provider(source.getProvider() != null ? source.getProvider() : "rag")
                        .content(matchingChunk != null ? (String) matchingChunk.get("content") : null)
                        .similarity(matchingChunk != null
                                ? (matchingChunk.get("similarity") instanceof Double
                                        ? (Double) matchingChunk.get("similarity")
                                        : matchingChunk.get("similarity") != null
                                                ? ((Number) matchingChunk.get("similarity")).doubleValue()
                                                : null)
                                : null)
                        .distance(matchingChunk != null
                                ? (matchingChunk.get("distance") instanceof Double
                                        ? (Double) matchingChunk.get("distance")
                                        : matchingChunk.get("distance") != null
                                                ? ((Number) matchingChunk.get("distance")).doubleValue()
                                                : null)
                                : null)
                        // WEB fields = null
                        .webIndex(null)
                        .url(null)
                        .title(null)
                        .snippet(null)
                        .imageUrl(null)
                        .favicon(null)
                        .build();

                allSources.add(ragSource);
            } else if ("WEB".equalsIgnoreCase(source.getSourceType())) {
                // Tìm web result tương ứng để lấy imageUrl
                Map<String, Object> matchingWebResult = null;
                if (source.getWebIndex() != null && source.getWebIndex() >= 0
                        && source.getWebIndex() < webResults.size()) {
                    matchingWebResult = webResults.get(source.getWebIndex());
                }

                SourceResponse webSource = SourceResponse.builder()
                        .sourceType("WEB")
                        .webIndex(source.getWebIndex())
                        .url(source.getUrl())
                        .title(source.getTitle())
                        .snippet(source.getSnippet())
                        .score(source.getScore())
                        .provider(source.getProvider() != null ? source.getProvider() : "web")
                        .imageUrl(matchingWebResult != null ? (String) matchingWebResult.get("imageUrl") : null)
                        .favicon(null) // TODO: Extract favicon từ URL nếu cần
                        // RAG fields = null
                        .fileId(null)
                        .chunkIndex(null)
                        .content(null)
                        .similarity(null)
                        .distance(null)
                        .build();

                allSources.add(webSource);
            }
        }

        // Sort theo score giảm dần
        allSources.sort((a, b) -> {
            if (a.getScore() == null && b.getScore() == null) {
                return 0;
            }
            if (a.getScore() == null) {
                return 1;
            }
            if (b.getScore() == null) {
                return -1;
            }
            return Double.compare(b.getScore(), a.getScore());
        });

        return allSources;
    }

    /**
     * Convert Set<NotebookBotMessageFile> sang List<FileResponse>.
     * Normalize fileUrl từ relative path sang full URL.
     */
    private List<FileResponse> convertToFileResponseList(Set<NotebookBotMessageFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .map(file -> {
                    // Normalize fileUrl từ relative path sang full URL
                    String normalizedFileUrl = urlNormalizer.normalizeToFull(file.getFileUrl());

                    return FileResponse.builder()
                            .id(file.getId())
                            .fileType(file.getFileType())
                            .fileUrl(normalizedFileUrl)
                            .mimeType(file.getMimeType())
                            .fileName(file.getFileName())
                            .ocrText(file.getOcrText())
                            .caption(file.getCaption())
                            .metadata(file.getMetadata())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách messages của conversation với cursor pagination.
     * Sắp xếp theo createdAt DESC (mới nhất trước).
     * 
     * @param conversationId Conversation ID
     * @param cursorNext     UUID của message cũ nhất từ lần load trước (null nếu là
     *                       lần đầu)
     * @return ListMessagesResponse
     */
    public ListMessagesResponse listMessages(UUID conversationId, UUID cursorNext) {
        // Validate conversation exists
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        // Lấy 11 messages để check hasMore
        Pageable pageable = PageRequest.of(0, 11);
        List<NotebookBotMessage> messages = messageRepository.findByConversationIdWithCursor(
                conversationId, cursorNext, pageable);

        boolean hasMore = messages.size() > 10;
        if (hasMore) {
            messages = messages.subList(0, 10);
        }

        // Convert sang ChatResponse
        List<ChatResponse> chatResponses = messages.stream()
                .map(this::convertToChatResponse)
                .collect(Collectors.toList());

        // Lấy cursorNext (UUID của message cũ nhất trong response)
        String nextCursor = chatResponses.isEmpty() ? null
                : chatResponses.get(chatResponses.size() - 1).getId().toString();

        return new ListMessagesResponse(chatResponses, nextCursor, hasMore);
    }

    /**
     * Convert NotebookBotMessage sang ChatResponse.
     * 
     * @param message NotebookBotMessage
     * @return ChatResponse
     */
    private ChatResponse convertToChatResponse(NotebookBotMessage message) {
        ChatResponse response = ChatResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .mode(message.getMode())
                .role(message.getRole())
                .context(message.getContext())
                .createdAt(message.getCreatedAt())
                .metadata(message.getMetadata())
                .build();

        // Set model nếu có
        if (message.getLlmModel() != null) {
            response.setModel(ModelResponse.builder()
                    .id(message.getLlmModel().getId())
                    .code(message.getLlmModel().getCode())
                    .provider(message.getLlmModel().getProvider())
                    .build());
        }

        // Set sources (RAG và WEB) - lấy từ database
        // Note: Vì đã eager load sources trong query, nên có thể lấy trực tiếp
        Set<NotebookBotMessageSource> dbSources = message.getNotebookBotMessageSources();
        if (dbSources != null && !dbSources.isEmpty()) {
            // Tạo llmInputData giả để build sources response
            // Trong trường hợp này, ta chỉ cần lấy thông tin từ database
            Map<String, Object> emptyLlmInputData = new HashMap<>();
            emptyLlmInputData.put("ragChunks", new ArrayList<>());
            emptyLlmInputData.put("webResults", new ArrayList<>());
            response.setSources(buildSourcesResponse(dbSources, emptyLlmInputData));
        } else {
            response.setSources(new ArrayList<>());
        }

        // Set files nếu có
        Set<NotebookBotMessageFile> files = message.getNotebookBotMessageFiles();
        if (files != null && !files.isEmpty()) {
            response.setFiles(convertToFileResponseList(files));
        } else {
            response.setFiles(new ArrayList<>());
        }

        return response;
    }

    /**
     * Cập nhật conversation sau khi có message mới:
     * - Cập nhật updatedAt
     * - Đổi title thành câu hỏi đầu tiên nếu title là mặc định và đây là message
     * đầu tiên
     * 
     * @param conversation   Conversation cần cập nhật
     * @param messageContent Nội dung message (câu hỏi)
     */
    private void updateConversationAfterMessage(NotebookBotConversation conversation, String messageContent) {
        boolean needUpdate = false;

        // Cập nhật updatedAt
        conversation.setUpdatedAt(OffsetDateTime.now());
        needUpdate = true;

        // Kiểm tra nếu title là mặc định và đây là message đầu tiên thì đổi thành câu
        // hỏi
        String currentTitle = conversation.getTitle();
        if (currentTitle != null && isDefaultTitle(currentTitle)) {
            // Kiểm tra xem đây có phải là message đầu tiên không
            long messageCount = messageRepository.countByConversationId(conversation.getId());
            // Nếu chỉ có 1 message (message vừa tạo) thì đây là message đầu tiên
            if (messageCount == 1) {
                // Lấy câu hỏi đầu tiên (truncate nếu quá dài)
                String newTitle = extractTitleFromMessage(messageContent);
                if (newTitle != null && !newTitle.trim().isEmpty()) {
                    conversation.setTitle(newTitle);
                    needUpdate = true;
                }
            }
        }

        // Save conversation nếu có thay đổi
        if (needUpdate) {
            conversationRepository.save(conversation);
        }
    }

    /**
     * Kiểm tra xem title có phải là title mặc định không.
     * 
     * @param title Title cần kiểm tra
     * @return true nếu là title mặc định
     */
    private boolean isDefaultTitle(String title) {
        if (title == null) {
            return false;
        }

        String lowerTitle = title.trim().toLowerCase();
        // Các title mặc định có thể có
        return lowerTitle.equals("new conversation") ||
                lowerTitle.equals("new chat") ||
                lowerTitle.equals("cuộc trò chuyện mới") ||
                lowerTitle.equals("cuộc hội thoại mới") ||
                lowerTitle.startsWith("new conversation") ||
                lowerTitle.startsWith("new chat");
    }

    /**
     * Trích xuất title từ nội dung message (câu hỏi).
     * Truncate nếu quá dài (tối đa 100 ký tự).
     * 
     * @param messageContent Nội dung message
     * @return Title đã được truncate
     */
    private String extractTitleFromMessage(String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return null;
        }

        // Loại bỏ whitespace ở đầu và cuối
        String trimmed = messageContent.trim();

        // Loại bỏ markdown formatting nếu có (**, __, etc.)
        trimmed = trimmed.replaceAll("\\*\\*|__", "");

        // Truncate nếu quá dài (tối đa 100 ký tự)
        int maxLength = 100;
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength).trim();
            // Thêm "..." nếu bị truncate
            if (trimmed.length() == maxLength) {
                trimmed += "...";
            }
        }

        return trimmed;
    }

    /**
     * Set conversation thành active cho user trong notebook.
     * Nếu đã có state thì update, nếu chưa có thì tạo mới.
     * 
     * @param notebookId     Notebook ID
     * @param userId         User ID
     * @param conversationId Conversation ID cần set active
     * @return ConversationItem của conversation đã được set active
     */
    @Transactional
    public ConversationItem setActiveConversation(UUID notebookId, UUID userId, UUID conversationId) {
        // Validate notebook
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new BadRequestException("Notebook không tồn tại."));

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại."));

        // Validate conversation
        NotebookBotConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BadRequestException("Conversation không tồn tại."));

        // Kiểm tra conversation thuộc về notebook
        if (!conversation.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("Conversation không thuộc về notebook này.");
        }

        // Tìm hoặc tạo conversation state
        NotebookBotConversationState state = conversationStateRepository
                .findByUserIdAndNotebookId(userId, notebookId)
                .orElse(null);

        if (state == null) {
            // Tạo mới
            state = NotebookBotConversationState.builder()
                    .user(user)
                    .notebook(notebook)
                    .conversation(conversation)
                    .lastOpenedAt(OffsetDateTime.now())
                    .build();
        } else {
            // Update existing
            state.setConversation(conversation);
            state.setLastOpenedAt(OffsetDateTime.now());
        }

        conversationStateRepository.save(state);

        // Convert và return ConversationItem
        return new ConversationItem(
                conversation.getId(),
                conversation.getTitle(),
                notebookId,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                null, // firstMessage - không cần trong active conversation
                null); // totalMessages - không cần trong active conversation
    }

    /**
     * Lấy conversation đang active cho user trong notebook.
     * 
     * @param notebookId Notebook ID
     * @param userId     User ID
     * @return ConversationItem của conversation đang active, hoặc null nếu chưa có
     */
    public ConversationItem getActiveConversation(UUID notebookId, UUID userId) {
        Optional<NotebookBotConversationState> stateOpt = conversationStateRepository
                .findByUserIdAndNotebookId(userId, notebookId);

        if (stateOpt.isEmpty()) {
            return null;
        }

        NotebookBotConversationState state = stateOpt.get();
        NotebookBotConversation conversation = state.getConversation();

        // Kiểm tra conversation vẫn còn tồn tại
        if (conversation == null) {
            return null;
        }

        // Convert và return ConversationItem
        return new ConversationItem(
                conversation.getId(),
                conversation.getTitle(),
                notebookId,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                null, // firstMessage - không cần trong active conversation
                null); // totalMessages - không cần trong active conversation
    }

    /**
     * Xóa conversation theo ID.
     * Chỉ người tạo conversation mới được xóa.
     * 
     * @param notebookId     Notebook ID
     * @param userId         User ID (phải là người tạo conversation)
     * @param conversationId Conversation ID cần xóa
     * @throws NotFoundException   nếu conversation không tồn tại
     * @throws BadRequestException nếu không có quyền xóa hoặc conversation không
     *                             thuộc notebook
     */
    @Transactional
    public void deleteConversation(UUID notebookId, UUID userId, UUID conversationId) {
        // Validate notebook tồn tại
        notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại."));

        // Validate conversation
        NotebookBotConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation không tồn tại."));

        // Kiểm tra conversation thuộc về notebook
        if (!conversation.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("Conversation không thuộc về notebook này.");
        }

        // Kiểm tra quyền: chỉ người tạo conversation mới được xóa
        if (conversation.getCreatedBy() == null) {
            throw new BadRequestException("Không thể xóa conversation này. Conversation không có thông tin người tạo.");
        }

        if (!conversation.getCreatedBy().getId().equals(userId)) {
            throw new BadRequestException("Bạn chỉ có thể xóa conversation của chính mình.");
        }

        // Kiểm tra xem conversation này có đang active không
        boolean isActive = conversationStateRepository.findByUserIdAndNotebookId(userId, notebookId)
                .map(state -> state.getConversation() != null && state.getConversation().getId().equals(conversationId))
                .orElse(false);

        // Xóa files từ storage trước (nếu có)
        List<NotebookBotMessageFile> files = messageFileRepository.findByConversationId(conversationId);
        for (NotebookBotMessageFile file : files) {
            if (file.getFileUrl() != null && !file.getFileUrl().isEmpty()) {
                fileStorageService.deleteFile(file.getFileUrl());
            }
        }

        // Xóa active state nếu conversation đang active
        if (isActive) {
            conversationStateRepository.findByUserIdAndNotebookId(userId, notebookId)
                    .ifPresent(conversationStateRepository::delete);
        }

        // Xóa conversation (cascade sẽ tự động xóa messages, sources, files)
        conversationRepository.delete(conversation);

        // Nếu conversation đang active, tìm conversation mới nhất và set active
        if (isActive) {
            // Tìm conversation mới nhất (theo updatedAt) của user trong notebook
            // (conversation vừa xóa đã không còn trong DB nên không cần exclude)
            List<NotebookBotConversation> latestConversations = conversationRepository
                    .findLatestByNotebookIdAndUserId(
                            notebookId,
                            userId,
                            PageRequest.of(0, 1));

            if (!latestConversations.isEmpty()) {
                // Set conversation mới nhất thành active
                NotebookBotConversation latestConversation = latestConversations.get(0);
                NotebookBotConversationState state = conversationStateRepository
                        .findByUserIdAndNotebookId(userId, notebookId)
                        .orElse(null);

                if (state == null) {
                    // Tạo mới state
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User không tồn tại."));
                    Notebook notebook = notebookRepository.findById(notebookId)
                            .orElseThrow(() -> new RuntimeException("Notebook không tồn tại."));
                    state = NotebookBotConversationState.builder()
                            .user(user)
                            .notebook(notebook)
                            .conversation(latestConversation)
                            .lastOpenedAt(OffsetDateTime.now())
                            .build();
                } else {
                    // Update existing state
                    state.setConversation(latestConversation);
                    state.setLastOpenedAt(OffsetDateTime.now());
                }
                conversationStateRepository.save(state);
            } else {
                // Không còn conversation nào, xóa state nếu có
                conversationStateRepository.findByUserIdAndNotebookId(userId, notebookId)
                        .ifPresent(conversationStateRepository::delete);
            }
        }
    }
}
