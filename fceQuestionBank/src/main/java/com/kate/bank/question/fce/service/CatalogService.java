package com.kate.bank.question.fce.service;

import java.util.List;
import java.util.stream.Collectors;
import com.kate.bank.question.fce.api.dto.CatalogResponse;
import com.kate.bank.question.fce.api.dto.CatalogResponse.BookItem;
import com.kate.bank.question.fce.api.dto.CatalogResponse.EditionItem;
import com.kate.bank.question.fce.api.dto.CatalogResponse.PaperItem;
import com.kate.bank.question.fce.model.Book;
import com.kate.bank.question.fce.model.ExamPaper;
import com.kate.bank.question.fce.repo.BookRepository;
import com.kate.bank.question.fce.repo.EditionRepository;
import com.kate.bank.question.fce.repo.ExamPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {
    private final EditionRepository editions;
    private final BookRepository books;
    private final ExamPaperRepository papers;

    public CatalogResponse getCatalog() {
        var booksByEdition = books.findAllByOrderBySortOrderAscIdAsc().stream()
                .collect(Collectors.groupingBy(Book::getEditionId));
        var papersByBook = papers.findAllByStatusInOrderByTestNoAscIdAsc(List.of("pending", "published"))
                .stream().collect(Collectors.groupingBy(ExamPaper::getBookId));
        return new CatalogResponse(editions.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(edition -> new EditionItem(edition.getId().toString(), edition.getCode(), edition.getName(),
                        booksByEdition.getOrDefault(edition.getId(), List.of()).stream()
                                .map(book -> new BookItem(book.getId().toString(), book.getBookNo(), book.getTitle(),
                                        papersByBook.getOrDefault(book.getId(), List.of()).stream()
                                                .map(this::toPaperItem).toList())).toList())).toList());
    }

    private PaperItem toPaperItem(ExamPaper paper) {
        return new PaperItem(paper.getId().toString(), paper.getTestNo(), paper.getSlug(), paper.getTitle(),
                paper.getSubtitle(), paper.getStatus(), paper.getDurationMinutes(),
                "published".equals(paper.getStatus()));
    }
}
