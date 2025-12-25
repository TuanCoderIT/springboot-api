package com.example.springboot_api.services.shared.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageClassifierServiceTest {

    @Mock
    private AIModelService aiModelService;

    private MessageClassifierService messageClassifierService;

    @BeforeEach
    void setUp() {
        messageClassifierService = new MessageClassifierService(aiModelService);
    }

    @Test
    void classifyMessage_ShouldReturnNoSearch_WhenGreeting() {
        ClassifierMode mode = messageClassifierService.classifyMessage("Xin chào bạn", false, null);
        assertEquals(ClassifierMode.NO_SEARCH, mode);
    }

    @ParameterizedTest
    @CsvSource({
            "Chào bạn, NO_SEARCH",
            "Cảm ơn nhé, NO_SEARCH",
            "Hello, NO_SEARCH",
            "Ok thanks, NO_SEARCH",
            "Hi there, NO_SEARCH"
    })
    void classifyMessage_ShouldIdentifySmallTalk(String message, ClassifierMode expectedMode) {
        ClassifierMode mode = messageClassifierService.classifyMessage(message, false, null);
        assertEquals(expectedMode, mode);
    }

    @ParameterizedTest
    @CsvSource({
            "Nghĩa là gì, REUSE",
            "Tại sao vậy, REUSE",
            "Giải thích thêm đi, REUSE",
            "Cho ví dụ cụ thể, REUSE",
            "Đoạn đó nói gì, REUSE",
            "Ý bạn là sao, REUSE",
            "chưa hiểu lắm, REUSE",
            "Example please, REUSE",
            "Why is that, REUSE"
    })
    void classifyMessage_ShouldIdentifyReuse_WhenContextExists(String message, ClassifierMode expectedMode) {
        // Assume context exists for follow-up questions
        ClassifierMode mode = messageClassifierService.classifyMessage(message, true, List.of("Previous context"));
        assertEquals(expectedMode, mode);
    }

    @ParameterizedTest
    @CsvSource({
            "React là gì, SEARCH",
            "Giải thích về Docker, SEARCH",
            "Spring Boot architecture, SEARCH",
            "So sánh SQL và NoSQL, SEARCH",
            "Hướng dẫn cài đặt Java, SEARCH",
            "What is Microservices, SEARCH",
            "How to define a variable in Python, SEARCH"
    })
    void classifyMessage_ShouldIdentifySearch_ForTechQuestions(String message, ClassifierMode expectedMode) {
        ClassifierMode mode = messageClassifierService.classifyMessage(message, false, null);
        assertEquals(expectedMode, mode);
    }

    @ParameterizedTest
    @CsvSource({
            "Công văn 123 quy định gì, SEARCH",
            "Nghị định về đào tạo, SEARCH",
            "Thông tư mới nhất, SEARCH",
            "Quy chế điểm thi, SEARCH",
            "Luật giáo dục 2019, SEARCH",
            "Điều 5 khoản 2 có nội dung gì, SEARCH",
            "Phụ lục đính kèm, SEARCH",
            "Hướng dẫn thực hiện quy chế, SEARCH"
    })
    void classifyMessage_ShouldIdentifySearch_ForRegulationQuestions(String message, ClassifierMode expectedMode) {
        ClassifierMode mode = messageClassifierService.classifyMessage(message, false, null);
        assertEquals(expectedMode, mode);
    }

    @Test
    void classifyMessage_ShouldFallbackToLLM_WhenAmbiguous() {
        // Một câu mơ hồ không match rule nào rõ ràng
        String ambiguousMessage = "Con mèo đang ngủ";

        // Mock LLM response
        when(aiModelService.callGeminiModel(anyString())).thenReturn("SEARCH");

        ClassifierMode mode = messageClassifierService.classifyMessage(ambiguousMessage, true, Collections.emptyList());

        // Expect LLM result
        assertEquals(ClassifierMode.SEARCH, mode);
    }
}
