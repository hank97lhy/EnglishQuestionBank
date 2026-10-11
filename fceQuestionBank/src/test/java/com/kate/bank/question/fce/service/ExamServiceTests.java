package com.kate.bank.question.fce.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kate.bank.question.fce.model.ExamPaper;
import com.kate.bank.question.fce.model.ExamSection;
import com.kate.bank.question.fce.model.OptionSet;
import com.kate.bank.question.fce.model.PassageParagraph;
import com.kate.bank.question.fce.model.Question;
import com.kate.bank.question.fce.model.QuestionAnswer;
import com.kate.bank.question.fce.model.QuestionEvidence;
import com.kate.bank.question.fce.model.QuestionOption;
import com.kate.bank.question.fce.repo.ExamPaperRepository;
import com.kate.bank.question.fce.repo.ExamSectionRepository;
import com.kate.bank.question.fce.repo.OptionSetRepository;
import com.kate.bank.question.fce.repo.PassageParagraphRepository;
import com.kate.bank.question.fce.repo.QuestionAnswerRepository;
import com.kate.bank.question.fce.repo.QuestionEvidenceRepository;
import com.kate.bank.question.fce.repo.QuestionOptionRepository;
import com.kate.bank.question.fce.repo.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.kate.bank.question.fce.support.ModelFixtures.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExamServiceTests {
    private final ExamPaperRepository papers = mock(ExamPaperRepository.class);
    private final ExamSectionRepository sections = mock(ExamSectionRepository.class);
    private final PassageParagraphRepository paragraphs = mock(PassageParagraphRepository.class);
    private final OptionSetRepository sets = mock(OptionSetRepository.class);
    private final QuestionOptionRepository options = mock(QuestionOptionRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionAnswerRepository answers = mock(QuestionAnswerRepository.class);
    private final QuestionEvidenceRepository evidence = mock(QuestionEvidenceRepository.class);
    private final ExamService service = new ExamService(papers, sections, paragraphs, sets, options, questions, answers, evidence);
    private static final String SLUG = "standard-1-test-1";

    private ExamSection section(int partNo) {
        return entity(ExamSection.class, 10, "paperId", 1L, "code", "P" + partNo, "partNo", partNo,
                "title", "Part " + partNo, "questionFormat", "gapped_text", "paperReference", "原卷 Part 6",
                "expectedOptionCount", 7, "sourceGroups", List.of(Map.of("title", "A", "paragraph_ids", List.of("P6-p1"))));
    }

    private void published() {
        when(papers.findBySlugAndStatus(SLUG, "published")).thenReturn(Optional.of(
                entity(ExamPaper.class, 1, "slug", SLUG, "title", "试卷", "subtitle", "阅读与语用",
                        "durationMinutes", 75, "editionNote", "更新版", "sourceMetadata", Map.of("exam_page", 2))));
    }

    private void content() {
        published();
        when(sections.findByPaperIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(section(6)));
        when(paragraphs.findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L))).thenReturn(List.of(
                entity(PassageParagraph.class, 11, "sectionId", 10L, "sourceKey", "P6-p1", "role", "paragraph",
                        "text", "First line\n{{37}}", "translation", "第一行\n{{37}}")));
        when(sets.findBySectionIdIn(List.of(10L))).thenReturn(List.of(
                entity(OptionSet.class, 12, "sectionId", 10L, "code", "shared")));
        when(options.findByOptionSetIdInOrderBySortOrderAscIdAsc(List.of(12L))).thenReturn(List.of(
                entity(QuestionOption.class, 13, "optionSetId", 12L, "label", "A", "content", "Choice A"),
                entity(QuestionOption.class, 14, "optionSetId", 12L, "label", "B", "content", "Choice B")));
        when(questions.findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L))).thenReturn(List.of(
                entity(Question.class, 9007199254740993L, "sectionId", 10L, "questionNo", 37, "optionSetId", 12L,
                        "stem", "Stem\n{{37}}", "questionTypeLabel", "衔接", "analysis", "原因", "typeNote", "说明",
                        "strategy", "策略", "pitfall", "易错点", "solveSteps", List.of("第一步", "第二步"),
                        "distractors", Map.of("B", "不匹配"), "knowledge", Map.of("title", "知识点"),
                        "logicLinks", List.of(Map.of("label", "线索"))),
                entity(Question.class, 16, "sectionId", 10L, "questionNo", 38, "optionSetId", 12L)));
        when(answers.findByQuestionIdIn(List.of(9007199254740993L, 16L))).thenReturn(List.of(
                entity(QuestionAnswer.class, 17, "questionId", 9007199254740993L, "rawAnswer", "A",
                        "displayText", "A", "answerStatus", "official", "answerSource", "答案页"),
                entity(QuestionAnswer.class, 18, "questionId", 16L, "rawAnswer", List.of("B", "C"),
                        "displayText", "B / C", "answerStatus", "official", "answerSource", "答案页")));
        when(evidence.findByQuestionIdInOrderBySortOrderAscIdAsc(List.of(9007199254740993L, 16L))).thenReturn(List.of(
                entity(QuestionEvidence.class, 19, "sectionId", 10L, "questionId", 9007199254740993L,
                        "paragraphId", 11L, "quote", "{{37}}")));
    }

    @Test
    // 测试目的：验证原文、译文、题干和解析完整保留，并区分数据库 ID、源题号与源段落标识。
    void preservesContentAndRestoresSourceReferences() {
        content();
        var paper = service.getPaper(SLUG);
        assertThat(paper.examSummary().sections()).isEqualTo(1);
        assertThat(paper.examSummary().questions()).isEqualTo(2);
        assertThat(paper.origin()).containsEntry("exam_page", 2);
        var part = paper.sections().getFirst();
        assertThat(part.paragraphs().getFirst().text()).isEqualTo("First line\n{{37}}");
        assertThat(part.paragraphs().getFirst().translation()).isEqualTo("第一行\n{{37}}");
        assertThat(part.sourceGroups()).isEqualTo(section(6).getSourceGroups());
        var question = part.questions().getFirst();
        assertThat(question.databaseId()).isEqualTo("9007199254740993");
        assertThat(question.id()).isEqualTo("37");
        assertThat(question.stem()).isEqualTo("Stem\n{{37}}");
        assertThat(question.evidence().getFirst().paragraphId()).isEqualTo("P6-p1");
        assertThat(question.evidence().getFirst().quote()).isEqualTo("{{37}}");
        assertThat(question.questionType()).isEqualTo("衔接");
        assertThat(question.analysis()).isEqualTo("原因");
        assertThat(question.typeNote()).isEqualTo("说明");
        assertThat(question.strategy()).isEqualTo("策略");
        assertThat(question.pitfall()).isEqualTo("易错点");
        assertThat(question.solveSteps()).containsExactly("第一步", "第二步");
        assertThat(question.distractors()).containsEntry("B", "不匹配");
        assertThat(question.knowledge()).containsEntry("title", "知识点");
        assertThat(question.logicLinks()).containsExactly(Map.of("label", "线索"));
    }

    @Test
    // 测试目的：验证 Part 6/7 共用选项按顺序展开至每题，且整卷只批量查询一次选项与答案。
    void expandsSharedOptionsWithoutPerQuestionQueries() {
        content();
        var part = service.getPaper(SLUG).sections().getFirst();
        assertThat(part.sharedOptions().keySet()).containsExactly("A", "B");
        assertThat(part.questions()).allSatisfy(q -> assertThat(q.options()).isEqualTo(part.sharedOptions()));
        verify(options, times(1)).findByOptionSetIdInOrderBySortOrderAscIdAsc(List.of(12L));
        verify(answers, times(1)).findByQuestionIdIn(List.of(9007199254740993L, 16L));
    }

    @Test
    // 测试目的：验证答案字符串与字符串数组原始类型、展示文本及出处不会被改变。
    void preservesRawAnswerTypes() {
        content();
        var rows = service.getPaper(SLUG).sections().getFirst().questions();
        assertThat(rows.getFirst().answer()).isEqualTo("A");
        assertThat(rows.get(1).answer()).isEqualTo(List.of("B", "C"));
        assertThat(rows.get(1).answerDisplay()).isEqualTo("B / C");
        assertThat(rows.getFirst().answerStatus()).isEqualTo("official");
        assertThat(rows.getFirst().answerSource()).isEqualTo("答案页");
    }

    @Test
    // 测试目的：验证包含局部替换斜杠的答案原样返回，不错误拆分为等价完整答案。
    void neverSplitsSlashInAnswer() {
        content();
        String raw = "hadn't/had not EXPECTED to see";
        when(answers.findByQuestionIdIn(List.of(9007199254740993L, 16L))).thenReturn(List.of(
                entity(QuestionAnswer.class, 17, "questionId", 9007199254740993L, "rawAnswer", raw, "displayText", raw)));
        var result = service.getPaper(SLUG).sections().getFirst().questions().getFirst();
        assertThat(result.answer()).isEqualTo(raw);
        assertThat(result.answerDisplay()).isEqualTo(raw);
    }

    @Test
    // 测试目的：验证无选项题和缺失答案可安全投影为空选项、空证据和 null 答案。
    void handlesQuestionsWithoutOptionsOrAnswers() {
        published();
        when(sections.findByPaperIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(section(2)));
        when(questions.findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L))).thenReturn(List.of(
                entity(Question.class, 16, "sectionId", 10L, "questionNo", 9)));
        var result = service.getPaper(SLUG).sections().getFirst().questions().getFirst();
        assertThat(result.options()).isEmpty();
        assertThat(result.evidence()).isEmpty();
        assertThat(result.answer()).isNull();
        verifyNoInteractions(options);
    }

    @Test
    // 测试目的：验证无 Part 的试卷返回零统计，避免执行空 IN 查询。
    void handlesEmptyPaperWithoutChildQueries() {
        published();
        var result = service.getPaper(SLUG);
        assertThat(result.sections()).isEmpty();
        assertThat(result.examSummary().questions()).isZero();
        verifyNoInteractions(paragraphs, sets, options, questions, answers, evidence);
    }

    @Test
    // 测试目的：验证空 Part 不执行选项或答案的空 IN 查询。
    void handlesEmptySection() {
        published();
        when(sections.findByPaperIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(section(1)));
        assertThat(service.getPaper(SLUG).sections().getFirst().questions()).isEmpty();
        verifyNoInteractions(options, answers, evidence);
    }

    @Test
    // 测试目的：验证不存在、pending 或 draft 试卷均通过 published 查询限制返回 404。
    void unavailablePaperIsNotFound() {
        assertThatThrownBy(() -> service.getPaper(SLUG)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(papers).findBySlugAndStatus(SLUG, "published");
        verifyNoInteractions(sections, paragraphs, questions);
    }

    @Test
    // 测试目的：验证查询单 Part 时先限制已发布试卷，Part 不存在返回 404。
    void missingSectionIsNotFound() {
        published();
        assertThatThrownBy(() -> service.getSection(SLUG, "P9"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(sections).findByPaperIdAndCode(1L, "P9");
        verifyNoInteractions(paragraphs, questions);
    }

    @Test
    // 测试目的：验证单 Part 接口只读取目标 Part，并与整卷投影保持一致。
    void readsOnlyRequestedSection() {
        content();
        when(sections.findByPaperIdAndCode(1L, "P6")).thenReturn(Optional.of(section(6)));
        assertThat(service.getSection(SLUG, "P6").questions()).hasSize(2);
        verify(sections).findByPaperIdAndCode(1L, "P6");
        verify(paragraphs).findBySectionIdInOrderBySortOrderAscIdAsc(List.of(10L));
    }

    @Test
    // 测试目的：验证无效证据引用会明确报错，不向网页输出错误段落定位。
    void rejectsEvidenceOutsideLoadedParagraphs() {
        content();
        when(evidence.findByQuestionIdInOrderBySortOrderAscIdAsc(List.of(9007199254740993L, 16L)))
                .thenReturn(List.of(entity(QuestionEvidence.class, 19, "questionId", 9007199254740993L,
                        "paragraphId", 999L)));
        assertThatThrownBy(() -> service.getPaper(SLUG)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Evidence references an invalid paragraph");
    }

    @ParameterizedTest
    @CsvSource({"1,cloze", "2,grammar", "3,grammar", "4,grammar", "5,reading", "6,seven", "7,reading"})
    // 测试目的：验证 7 种 Part 映射至静态网页渲染所需的 kind，而非直接使用数据库题型代码。
    void mapsAllStaticKinds(int partNo, String expectedKind) {
        published();
        when(sections.findByPaperIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(section(partNo)));
        assertThat(service.getPaper(SLUG).sections().getFirst().kind()).isEqualTo(expectedKind);
    }
}
