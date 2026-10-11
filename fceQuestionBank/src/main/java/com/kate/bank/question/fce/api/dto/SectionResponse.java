package com.kate.bank.question.fce.api.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SectionResponse(@JsonProperty("database_id") String databaseId, String id,
        String part, @JsonProperty("part_no") int partNo, String kind,
        @JsonProperty("question_format") String questionFormat, String title,
        @JsonProperty("paper_reference") String paperReference,
        @JsonProperty("expected_option_count") Integer expectedOptionCount,
        @JsonProperty("source_groups") List<Map<String, Object>> sourceGroups,
        @JsonProperty("shared_options") Map<String, String> sharedOptions,
        List<ParagraphResponse> paragraphs, List<QuestionResponse> questions) {
    public record ParagraphResponse(@JsonProperty("database_id") String databaseId, String id,
                                     String role, String text, String translation) {}
    public record EvidenceResponse(@JsonProperty("paragraph_id") String paragraphId, String quote) {}
    public record QuestionResponse(@JsonProperty("database_id") String databaseId, String id,
            @JsonProperty("question_no") int questionNo, String stem, Map<String, String> options,
            Object answer, @JsonProperty("answer_display") String answerDisplay,
            @JsonProperty("answer_status") String answerStatus,
            @JsonProperty("answer_source") String answerSource,
            @JsonProperty("question_type") String questionType, String analysis,
            @JsonProperty("type_note") String typeNote, String strategy, String pitfall,
            @JsonProperty("solve_steps") List<String> solveSteps, Map<String, Object> distractors,
            Map<String, Object> knowledge, @JsonProperty("logic_links") List<Map<String, Object>> logicLinks,
            List<EvidenceResponse> evidence) {}
}
