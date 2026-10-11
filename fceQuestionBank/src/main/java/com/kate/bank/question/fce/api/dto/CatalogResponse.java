package com.kate.bank.question.fce.api.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CatalogResponse(List<EditionItem> editions) {
    public record EditionItem(String id, String code, String name, List<BookItem> books) {}
    public record BookItem(String id, @JsonProperty("book_no") int bookNo,
                           String title, List<PaperItem> tests) {}
    public record PaperItem(String id, @JsonProperty("test_no") int testNo, String slug,
                            String title, String subtitle, String status,
                            @JsonProperty("duration_minutes") Integer durationMinutes, boolean available) {}
}
