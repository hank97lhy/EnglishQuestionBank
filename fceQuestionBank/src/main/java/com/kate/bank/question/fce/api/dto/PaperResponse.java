package com.kate.bank.question.fce.api.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Database content projected onto the static examData field names. */
public record PaperResponse(@JsonProperty("database_id") String databaseId, String slug,
        String title, String subtitle, @JsonProperty("duration_minutes") Integer durationMinutes,
        String edition, Map<String, Object> origin,
        @JsonProperty("exam_summary") Summary examSummary, List<SectionResponse> sections) {
    public record Summary(int sections, int questions) {}
}
