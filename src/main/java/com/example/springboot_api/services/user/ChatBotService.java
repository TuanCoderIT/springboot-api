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

        // Kết hợp queryText và imageTexts thành prompt với trọng số
        // Trọng số câu hỏi cao hơn trọng số hình ảnh một chút
        String combinedQueryText;
        if (!imageTexts.isEmpty()) {
            // Có cả câu hỏi và hình ảnh: câu hỏi là chính, hình ảnh là bổ sung
            combinedQueryText = String.format(
                    "Câu hỏi chính: %s\n\nThông tin bổ sung từ hình ảnh (trọng số thấp hơn): %s",
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
        if (req.getMode() == ChatMode.AUTO) {
            // Thu thập thông tin đầy đủ cho router
            String imagesText = imageTextsBuilder.toString().trim();
            List<String> uniqueFileTypes = fileTypes.stream().distinct().collect(Collectors.toList());

            // Lấy chat history để hiểu ngữ cảnh (không exclude message hiện tại vì chưa
            // được tạo)
            String chatHistoryForRouter = getChatHistory(conversation.getId(), userId, null);

            actualMode = determineModeFromGemini(queryText, imagesText, uniqueFileTypes, imageCount,
                    chatHistoryForRouter);
        }

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
        String chatHistory = getChatHistory(conversation.getId(), userId, userMessage.getId());

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
                    "Model code không được hỗ trợ: " + modelCode + ". Chỉ hỗ trợ 'gemini' hoặc 'groq'.");
        }

        // Parse JSON response từ LLM (loại bỏ code block markdown nếu có)
        Map<String, Object> llmResponseJson = parseLlmResponse(llmResponse);

        // Extract answer và sources từ JSON
        String answer = (String) llmResponseJson.get("answer");
        if (answer == null) {
            answer = "";
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

    /**
     * Lấy chat history (tối đa 20 messages để đảm bảo có đủ 5 user + 5 assistant)
     * và format thành string.
     * Lấy 5 câu hỏi của user và 5 câu trả lời của assistant.
     * 
     * @param conversationId   Conversation ID
     * @param userId           User ID
     * @param excludeMessageId Message ID hiện tại (để loại trừ)
     * @return Formatted chat history string
     */
    private String getChatHistory(UUID conversationId, UUID userId, UUID excludeMessageId) {
        try {
            // Lấy 20 messages mới nhất để đảm bảo có đủ 5 user + 5 assistant
            // Sử dụng query với files để lấy OCR text
            Pageable pageable = PageRequest.of(0, 20);
            List<NotebookBotMessage> recentMessages = messageRepository
                    .findRecentMessagesByConversationIdAndUserIdWithFiles(conversationId, userId, excludeMessageId,
                            pageable);

            if (recentMessages == null || recentMessages.isEmpty()) {
                return "";
            }

            // Phân loại thành user messages và assistant messages
            List<NotebookBotMessage> userMessages = new ArrayList<>();
            List<NotebookBotMessage> assistantMessages = new ArrayList<>();

            for (NotebookBotMessage msg : recentMessages) {
                if ("user".equalsIgnoreCase(msg.getRole())) {
                    userMessages.add(msg);
                } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                    assistantMessages.add(msg);
                }
            }

            // Lấy 5 câu hỏi gần nhất (sắp xếp theo thời gian tăng dần để giữ thứ tự)
            List<NotebookBotMessage> recentUserMessages = userMessages.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Lấy 5 câu trả lời gần nhất (sắp xếp theo thời gian tăng dần để giữ thứ tự)
            List<NotebookBotMessage> recentAssistantMessages = assistantMessages.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Nếu không có message nào thì trả về empty
            if (recentUserMessages.isEmpty() && recentAssistantMessages.isEmpty()) {
                return "";
            }

            // Format thành string
            StringBuilder historyBuilder = new StringBuilder();

            // Ghép cặp user-assistant theo thứ tự thời gian
            List<NotebookBotMessage> allMessages = new ArrayList<>();
            allMessages.addAll(recentUserMessages);
            allMessages.addAll(recentAssistantMessages);
            allMessages.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

            for (NotebookBotMessage msg : allMessages) {
                // Lấy nội dung text của message
                String messageContent = msg.getContent() != null ? msg.getContent() : "";

                // Thu thập OCR text từ tất cả các files của message
                StringBuilder ocrTextsBuilder = new StringBuilder();
                if (msg.getNotebookBotMessageFiles() != null && !msg.getNotebookBotMessageFiles().isEmpty()) {
                    for (NotebookBotMessageFile file : msg.getNotebookBotMessageFiles()) {
                        if (file.getOcrText() != null && !file.getOcrText().trim().isEmpty()) {
                            // Bỏ qua các error message từ OCR
                            if (!file.getOcrText().startsWith("OCR failed:")) {
                                if (ocrTextsBuilder.length() > 0) {
                                    ocrTextsBuilder.append("\n\n");
                                }
                                ocrTextsBuilder.append(file.getOcrText());
                            }
                        }
                    }
                }

                // Kết hợp nội dung message và OCR text
                String fullContent = messageContent;
                if (ocrTextsBuilder.length() > 0) {
                    String ocrText = ocrTextsBuilder.toString().trim();

                    // Format khác nhau cho user message và assistant message
                    if ("user".equalsIgnoreCase(msg.getRole())) {
                        // Với user message: OCR text là câu hỏi bổ sung từ hình ảnh
                        if (!messageContent.isEmpty()) {
                            fullContent = messageContent + "\n\n[Câu hỏi bổ sung từ hình ảnh: " + ocrText + "]";
                        } else {
                            fullContent = "[Câu hỏi từ hình ảnh: " + ocrText + "]";
                        }
                    } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                        // Với assistant message: OCR text là thông tin tham khảo (nếu có)
                        if (!messageContent.isEmpty()) {
                            fullContent = messageContent + "\n\n[Thông tin từ hình ảnh: " + ocrText + "]";
                        } else {
                            fullContent = "[Thông tin từ hình ảnh: " + ocrText + "]";
                        }
                    }
                }

                if ("user".equalsIgnoreCase(msg.getRole())) {
                    historyBuilder.append("Người dùng: ").append(fullContent).append("\n\n");
                } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                    historyBuilder.append("Trợ lý: ").append(fullContent).append("\n\n");
                }
            }

            return historyBuilder.toString().trim();

        } catch (Exception e) {
            // Nếu có lỗi thì trả về empty string
            return "";
        }
    }

    /**
     * Xây dựng prompt từ llmInputData và chat history để gửi cho LLM model.
     * 
     * Ví dụ JSON output của llmInputData với dữ liệu thật từ database:
     * 
     * MODE = "RAG":
     * {
     * "mode": "RAG",
     * "queryText": "Câu hỏi chính: Hãy giải thích về bài thực hành 6\n\nThông tin
     * bổ sung từ hình ảnh (trọng số thấp hơn): Phương trình 2x + 3 = 9",
     * "originalQueryText": "Hãy giải thích về bài thực hành 6",
     * "imageTexts": "Phương trình 2x + 3 = 9",
     * "ragChunks": [
     * {
     * "file_id": "987d2245-0466-4bc8-8a52-3a956e647e9a",
     * "chunk_index": 0,
     * "content": "BÀI THỰC HÀNH 6\nGV: Nguyễn Thị Minh Tâm 1. Đọc ảnh có các đường
     * nét...",
     * "similarity": 0.92,
     * "distance": 0.08
     * },
     * {
     * "file_id": "987d2245-0466-4bc8-8a52-3a956e647e9a",
     * "chunk_index": 1,
     * "content": "ền.\n· Lưu ảnh kết quả có đối tượng được tách khỏi nền...",
     * "similarity": 0.85,
     * "distance": 0.15
     * }
     * ],
     * "hasRagContext": true,
     * "hasWebResults": false
     * }
     * 
     * MODE = "WEB":
     * {
     * "mode": "WEB",
     * "queryText": "Tin tức mới nhất về AI năm 2025",
     * "originalQueryText": "Tin tức mới nhất về AI năm 2025",
     * "imageTexts": "",
     * "webResults": [
     * {
     * "title": "AI News 2025 - Latest Updates",
     * "link": "https://example.com/ai-news-2025",
     * "snippet": "The latest AI developments in 2025 include...",
     * "provider": "google"
     * },
     * {
     * "title": "Artificial Intelligence Trends 2025",
     * "link": "https://example.com/ai-trends",
     * "snippet": "Key trends shaping AI in 2025...",
     * "provider": "google"
     * }
     * ],
     * "hasRagContext": false,
     * "hasWebResults": true
     * }
     * 
     * MODE = "HYBRID":
     * {
     * "mode": "HYBRID",
     * "queryText": "So sánh bài thực hành 6 với các phương pháp AI hiện đại",
     * "originalQueryText": "So sánh bài thực hành 6 với các phương pháp AI hiện
     * đại",
     * "imageTexts": "",
     * "ragChunks": [
     * {
     * "file_id": "987d2245-0466-4bc8-8a52-3a956e647e9a",
     * "chunk_index": 0,
     * "content": "BÀI THỰC HÀNH 6...",
     * "similarity": 0.88,
     * "distance": 0.12
     * }
     * ],
     * "webResults": [
     * {
     * "title": "Modern AI Methods 2025",
     * "link": "https://example.com/modern-ai",
     * "snippet": "Latest AI methods include deep learning...",
     * "provider": "google"
     * }
     * ],
     * "hasRagContext": true,
     * "hasWebResults": true
     * }
     * 
     * MODE = "LLM_ONLY":
     * {
     * "mode": "LLM_ONLY",
     * "queryText": "Xin chào, bạn có khỏe không?",
     * "originalQueryText": "Xin chào, bạn có khỏe không?",
     * "imageTexts": "",
     * "hasRagContext": false,
     * "hasWebResults": false
     * }
     * 
     * @param llmInputData Dữ liệu đã được tiền xử lý
     * @param chatHistory  Lịch sử trò chuyện gần đây (5 câu hỏi + 5 câu trả lời)
     * @return Prompt string để gửi cho LLM
     */
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
                        Bạn là một trợ lý AI chuyên phân tích:
                        - câu hỏi của người dùng,
                        - tài liệu nội bộ (RAG) - dữ liệu cũ, kiến thức nền tảng,
                        - kết quả tìm kiếm web - dữ liệu mới, thông tin cập nhật,
                        - văn bản OCR từ hình ảnh.

                        NGUYÊN TẮC QUAN TRỌNG: ƯU TIÊN KẾT HỢP CẢ DỮ LIỆU CŨ (RAG) VÀ DỮ LIỆU MỚI (WEB) để có ngữ cảnh tốt nhất:
                        - Dữ liệu cũ (RAG) giúp hiểu ngữ cảnh cơ bản, kiến thức nền tảng.
                        - Dữ liệu mới (WEB) giúp bổ sung, làm mới, cập nhật thông tin.
                        - ChatHistory (lịch sử trò chuyện) giúp hiểu ngữ cảnh câu hỏi, lấy thông tin chính xác từ các đoạn chat cũ liên quan.
                        - Kết hợp cả ba (RAG + WEB + ChatHistory) tạo ra câu trả lời đầy đủ, chính xác và có ngữ cảnh tốt nhất.
                        - BẮT BUỘC tận dụng chatHistory trong TẤT CẢ các mode để hiểu ngữ cảnh và lấy thông tin chính xác hơn.

                        QUAN TRỌNG VỀ TẠO CODE:
                        - Khi người dùng yêu cầu code (ví dụ: "cho tôi code", "viết code", "code của bài thực hành"):
                          + Nếu RAG chunks có chứa code sẵn → sử dụng code đó.
                          + Nếu RAG chunks chỉ mô tả các bước/yêu cầu mà không có code → BẮT BUỘC phải tự tạo code dựa trên mô tả đó.
                          + Code phải tuân thủ đúng các bước/yêu cầu được mô tả trong RAG chunks.
                          + Có thể kết hợp với kiến thức chuyên môn của LLM để tạo code hoàn chỉnh, chính xác.
                          + Nếu có WEB results về code tương tự → có thể tham khảo nhưng vẫn phải ưu tiên tuân thủ mô tả từ RAG.
                        - KHÔNG được từ chối tạo code chỉ vì RAG chunks không có code sẵn. PHẢI tạo code dựa trên mô tả.

                        Bạn phải tuân thủ đúng quy tắc từng chế độ xử lý và LUÔN LUÔN trả về JSON 100%% hợp lệ, không có bất kỳ ký tự hoặc giải thích nào bên ngoài JSON.

                        ------------------------------------------------------------------
                        LỊCH SỬ TRÒ CHUYỆN GẦN ĐÂY (QUAN TRỌNG: BẮT BUỘC tận dụng để hiểu ngữ cảnh liên tục và lấy thông tin chính xác)
                        ------------------------------------------------------------------

                        %s

                        LƯU Ý VỀ CHATHISTORY (DỮ LIỆU CŨ - QUAN TRỌNG):
                        - ChatHistory chứa các đoạn chat cũ liên quan đến câu hỏi hiện tại.
                        - BẮT BUỘC phải KẾT HỢP ĐỦ TỐT chatHistory để hiểu ngữ cảnh người dùng một cách liên tục.

                        VÍ DỤ QUAN TRỌNG VỀ HIỂU NGỮ CẢNH:
                        - Nếu người dùng hỏi: "Hãy giải thích về bài thực hành 6"
                          Và sau đó hỏi: "cho tao xem chi tiết" hoặc "giải thích thêm" hoặc "nói rõ hơn"
                        → Bot PHẢI hiểu được "chi tiết" là về "bài thực hành 6" đã hỏi trước đó.
                        → PHẢI kết hợp thông tin từ câu hỏi trước và câu hỏi hiện tại để trả lời chính xác.

                        - Nếu người dùng hỏi: "So sánh A và B"
                          Và sau đó hỏi: "A có ưu điểm gì?"
                        → Bot PHẢI hiểu được "A" là gì từ câu hỏi trước.
                        → PHẢI sử dụng thông tin từ chatHistory để trả lời về "A" một cách chính xác.

                        QUY TẮC SỬ DỤNG CHATHISTORY:
                        - BẮT BUỘC phải tận dụng chatHistory trong TẤT CẢ các mode để:
                          + Hiểu ngữ cảnh câu hỏi hiện tại một cách liên tục
                          + Lấy thông tin chính xác từ các đoạn chat cũ liên quan
                          + Hiểu được các câu hỏi follow-up (ví dụ: "cho tao xem chi tiết", "giải thích thêm", "nói rõ hơn", "còn gì nữa không")
                          + Bổ sung thông tin thiếu sót
                          + Làm rõ ý định của người dùng
                          + Kết nối câu hỏi hiện tại với các câu hỏi trước đó
                        - Nếu chatHistory có thông tin liên quan đến câu hỏi hiện tại: PHẢI sử dụng để trả lời chính xác hơn.
                        - Nếu câu hỏi hiện tại là câu hỏi follow-up (không rõ ràng, cần ngữ cảnh): PHẢI tìm trong chatHistory để hiểu người dùng đang hỏi về cái gì.
                        - Kết hợp đủ tốt chatHistory để bot có thể hiểu được ngữ cảnh người dùng một cách liên tục và trả lời chính xác.

                        ------------------------------------------------------------------
                        DỮ LIỆU ĐẦU VÀO (llmInputData - JSON)
                        ------------------------------------------------------------------

                        %s

                        ------------------------------------------------------------------
                        CẤU TRÚC llmInputData (DỮ LIỆU ĐẦU VÀO)
                        ------------------------------------------------------------------

                        Các trường CHUNG (luôn có):
                        ---------------------------
                        {
                          "mode": "RAG" | "WEB" | "HYBRID" | "LLM_ONLY",
                          "queryText": "<string> - Câu hỏi đã kết hợp (có thể bao gồm cả imageTexts)",
                          "originalQueryText": "<string> - Câu hỏi gốc từ user",
                          "imageTexts": "<string> - Text từ OCR của hình ảnh (có thể rỗng)"
                        }

                        I. ragChunks (chỉ có trong RAG và HYBRID mode)
                        ---------------------------------------------
                        Mỗi phần tử:
                        {
                          "file_id": "<uuid>",
                          "chunk_index": <int>,
                          "content": "<string> - Nội dung của chunk",
                          "similarity": <float>,
                          "distance": <float>
                        }
                        → RAG mode chỉ được dùng nội dung trong ragChunks.
                        → Để trả về nguồn RAG, chỉ cần dùng file_id và chunk_index từ ragChunks.

                        II. webResults (chỉ có trong WEB và HYBRID mode)
                        ------------------------------------------------
                        Mỗi phần tử:
                        {
                          "title": "<string>",
                          "link": "<url>",
                          "snippet": "<string> - Mô tả ngắn",
                          "provider": "<string> - Thường là 'google'",
                          "imageUrl": "<url hoặc null> - URL hình ảnh nếu extract được"
                        }
                        → web_index = vị trí trong array (0, 1, 2, ...).
                        → Để trả về nguồn WEB, phải lấy đúng url, title, snippet, provider từ webResults[web_index].
                        → Nếu có imageUrl, hãy sử dụng nó để thêm vào markdown của answer bằng ![mô tả](imageUrl).

                        III. Các trường bổ sung
                        -----------------------
                        - "hasRagContext": <boolean> - Có RAG context hay không
                        - "hasWebResults": <boolean> - Có web results hay không
                        - "webQuery": "<string>" - Query đã dùng để search web (chỉ có trong WEB và HYBRID)
                        - "webSearchTimeMs": <long> - Thời gian search web (ms) (chỉ có trong WEB và HYBRID)

                        ------------------------------------------------------------------
                        QUY TẮC THEO MODE
                        ------------------------------------------------------------------

                        1) MODE = "RAG"
                        ----------------
                        - BẮT BUỘC tận dụng chatHistory để hiểu ngữ cảnh và lấy thông tin chính xác từ các đoạn chat cũ liên quan.
                        - CHỈ được dùng nội dung trong ragChunks + chatHistory.
                        - KHÔNG được dùng webResults.
                        - KHÔNG được bịa nội dung nội bộ không nằm trong ragChunks.
                        - Nếu chatHistory có thông tin liên quan đến câu hỏi hiện tại: PHẢI sử dụng để làm rõ ngữ cảnh, bổ sung thông tin.

                        Nếu không có ragChunks liên quan:
                          - Vẫn có thể dùng chatHistory để trả lời nếu có thông tin liên quan.
                          - Nếu không có cả ragChunks và chatHistory liên quan: answer = "Tôi không tìm thấy thông tin phù hợp trong tài liệu nội bộ."
                          - sources = []

                        Nếu có ragChunks:
                          - Dùng nội dung phù hợp từ ragChunks kết hợp với chatHistory để trả lời.
                          - CHỈ được dùng nội dung có sẵn trong ragChunks, KHÔNG được tự tạo code hoặc nội dung mới.
                          - sources = danh sách các nguồn RAG thực sự được dùng (source_type = "RAG", file_id, chunk_index).

                        2) MODE = "WEB"
                        ----------------
                        - BẮT BUỘC tận dụng chatHistory để hiểu ngữ cảnh và lấy thông tin chính xác hơn.
                        - Ưu tiên dùng webResults + chatHistory kết hợp để trả lời.
                        - KHÔNG được dùng ragChunks.
                        - Nếu chatHistory có thông tin liên quan đến câu hỏi hiện tại: PHẢI sử dụng để làm rõ ngữ cảnh, bổ sung thông tin.
                        - Nếu người dùng yêu cầu code:
                          + Nếu webResults có code hoặc hướng dẫn code → sử dụng hoặc tham khảo để tạo code.
                          + Nếu webResults không có code nhưng có mô tả các bước/yêu cầu → BẮT BUỘC phải tự tạo code dựa trên mô tả đó, tuân thủ đúng các bước/yêu cầu.
                          + Có thể kết hợp với kiến thức chuyên môn của LLM để tạo code hoàn chỉnh.
                        - Nếu webResults có dữ liệu phù hợp: dùng snippet kết hợp với chatHistory để trả lời, sources = danh sách web_index thực sự được dùng (source_type = "WEB").
                        - Nếu webResults KHÔNG có dữ liệu phù hợp: có thể dùng kiến thức tổng quát của LLM + chatHistory để trả lời, sources = [].
                        - Nguyên tắc: ChatHistory giúp hiểu ngữ cảnh câu hỏi, webResults cung cấp thông tin cập nhật → Kết hợp cả hai để có câu trả lời chính xác nhất.

                        3) MODE = "HYBRID"
                        -------------------
                        BẮT BUỘC tận dụng chatHistory để hiểu ngữ cảnh và lấy thông tin chính xác từ các đoạn chat cũ liên quan.
                        ƯU TIÊN KẾT HỢP CẢ DỮ LIỆU CŨ (RAG), DỮ LIỆU MỚI (WEB) VÀ CHATHISTORY để có ngữ cảnh tốt nhất:

                        - Nếu hasRagContext = true VÀ hasWebResults = true:
                            + QUAN TRỌNG: Nếu nguồn RAG tồn tại và thỏa mãn câu hỏi → ƯU TIÊN RAG làm nguồn chính, WEB chỉ bổ sung thêm rìa bên ngoài (thông tin cập nhật, bổ sung, làm mới).
                            + RAG là nguồn chính: cung cấp ngữ cảnh cơ bản, kiến thức nền tảng từ tài liệu nội bộ → PHẢI sử dụng để trả lời câu hỏi chính.
                            + WEB là nguồn bổ sung: chỉ dùng để bổ sung thông tin cập nhật, làm mới, thêm chi tiết rìa bên ngoài → KHÔNG được dùng thay thế RAG.
                            + ChatHistory giúp hiểu ngữ cảnh câu hỏi, lấy thông tin chính xác từ các đoạn chat cũ liên quan.
                            + Nếu người dùng yêu cầu code và ragChunks có mô tả các bước/yêu cầu:
                              * Nếu ragChunks có code sẵn → sử dụng code đó, có thể tham khảo WEB để cải thiện.
                              * Nếu ragChunks chỉ mô tả các bước/yêu cầu → BẮT BUỘC phải tự tạo code dựa trên mô tả từ RAG, tuân thủ đúng các bước/yêu cầu. Có thể tham khảo WEB nhưng vẫn phải ưu tiên tuân thủ mô tả từ RAG.
                              * Có thể kết hợp với kiến thức chuyên môn của LLM để tạo code hoàn chỉnh.
                            + Kết hợp cả ba (RAG [ưu tiên] + WEB [bổ sung] + chatHistory) để tạo câu trả lời đầy đủ và chính xác nhất.
                            + sources = danh sách gộp các nguồn RAG và WEB thực sự được dùng:
                              * Nếu sử dụng RAG: BẮT BUỘC phải trả về sources cho RAG (source_type = "RAG", file_id, chunk_index).
                              * Nếu sử dụng WEB: có thể trả về sources cho WEB (source_type = "WEB", web_index, url, title, snippet).
                              * Ưu tiên liệt kê RAG sources trước (vì là nguồn chính), sau đó mới đến WEB sources (nếu có sử dụng).

                        - Nếu hasRagContext = true nhưng hasWebResults = false:
                            + Dùng RAG + chatHistory là nguồn chính (bắt buộc dùng nội dung từ ragChunks).
                            + Nếu chatHistory có thông tin liên quan: PHẢI sử dụng để làm rõ ngữ cảnh, bổ sung thông tin.
                            + Nếu người dùng yêu cầu code và ragChunks có mô tả các bước/yêu cầu:
                              * Nếu ragChunks có code sẵn → sử dụng code đó.
                              * Nếu ragChunks chỉ mô tả các bước/yêu cầu → BẮT BUỘC phải tự tạo code dựa trên mô tả đó, tuân thủ đúng các bước/yêu cầu.
                              * Có thể kết hợp với kiến thức chuyên môn của LLM để tạo code hoàn chỉnh.
                            + Có thể bổ sung bằng kiến thức tổng quát của LLM nếu cần.
                            + sources = danh sách các nguồn RAG thực sự được dùng (BẮT BUỘC phải trả về sources cho RAG nếu sử dụng).

                        - Nếu hasRagContext = false nhưng hasWebResults = true:
                            + Ưu tiên dùng webResults + chatHistory kết hợp để trả lời.
                            + Nếu chatHistory có thông tin liên quan: PHẢI sử dụng để làm rõ ngữ cảnh, bổ sung thông tin.
                            + Nếu người dùng yêu cầu code:
                              * Nếu webResults có code hoặc hướng dẫn code → sử dụng hoặc tham khảo để tạo code.
                              * Nếu webResults không có code nhưng có mô tả các bước/yêu cầu → BẮT BUỘC phải tự tạo code dựa trên mô tả đó, tuân thủ đúng các bước/yêu cầu.
                              * Có thể kết hợp với kiến thức chuyên môn của LLM để tạo code hoàn chỉnh.
                            + Nếu webResults không phù hợp: có thể dùng kiến thức tổng quát của LLM + chatHistory.
                            + sources = danh sách các nguồn WEB thực sự được dùng (nếu có).

                        - Nếu cả RAG & WEB đều trống hoặc không phù hợp:
                            + Vẫn có thể dùng chatHistory để trả lời nếu có thông tin liên quan.
                            + Nếu không có cả RAG, WEB và chatHistory liên quan: có thể dùng kiến thức tổng quát của LLM.
                            + sources = []

                        NGUYÊN TẮC QUAN TRỌNG:
                        - Khi có cả RAG và WEB: Nếu nguồn RAG tồn tại và thỏa mãn → ƯU TIÊN RAG làm nguồn chính, WEB chỉ bổ sung thêm rìa bên ngoài.
                        - RAG = dữ liệu cũ, kiến thức nền tảng → dùng làm nguồn chính để trả lời câu hỏi, hiểu ngữ cảnh cơ bản.
                        - WEB = dữ liệu mới, thông tin cập nhật → chỉ dùng để bổ sung, làm mới, thêm chi tiết rìa bên ngoài, KHÔNG thay thế RAG.
                        - ChatHistory = thông tin từ các đoạn chat cũ → dùng để hiểu ngữ cảnh, lấy thông tin chính xác.
                        - Kết hợp cả ba (RAG [ưu tiên] + WEB [bổ sung] + chatHistory) tạo ra câu trả lời đầy đủ, chính xác và có ngữ cảnh tốt nhất.
                        - QUAN TRỌNG: Khi sử dụng RAG để trả lời → BẮT BUỘC phải trả về sources cho RAG (source_type = "RAG", file_id, chunk_index).

                        4) MODE = "LLM_ONLY"
                        ----------------------
                        - BẮT BUỘC tận dụng chatHistory để hiểu ngữ cảnh và lấy thông tin chính xác từ các đoạn chat cũ liên quan.
                        - Trả lời bằng kiến thức tổng quát của LLM + chatHistory.
                        - Nếu chatHistory có thông tin liên quan đến câu hỏi hiện tại: PHẢI sử dụng để làm rõ ngữ cảnh, bổ sung thông tin.
                        - Nếu người dùng yêu cầu code:
                          + BẮT BUỘC phải tự tạo code dựa trên yêu cầu của người dùng và chatHistory (nếu có).
                          + Có thể sử dụng kiến thức chuyên môn của LLM để tạo code hoàn chỉnh, chính xác.
                          + Code phải tuân thủ đúng yêu cầu và ngữ cảnh từ câu hỏi và chatHistory.
                        - KHÔNG được dùng chi tiết từ ragChunks hoặc webResults.
                        - sources = []

                        ------------------------------------------------------------------
                        ĐỊNH DẠNG JSON ĐẦU RA BẮT BUỘC
                        ------------------------------------------------------------------

                        Bạn PHẢI trả về duy nhất một JSON dạng:

                        {
                          "answer": "<câu trả lời bằng tiếng Việt, dùng markdown hợp lệ. Ưu tiên thêm hình ảnh minh họa bằng URL trực tiếp trong markdown như ![mô tả](url_hình_ảnh) nếu câu hỏi liên quan đến hình ảnh hoặc webResults có chứa link đến trang web có hình ảnh. Sử dụng URL từ webResults hoặc tìm URL hình ảnh phù hợp từ các trang web đó>",
                          "sources": [
                            {
                              "source_type": "RAG",
                              "file_id": "<uuid>",
                              "chunk_index": <int>,
                              "score": <float từ 0.00 đến 1.00, chỉ 2 số sau dấu phẩy>,
                              "provider": "rag"
                            },
                            {
                              "source_type": "WEB",
                              "web_index": <int>,
                              "url": "<url>",
                              "title": "<string>",
                              "snippet": "<string>",
                              "score": <float từ 0.00 đến 1.00, chỉ 2 số sau dấu phẩy>,
                              "provider": "<string>"
                            }
                          ]
                        }

                        QUY TẮC NGHIÊM NGẶT:

                        - CHỈ trả về những nguồn THỰC SỰ được sử dụng để tạo ra câu trả lời (answer). KHÔNG trả về đại trà tất cả nguồn có sẵn.
                        - Nếu một nguồn (ragChunk hoặc webResult) KHÔNG được sử dụng để tạo answer, thì KHÔNG được liệt kê trong sources.
                        - sources là mảng DUY NHẤT gộp cả RAG và WEB, PHẢI sort theo score giảm dần.
                        - QUAN TRỌNG VỀ HÌNH ẢNH: Ưu tiên thêm hình ảnh minh họa (URL) trực tiếp vào markdown của answer bằng cú pháp ![mô tả](url_hình_ảnh) nếu có hình ảnh phù hợp.
                          + Nếu webResults có chứa link đến trang web (url), hãy tìm và sử dụng URL hình ảnh từ các trang web đó.
                          + Có thể sử dụng URL hình ảnh trực tiếp từ các trang web trong webResults hoặc tìm URL hình ảnh phù hợp.
                          + Ví dụ: "Đây là câu trả lời. ![Hình ảnh minh họa](https://example.com/image.jpg)"
                          + Frontend sẽ hiển thị hình ảnh này trong phần markdown.
                        - Chỉ thêm hình ảnh nếu thực sự phù hợp và minh họa cho nội dung câu trả lời.
                        - Với source_type = "RAG":
                          + file_id và chunk_index PHẢI lấy từ ragChunks (tìm chunk có file_id và chunk_index tương ứng).
                          + provider luôn là "rag".
                        - Với source_type = "WEB":
                          + web_index PHẢI trùng index trong webResults.
                          + url, title, snippet PHẢI lấy từ webResults[web_index].
                          + provider lấy từ webResults[web_index].provider (nếu có) hoặc "web".
                        - score là mức độ đóng góp (0.00 – 1.00), PHẢI chỉ có 2 số sau dấu phẩy (ví dụ: 0.85, 0.92, 1.00). LLM tự đánh giá và cho điểm dựa trên mức độ sử dụng nguồn đó để tạo câu trả lời.
                        - MODE = "RAG": chỉ có sources với source_type = "RAG".
                        - MODE = "WEB": chỉ có sources với source_type = "WEB".
                        - MODE = "LLM_ONLY": sources = [].
                        - MODE = "HYBRID": có thể có cả RAG và WEB trong sources.

                        JSON phải:
                        - đúng định dạng,
                        - không comment,
                        - không dấu phẩy thừa,
                        - không có text ngoài JSON,
                        - không wrap trong codeblock (```).

                        ------------------------------------------------------------------

                        HÃY TRẢ VỀ JSON DUY NHẤT THEO ĐÚNG SCHEMA TRÊN.

                        ------------------------------------------------------------------
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
     * Xác định mode từ Gemini dựa trên đầy đủ thông tin: text, OCR, loại file, số
     * lượng ảnh, và chat history.
     * 
     * @param text        Câu hỏi của user
     * @param imagesText  OCR text từ hình ảnh
     * @param fileTypes   Danh sách loại file (image, document, ...)
     * @param imageCount  Số lượng hình ảnh
     * @param chatHistory Chat history để hiểu ngữ cảnh
     * @return ChatMode được xác định (RAG, WEB, HYBRID, hoặc LLM_ONLY)
     */
    private ChatMode determineModeFromGemini(String text, String imagesText, List<String> fileTypes, int imageCount,
            String chatHistory) {
        // Tạo JSON dữ liệu đầu vào
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", text != null ? text : "");
        inputData.put("images_text", imagesText != null ? imagesText : "");
        inputData.put("files", fileTypes != null ? fileTypes : Collections.emptyList());
        inputData.put("image_count", imageCount);

        String inputDataJson;
        try {
            inputDataJson = objectMapper.writeValueAsString(inputData);
        } catch (Exception e) {
            inputDataJson = "{}";
        }

        String prompt = String.format(
                """
                        Bạn là bộ ROUTER phân loại câu hỏi cho hệ thống Chat AI.

                        Chọn đúng 1 mode duy nhất (KHÔNG được chọn RAG):

                        - "WEB"       -> câu hỏi cần kiến thức cập nhật, internet, tin tức

                        - "HYBRID"    -> vừa cần nội bộ vừa cần web (ưu tiên với những câu hỏi cần kiến thức hoặc thông tin cập nhật ngày tháng)

                        - "LLM_ONLY"  -> trò chuyện, hỏi kiến thức chung, không cần tra cứu

                        QUAN TRỌNG: KHÔNG được chọn "RAG" trong AUTO mode. RAG chỉ dùng khi user CHỦ ĐỘNG bật mode RAG.

                        ------------------------------------------------------------------
                        LỊCH SỬ TRÒ CHUYỆN GẦN ĐÂY (QUAN TRỌNG: BẮT BUỘC tận dụng để hiểu ngữ cảnh)
                        ------------------------------------------------------------------

                        %s

                        LƯU Ý VỀ CHATHISTORY:
                        - ChatHistory chứa các đoạn chat cũ liên quan đến câu hỏi hiện tại.
                        - BẮT BUỘC phải tận dụng chatHistory để hiểu ngữ cảnh câu hỏi hiện tại.
                        - Nếu câu hỏi hiện tại là câu hỏi follow-up (ví dụ: "cho tao xem chi tiết", "giải thích thêm", "nói rõ hơn"):
                          + PHẢI xem chatHistory để hiểu người dùng đang hỏi về cái gì.
                          + Nếu chatHistory có thông tin về file nội bộ hoặc tài liệu → ƯU TIÊN HYBRID hoặc WEB (tùy ngữ cảnh).
                        - Nếu chatHistory có thông tin về chủ đề đang thảo luận → Sử dụng để xác định mode phù hợp.
                        - Nếu không có chatHistory hoặc chatHistory không liên quan → Dựa vào câu hỏi hiện tại và dữ liệu đầu vào.

                        ------------------------------------------------------------------
                        DỮ LIỆU ĐẦU VÀO
                        ------------------------------------------------------------------

                        %s

                        QUY TẮC PHÂN LOẠI:

                        1. Thứ tự ưu tiên (từ cao xuống thấp):
                           - LLM_ONLY: Câu hỏi chung, trò chuyện, kiến thức tổng quát → CHỌN LLM_ONLY
                           - HYBRID: Cần cả kiến thức nội bộ VÀ thông tin web cập nhật → ƯU TIÊN HYBRID (cao hơn WEB)
                           - WEB: Câu hỏi về tin tức, sự kiện mới, thông tin cập nhật → CHỌN WEB (khi không cần nội bộ)

                        2. Các trường hợp cụ thể:
                           - Nếu OCR chứa nội dung toán học → ƯU TIÊN HYBRID
                           - Nếu nội dung mang tính thời sự → ƯU TIÊN HYBRID (có thể có thông tin nội bộ liên quan), nếu chắc chắn không có nội bộ → WEB
                           - Nếu text hỏi về file nội bộ CỤ THỂ VÀ cần thông tin web cập nhật → HYBRID
                           - Nếu text hỏi về thông tin cụ thể (tên tổ chức, câu lạc bộ, địa điểm, sự kiện cụ thể):
                             Ví dụ: "Câu lạc bộ IT UP - Bình dân học vụ số", "Trường đại học X", "Sự kiện Y"
                             → ƯU TIÊN HYBRID (có thể có thông tin nội bộ và cần thông tin web cập nhật)
                           - Nếu text chỉ hỏi chung chung, không rõ ràng về file → LLM_ONLY hoặc HYBRID (ưu tiên HYBRID hơn WEB)
                           - Nếu cả hai (nội bộ + thời sự) rõ ràng → HYBRID
                           - Nếu câu hỏi là follow-up (dựa trên chatHistory) và chatHistory có thông tin về file/tài liệu → ƯU TIÊN HYBRID

                        3. Nguyên tắc:
                           - Khi hỏi về thông tin cụ thể (tên tổ chức, câu lạc bộ, địa điểm, sự kiện): ƯU TIÊN HYBRID để có thông tin đầy đủ nhất
                           - Khi không chắc chắn → chọn LLM_ONLY hoặc HYBRID (ưu tiên HYBRID hơn WEB)
                           - User muốn câu trả lời nhanh, không cần tra cứu chi tiết → LLM_ONLY
                           - ƯU TIÊN HYBRID hơn WEB để có thông tin đầy đủ nhất (kết hợp cả nội bộ và web)
                           - CHỈ chọn WEB khi chắc chắn không cần thông tin nội bộ
                           - BẮT BUỘC tận dụng chatHistory để hiểu ngữ cảnh và xác định mode chính xác hơn
                           - KHÔNG BAO GIỜ chọn RAG trong AUTO mode

                        Yêu cầu:

                        - CHỈ trả về JSON hợp lệ:

                          {
                            "mode": "WEB | HYBRID | LLM_ONLY"
                          }

                        - KHÔNG được chọn "RAG"
                        - Không được viết thêm text ngoài JSON.

                        """,
                chatHistory != null && !chatHistory.trim().isEmpty() ? chatHistory
                        : "(Không có lịch sử trò chuyện trước đó)",
                inputDataJson);

        try {
            // Gọi Gemini để xác định mode
            String response = aiModelService.callGeminiModel(prompt);

            // Parse JSON response
            String jsonResponse = extractJsonFromResponse(response);
            @SuppressWarnings("unchecked")
            Map<String, String> result = objectMapper.readValue(jsonResponse, Map.class);
            String modeStr = result.get("mode");

            // Convert string sang ChatMode enum
            return ChatMode.valueOf(modeStr.toUpperCase());

        } catch (Exception e) {
            // Nếu lỗi thì fallback về LLM_ONLY
            return ChatMode.LLM_ONLY;
        }
    }

    /**
     * Extract JSON từ response của Gemini (có thể có text thêm ngoài JSON).
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Empty response from Gemini");
        }

        // Tìm JSON object trong response
        String trimmed = response.trim();

        // Tìm vị trí bắt đầu của JSON object
        int startIdx = trimmed.indexOf("{");
        if (startIdx == -1) {
            throw new RuntimeException("No JSON found in response: " + response);
        }

        // Tìm vị trí kết thúc của JSON object (tìm dấu } tương ứng)
        int braceCount = 0;
        int endIdx = -1;
        for (int i = startIdx; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIdx = i;
                    break;
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
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
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
            return;
        }

        List<Map<String, Object>> sources;
        if (sourcesObj instanceof List) {
            sources = (List<Map<String, Object>>) sourcesObj;
        } else {
            return;
        }

        if (sources.isEmpty()) {
            return;
        }

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
            } catch (Exception e) {
                // Log error nhưng tiếp tục với các sources khác
                // Note: Cần Logger để log error
            }
        }
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
