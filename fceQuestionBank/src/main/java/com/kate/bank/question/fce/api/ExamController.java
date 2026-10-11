package com.kate.bank.question.fce.api;

import com.kate.bank.question.fce.api.dto.CatalogResponse;
import com.kate.bank.question.fce.api.dto.PaperResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse;
import com.kate.bank.question.fce.service.CatalogService;
import com.kate.bank.question.fce.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public read API for the FCE library and published Reading and Use of English papers. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExamController {
    private final CatalogService catalogService;
    private final ExamService examService;

    @GetMapping("/catalog")
    public CatalogResponse catalog() {
        return catalogService.getCatalog();
    }

    @GetMapping("/papers/{slug}")
    public PaperResponse paper(@PathVariable String slug) {
        return examService.getPaper(slug);
    }

    @GetMapping("/papers/{slug}/sections/{code}")
    public SectionResponse section(@PathVariable String slug, @PathVariable String code) {
        return examService.getSection(slug, code);
    }
}
