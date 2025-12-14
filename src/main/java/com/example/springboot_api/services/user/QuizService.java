package com.example.springboot_api.services.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.quiz.QuizListResponse;
import com.example.springboot_api.dto.user.quiz.QuizResponse;
import com.example.springboot_api.mappers.QuizMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các thao tác liên quan đến Quiz.
 * Cung cấp API lấy danh sách quiz theo NotebookAiSet ID.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

        private final QuizRepository quizRepository;
        private final NotebookAiSetRepository aiSetRepository;
        private final NotebookRepository notebookRepository;
        private final NotebookMemberRepository notebookMemberRepository;
        private final QuizMapper quizMapper;

        /**
         * Lấy danh sách quiz theo NotebookAiSet ID.
         * Bao gồm tất cả thông tin: AI Set info, quizzes, options.
         * Kiểm tra quyền truy cập: user phải là thành viên đã được duyệt của notebook.
         *
         * @param userId          ID của user đang request
         * @param notebookId      ID của notebook
         * @param notebookAiSetId ID của NotebookAiSet chứa các quiz
         * @return QuizListResponse chứa đầy đủ thông tin quiz và options
         * @throws NotFoundException   nếu không tìm thấy AI Set hoặc Notebook
         * @throws BadRequestException nếu user chưa gia nhập nhóm
         */
        @Transactional(readOnly = true)
        public QuizListResponse getQuizzesByAiSetId(UUID userId, UUID notebookId, UUID notebookAiSetId) {
                // Kiểm tra notebook tồn tại
                Notebook notebook = notebookRepository.findById(notebookId)
                                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

                // Kiểm tra user có quyền truy cập notebook không
                Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId,
                                userId);
                boolean isCommunity = "community".equals(notebook.getType());
                boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

                if (isCommunity) {
                        if (!isMember) {
                                throw new BadRequestException(
                                                "Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
                        }
                } else {
                        if (!isMember) {
                                throw new BadRequestException("Bạn chưa tham gia nhóm này");
                        }
                }

                // Validate và lấy AI Set
                NotebookAiSet aiSet = aiSetRepository.findById(notebookAiSetId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy AI Set với ID: " + notebookAiSetId));

                // Kiểm tra AI Set có thuộc notebook không
                if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
                        throw new BadRequestException("AI Set không thuộc notebook này");
                }

                // Lấy danh sách quiz kèm options và convert sang DTO
                List<NotebookQuizz> quizzes = quizRepository.findByAiSetIdWithOptions(notebookAiSetId);
                List<QuizResponse> quizResponses = quizMapper.toQuizResponseList(quizzes);

                return quizMapper.toQuizListResponse(aiSet, quizResponses);
        }
}
