package com.example.springboot_api.mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.example.springboot_api.dto.user.chatbot.ChatResponse;
import com.example.springboot_api.dto.user.chatbot.ConversationItem;
import com.example.springboot_api.dto.user.chatbot.FileResponse;
import com.example.springboot_api.dto.user.chatbot.LlmModelResponse;
import com.example.springboot_api.dto.user.chatbot.ModelResponse;
import com.example.springboot_api.dto.user.chatbot.SourceResponse;
import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.NotebookBotConversation;
import com.example.springboot_api.models.NotebookBotMessage;
import com.example.springboot_api.models.NotebookBotMessageFile;
import com.example.springboot_api.models.NotebookBotMessageSource;
import com.example.springboot_api.services.user.ChatBotService.RagChunk;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi ChatBot entities sang DTOs.
 * Tách logic mapping khỏi ChatBotService để code clean hơn.
 */
@Component
@RequiredArgsConstructor
public class ChatBotMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert LlmModel entity sang LlmModelResponse DTO.
     */
    public LlmModelResponse toLlmModelResponse(LlmModel model) {
        if (model == null) {
            return null;
        }

        return new LlmModelResponse(
                model.getId(),
                model.getCode(),
                model.getProvider(),
                model.getDisplayName(),
                model.getIsActive(),
                model.getIsDefault(),
                model.getMetadata(),
                model.getCreatedAt());
    }

    /**
     * Convert list LlmModel entities sang list LlmModelResponse DTOs.
     */
    public List<LlmModelResponse> toLlmModelResponseList(List<LlmModel> models) {
        if (models == null) {
            return List.of();
        }

        return models.stream()
                .map(this::toLlmModelResponse)
                .toList();
    }

    /**
     * Convert NotebookBotMessageFile entity sang FileResponse DTO.
     */
    public FileResponse toFileResponse(NotebookBotMessageFile file) {
        if (file == null) {
            return null;
        }

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
    }

    /**
     * Convert Set<NotebookBotMessageFile> sang List<FileResponse>.
     */
    public List<FileResponse> toFileResponseList(Set<NotebookBotMessageFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert NotebookBotConversation sang ConversationItem DTO.
     */
    public ConversationItem toConversationItem(NotebookBotConversation conversation,
            String firstMessage, Long totalMessages) {
        if (conversation == null) {
            return null;
        }

        return new ConversationItem(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getNotebook() != null ? conversation.getNotebook().getId() : null,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                firstMessage,
                totalMessages);
    }

    /**
     * Convert NotebookBotMessage sang ChatResponse DTO.
     */
    public ChatResponse toChatResponse(NotebookBotMessage message) {
        if (message == null) {
            return null;
        }

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

        return response;
    }

    /**
     * Convert NotebookBotMessage sang ChatResponse DTO kèm sources và files.
     */
    public ChatResponse toChatResponseWithSourcesAndFiles(NotebookBotMessage message,
            Map<String, Object> llmInputData) {
        ChatResponse response = toChatResponse(message);
        if (response == null) {
            return null;
        }

        // Set sources (RAG và WEB)
        Set<NotebookBotMessageSource> dbSources = message.getNotebookBotMessageSources();
        if (dbSources != null && !dbSources.isEmpty()) {
            response.setSources(toSourceResponseList(dbSources, llmInputData));
        } else {
            response.setSources(new ArrayList<>());
        }

        // Set files nếu có
        Set<NotebookBotMessageFile> files = message.getNotebookBotMessageFiles();
        if (files != null && !files.isEmpty()) {
            response.setFiles(toFileResponseList(files));
        } else {
            response.setFiles(new ArrayList<>());
        }

        return response;
    }

    /**
     * Build List<SourceResponse> từ database sources và llmInputData.
     */
    @SuppressWarnings("unchecked")
    public List<SourceResponse> toSourceResponseList(Set<NotebookBotMessageSource> dbSources,
            Map<String, Object> llmInputData) {
        List<SourceResponse> allSources = new ArrayList<>();

        if (dbSources == null || dbSources.isEmpty()) {
            return allSources;
        }

        // Lấy ragChunks và webResults từ llmInputData để có thông tin đầy đủ
        List<Map<String, Object>> ragChunks = llmInputData != null
                ? (List<Map<String, Object>>) llmInputData.get("ragChunks")
                : null;
        if (ragChunks == null) {
            ragChunks = new ArrayList<>();
        }

        List<Map<String, Object>> webResults = llmInputData != null
                ? (List<Map<String, Object>>) llmInputData.get("webResults")
                : null;
        if (webResults == null) {
            webResults = new ArrayList<>();
        }

        for (NotebookBotMessageSource source : dbSources) {
            if ("RAG".equalsIgnoreCase(source.getSourceType())) {
                allSources.add(toRagSourceResponse(source, ragChunks));
            } else if ("WEB".equalsIgnoreCase(source.getSourceType())) {
                allSources.add(toWebSourceResponse(source, webResults));
            }
        }

        // Sort theo score giảm dần
        allSources.sort(Comparator.comparing(
                SourceResponse::getScore,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return allSources;
    }

    /**
     * Build RAG SourceResponse từ NotebookBotMessageSource và ragChunks.
     */
    private SourceResponse toRagSourceResponse(NotebookBotMessageSource source,
            List<Map<String, Object>> ragChunks) {
        // Tìm chunk tương ứng trong ragChunks để lấy content, similarity, distance
        Map<String, Object> matchingChunk = ragChunks.stream()
                .filter(chunk -> source.getFileId() != null
                        && source.getFileId().toString().equals(chunk.get("file_id"))
                        && source.getChunkIndex() != null
                        && source.getChunkIndex().equals(chunk.get("chunk_index")))
                .findFirst()
                .orElse(null);

        return SourceResponse.builder()
                .sourceType("RAG")
                .fileId(source.getFileId())
                .chunkIndex(source.getChunkIndex())
                .score(source.getScore())
                .provider(source.getProvider() != null ? source.getProvider() : "rag")
                .content(matchingChunk != null ? (String) matchingChunk.get("content") : null)
                .similarity(extractDoubleValue(matchingChunk, "similarity"))
                .distance(extractDoubleValue(matchingChunk, "distance"))
                // WEB fields = null
                .webIndex(null)
                .url(null)
                .title(null)
                .snippet(null)
                .imageUrl(null)
                .favicon(null)
                .build();
    }

    /**
     * Build WEB SourceResponse từ NotebookBotMessageSource và webResults.
     */
    private SourceResponse toWebSourceResponse(NotebookBotMessageSource source,
            List<Map<String, Object>> webResults) {
        // Tìm web result tương ứng để lấy imageUrl
        Map<String, Object> matchingWebResult = null;
        if (source.getWebIndex() != null && source.getWebIndex() >= 0
                && source.getWebIndex() < webResults.size()) {
            matchingWebResult = webResults.get(source.getWebIndex());
        }

        return SourceResponse.builder()
                .sourceType("WEB")
                .webIndex(source.getWebIndex())
                .url(source.getUrl())
                .title(source.getTitle())
                .snippet(source.getSnippet())
                .score(source.getScore())
                .provider(source.getProvider() != null ? source.getProvider() : "web")
                .imageUrl(matchingWebResult != null ? (String) matchingWebResult.get("imageUrl") : null)
                .favicon(null)
                // RAG fields = null
                .fileId(null)
                .chunkIndex(null)
                .content(null)
                .similarity(null)
                .distance(null)
                .build();
    }

    /**
     * Extract Double value từ Map an toàn.
     */
    private Double extractDoubleValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Convert List<RagChunk> sang format JSON (List<Map>).
     */
    public List<Map<String, Object>> toRagChunksJson(List<RagChunk> ragChunks) {
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
     * Không bao gồm image extraction - để service xử lý.
     */
    public List<Map<String, Object>> toWebResultJson(WebSearchResult webResult) {
        if (webResult == null || webResult.items() == null || webResult.items().isEmpty()) {
            return new ArrayList<>();
        }

        return webResult.items().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("title", item.title());
                    itemMap.put("link", item.link());
                    itemMap.put("snippet", item.snippet());
                    itemMap.put("provider", "google");
                    return itemMap;
                })
                .collect(Collectors.toList());
    }
}
