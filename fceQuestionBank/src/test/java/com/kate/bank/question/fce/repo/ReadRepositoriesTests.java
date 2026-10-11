package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import com.kate.bank.question.fce.model.Book;
import com.kate.bank.question.fce.model.Edition;
import com.kate.bank.question.fce.model.ExamPaper;
import com.kate.bank.question.fce.model.ExamSection;
import com.kate.bank.question.fce.model.OptionSet;
import com.kate.bank.question.fce.model.PassageParagraph;
import com.kate.bank.question.fce.model.Question;
import com.kate.bank.question.fce.model.QuestionAnswer;
import com.kate.bank.question.fce.model.QuestionEvidence;
import com.kate.bank.question.fce.model.QuestionOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static com.kate.bank.question.fce.support.ModelFixtures.entity;
import static org.assertj.core.api.Assertions.assertThat;

/** Repository integration checks use only H2, never the migrated business database. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReadRepositoriesTests {
    @Autowired private EntityManager entityManager;
    @Autowired private EditionRepository editions;
    @Autowired private BookRepository books;
    @Autowired private ExamPaperRepository papers;
    @Autowired private ExamSectionRepository sections;
    @Autowired private PassageParagraphRepository paragraphs;
    @Autowired private OptionSetRepository sets;
    @Autowired private QuestionOptionRepository options;
    @Autowired private QuestionRepository questions;
    @Autowired private QuestionAnswerRepository answers;
    @Autowired private QuestionEvidenceRepository evidence;

    private void persist(Object... rows) {
        for (Object row : rows) {
            entityManager.persist(row);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    // 测试目的：验证真实 repo 查询按版本/册/试卷排序，隐藏草稿，按 slug 与 published 共同筛选。
    void catalogQueriesSortAndFilterPublicationStatus() {
        persist(entity(Edition.class, 2, "code", "campus", "sortOrder", 2),
                entity(Edition.class, 1, "code", "standard", "sortOrder", 1),
                entity(Book.class, 20, "editionId", 2L, "bookNo", 3, "sortOrder", 3),
                entity(Book.class, 10, "editionId", 1L, "bookNo", 1, "sortOrder", 1),
                entity(ExamPaper.class, 100, "bookId", 10L, "testNo", 2, "slug", "ready", "status", "published",
                        "sourceMetadata", Map.of("exam_page", 2)),
                entity(ExamPaper.class, 101, "bookId", 10L, "testNo", 1, "slug", "pending", "status", "pending"),
                entity(ExamPaper.class, 102, "bookId", 20L, "testNo", 3, "slug", "draft", "status", "draft"));
        assertThat(editions.findAllByOrderBySortOrderAscIdAsc()).extracting(Edition::getCode)
                .containsExactly("standard", "campus");
        assertThat(books.findAllByOrderBySortOrderAscIdAsc()).extracting(Book::getId).containsExactly(10L, 20L);
        assertThat(papers.findAllByStatusInOrderByTestNoAscIdAsc(List.of("pending", "published")))
                .extracting(ExamPaper::getSlug).containsExactly("pending", "ready");
        assertThat(papers.findBySlugAndStatus("ready", "published").orElseThrow().getSourceMetadata())
                .containsEntry("exam_page", 2);
        assertThat(papers.findBySlugAndStatus("pending", "published")).isEmpty();
        assertThat(papers.findBySlugAndStatus("draft", "published")).isEmpty();
        assertThat(papers.findBySlugAndStatus("unknown", "published")).isEmpty();
    }

    @Test
    // 测试目的：验证 Part 与所有内容表的实际查询隔离试卷/Part，保持顺序并正确读取 JSON 与数组字段。
    void contentQueriesStayWithinParentsAndRoundTripJson() {
        persist(entity(ExamSection.class, 10, "paperId", 1L, "code", "P1", "partNo", 1, "sortOrder", 2,
                        "sourceGroups", List.of(Map.of("title", "正文", "paragraph_ids", List.of("P1-p1")))),
                entity(ExamSection.class, 20, "paperId", 1L, "code", "P2", "partNo", 2, "sortOrder", 1),
                entity(ExamSection.class, 30, "paperId", 2L, "code", "P1", "partNo", 1, "sortOrder", 1),
                entity(PassageParagraph.class, 11, "sectionId", 10L, "sourceKey", "P1-p1", "text", "Text\n{{1}}", "sortOrder", 2),
                entity(PassageParagraph.class, 12, "sectionId", 10L, "sourceKey", "P1-h", "sortOrder", 1),
                entity(PassageParagraph.class, 31, "sectionId", 30L, "sourceKey", "P1-other", "sortOrder", 1),
                entity(OptionSet.class, 13, "sectionId", 10L, "code", "shared"),
                entity(OptionSet.class, 33, "sectionId", 30L, "code", "shared"),
                entity(QuestionOption.class, 14, "optionSetId", 13L, "label", "B", "content", "Second", "sortOrder", 2),
                entity(QuestionOption.class, 15, "optionSetId", 13L, "label", "A", "content", "First", "sortOrder", 1),
                entity(QuestionOption.class, 34, "optionSetId", 33L, "label", "C", "sortOrder", 1),
                entity(Question.class, 16, "sectionId", 10L, "questionNo", 1, "sortOrder", 2, "optionSetId", 13L,
                        "solveSteps", List.of("定位", "核对"), "distractors", Map.of("B", "理由"),
                        "knowledge", Map.of("title", "知识点"), "logicLinks", List.of(Map.of("label", "线索"))),
                entity(Question.class, 17, "sectionId", 10L, "questionNo", 2, "sortOrder", 1),
                entity(Question.class, 36, "sectionId", 30L, "questionNo", 1, "sortOrder", 1),
                entity(QuestionAnswer.class, 18, "questionId", 16L, "rawAnswer", List.of("A", "B"),
                        "acceptedAnswers", new String[]{"A", "B"}),
                entity(QuestionAnswer.class, 19, "questionId", 17L, "rawAnswer", "hadn't/had not EXPECTED to see"),
                entity(QuestionAnswer.class, 38, "questionId", 36L, "rawAnswer", "C"),
                entity(QuestionEvidence.class, 21, "sectionId", 10L, "questionId", 16L, "paragraphId", 11L, "quote", "{{1}}", "sortOrder", 2),
                entity(QuestionEvidence.class, 22, "sectionId", 10L, "questionId", 16L, "paragraphId", 12L, "quote", "Heading", "sortOrder", 1),
                entity(QuestionEvidence.class, 39, "sectionId", 30L, "questionId", 36L, "paragraphId", 31L, "sortOrder", 1));
        assertThat(sections.findByPaperIdOrderBySortOrderAscIdAsc(1L)).extracting(ExamSection::getId).containsExactly(20L, 10L);
        assertThat(sections.findByPaperIdAndCode(2L, "P1").orElseThrow().getId()).isEqualTo(30L);
        assertThat(sections.findByPaperIdAndCode(1L, "P7")).isEmpty();
        assertThat(sections.findByPaperIdAndCode(1L, "P1").orElseThrow().getSourceGroups())
                .containsExactly(Map.of("title", "正文", "paragraph_ids", List.of("P1-p1")));
        assertThat(paragraphs.findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L)))
                .extracting(PassageParagraph::getSourceKey).containsExactly("P1-h", "P1-p1");
        assertThat(sets.findBySectionIdIn(List.of(10L))).extracting(OptionSet::getId).containsExactly(13L);
        assertThat(options.findByOptionSetIdInOrderBySortOrderAscIdAsc(List.of(13L)))
                .extracting(QuestionOption::getLabel).containsExactly("A", "B");
        var questionRows = questions.findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L));
        assertThat(questionRows).extracting(Question::getQuestionNo).containsExactly(2, 1);
        assertThat(questionRows.get(1).getSolveSteps()).containsExactly("定位", "核对");
        assertThat(questionRows.get(1).getDistractors()).containsEntry("B", "理由");
        assertThat(questionRows.get(1).getKnowledge()).containsEntry("title", "知识点");
        assertThat(questionRows.get(1).getLogicLinks()).containsExactly(Map.of("label", "线索"));
        var answerRows = answers.findByQuestionIdIn(List.of(16L, 17L));
        assertThat(answerRows).hasSize(2);
        assertThat(answerRows).filteredOn(a -> a.getQuestionId().equals(16L)).singleElement().satisfies(a -> {
            assertThat(a.getRawAnswer()).isEqualTo(List.of("A", "B"));
            assertThat(a.getAcceptedAnswers()).containsExactly("A", "B");
        });
        assertThat(answerRows).filteredOn(a -> a.getQuestionId().equals(17L)).singleElement()
                .satisfies(a -> assertThat(a.getRawAnswer()).isEqualTo("hadn't/had not EXPECTED to see"));
        assertThat(evidence.findByQuestionIdInOrderBySortOrderAscIdAsc(List.of(16L)))
                .extracting(QuestionEvidence::getQuote).containsExactly("Heading", "{{1}}");
    }
}
