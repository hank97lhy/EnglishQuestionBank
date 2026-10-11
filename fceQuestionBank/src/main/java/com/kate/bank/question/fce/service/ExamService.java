package com.kate.bank.question.fce.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.kate.bank.question.fce.api.dto.PaperResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse.EvidenceResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse.ParagraphResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse.QuestionResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamService {
    private final ExamPaperRepository papers;
    private final ExamSectionRepository sections;
    private final PassageParagraphRepository paragraphs;
    private final OptionSetRepository optionSets;
    private final QuestionOptionRepository options;
    private final QuestionRepository questions;
    private final QuestionAnswerRepository answers;
    private final QuestionEvidenceRepository evidence;

    public PaperResponse getPaper(String slug) {
        var paper = publishedPaper(slug);
        var content = loadSections(sections.findByPaperIdOrderBySortOrderAscIdAsc(paper.getId()));
        int questionCount = content.stream().mapToInt(section -> section.questions().size()).sum();
        return new PaperResponse(paper.getId().toString(), paper.getSlug(), paper.getTitle(), paper.getSubtitle(),
                paper.getDurationMinutes(), paper.getEditionNote(), paper.getSourceMetadata(),
                new PaperResponse.Summary(content.size(), questionCount), content);
    }

    public SectionResponse getSection(String slug, String code) {
        var paper = publishedPaper(slug);
        var section = sections.findByPaperIdAndCode(paper.getId(), code)
                .orElseThrow(() -> notFound("Part not found"));
        return loadSections(List.of(section)).getFirst();
    }

    private ExamPaper publishedPaper(String slug) {
        return papers.findBySlugAndStatus(slug, "published")
                .orElseThrow(() -> notFound("Published paper not found"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    /** Fetch each table once per request, avoiding per-question database queries. */
    private List<SectionResponse> loadSections(List<ExamSection> requestedSections) {
        if (requestedSections.isEmpty()) {
            return List.of();
        }
        var sectionIds = requestedSections.stream().map(ExamSection::getId).toList();
        var paragraphRows = paragraphs.findBySectionIdInOrderBySortOrderAscIdAsc(sectionIds);
        var paragraphById = paragraphRows.stream().collect(Collectors.toMap(PassageParagraph::getId, Function.identity()));
        var paragraphsBySection = paragraphRows.stream().collect(Collectors.groupingBy(PassageParagraph::getSectionId));
        var sets = optionSets.findBySectionIdIn(sectionIds);
        var setIds = sets.stream().map(OptionSet::getId).toList();
        List<QuestionOption> optionRows = setIds.isEmpty() ? List.of()
                : options.findByOptionSetIdInOrderBySortOrderAscIdAsc(setIds);
        var optionsBySet = optionRows.stream().collect(Collectors.groupingBy(QuestionOption::getOptionSetId));
        var questionRows = questions.findBySectionIdInOrderBySortOrderAscIdAsc(sectionIds);
        var questionIds = questionRows.stream().map(Question::getId).toList();
        List<QuestionAnswer> answerRows = questionIds.isEmpty() ? List.of() : answers.findByQuestionIdIn(questionIds);
        List<QuestionEvidence> evidenceRows = questionIds.isEmpty() ? List.of()
                : evidence.findByQuestionIdInOrderBySortOrderAscIdAsc(questionIds);
        var answersByQuestion = answerRows.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity()));
        var evidenceByQuestion = evidenceRows.stream().collect(Collectors.groupingBy(QuestionEvidence::getQuestionId));
        var questionsBySection = questionRows.stream().collect(Collectors.groupingBy(Question::getSectionId));

        return requestedSections.stream().map(section -> {
            var sharedOptions = sets.stream()
                    .filter(set -> set.getSectionId().equals(section.getId()) && "shared".equals(set.getCode()))
                    .findFirst().map(set -> optionMap(optionsBySet.getOrDefault(set.getId(), List.of())))
                    .orElseGet(Map::of);
            return new SectionResponse(section.getId().toString(), section.getCode(), "Part " + section.getPartNo(),
                    section.getPartNo(), kind(section.getPartNo()), section.getQuestionFormat(), section.getTitle(),
                    section.getPaperReference(), section.getExpectedOptionCount(), section.getSourceGroups(), sharedOptions,
                    paragraphsBySection.getOrDefault(section.getId(), List.of()).stream()
                            .map(p -> new ParagraphResponse(p.getId().toString(), p.getSourceKey(), p.getRole(),
                                    p.getText(), p.getTranslation())).toList(),
                    questionsBySection.getOrDefault(section.getId(), List.of()).stream().map(question -> {
                        var answer = answersByQuestion.get(question.getId());
                        var citations = evidenceByQuestion.getOrDefault(question.getId(), List.of()).stream()
                                .map(item -> {
                                    var paragraph = paragraphById.get(item.getParagraphId());
                                    if (paragraph == null || !paragraph.getSectionId().equals(question.getSectionId())) {
                                        throw new IllegalStateException("Evidence references an invalid paragraph");
                                    }
                                    return new EvidenceResponse(paragraph.getSourceKey(), item.getQuote());
                                }).toList();
                        return new QuestionResponse(question.getId().toString(), question.getQuestionNo().toString(),
                                question.getQuestionNo(), question.getStem(),
                                optionMap(optionsBySet.getOrDefault(question.getOptionSetId(), List.of())),
                                answer == null ? null : answer.getRawAnswer(),
                                answer == null ? null : answer.getDisplayText(),
                                answer == null ? null : answer.getAnswerStatus(),
                                answer == null ? null : answer.getAnswerSource(),
                                question.getQuestionTypeLabel(), question.getAnalysis(), question.getTypeNote(),
                                question.getStrategy(), question.getPitfall(), question.getSolveSteps(),
                                question.getDistractors(), question.getKnowledge(), question.getLogicLinks(), citations);
                    }).toList());
        }).toList();
    }

    private Map<String, String> optionMap(Collection<QuestionOption> rows) {
        var result = new LinkedHashMap<String, String>();
        rows.forEach(option -> result.put(option.getLabel(), option.getContent()));
        return result;
    }

    private String kind(int partNo) {
        return switch (partNo) {
            case 1 -> "cloze";
            case 2, 3, 4 -> "grammar";
            case 5, 7 -> "reading";
            case 6 -> "seven";
            default -> throw new IllegalStateException("Unsupported Part number: " + partNo);
        };
    }
}
