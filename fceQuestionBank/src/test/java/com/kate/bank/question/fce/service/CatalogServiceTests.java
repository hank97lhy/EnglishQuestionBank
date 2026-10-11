package com.kate.bank.question.fce.service;

import java.util.List;
import com.kate.bank.question.fce.model.Book;
import com.kate.bank.question.fce.model.Edition;
import com.kate.bank.question.fce.model.ExamPaper;
import com.kate.bank.question.fce.repo.BookRepository;
import com.kate.bank.question.fce.repo.EditionRepository;
import com.kate.bank.question.fce.repo.ExamPaperRepository;
import org.junit.jupiter.api.Test;
import static com.kate.bank.question.fce.support.ModelFixtures.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceTests {
    private final EditionRepository editions = mock(EditionRepository.class);
    private final BookRepository books = mock(BookRepository.class);
    private final ExamPaperRepository papers = mock(ExamPaperRepository.class);
    private final CatalogService service = new CatalogService(editions, books, papers);

    @Test
    // 测试目的：验证目录按所属版本/册分组，并正确区分 published 与 pending，保留大整数 ID。
    void groupsCatalogAndMarksOnlyPublishedPapersAvailable() {
        long largeId = 9007199254740993L;
        when(editions.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                entity(Edition.class, largeId, "code", "standard", "name", "标准版"),
                entity(Edition.class, 2, "code", "campus", "name", "校园版")));
        when(books.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                entity(Book.class, 10, "editionId", largeId, "bookNo", 1, "title", "标准版 1"),
                entity(Book.class, 20, "editionId", 2L, "bookNo", 3, "title", "校园版 3")));
        when(papers.findAllByStatusInOrderByTestNoAscIdAsc(List.of("pending", "published"))).thenReturn(List.of(
                entity(ExamPaper.class, 100, "bookId", 20L, "testNo", 1, "status", "published"),
                entity(ExamPaper.class, 101, "bookId", 10L, "testNo", 2, "status", "pending")));
        var result = service.getCatalog();
        assertThat(result.editions()).extracting(item -> item.code()).containsExactly("standard", "campus");
        assertThat(result.editions().getFirst().id()).isEqualTo("9007199254740993");
        assertThat(result.editions().getFirst().books().getFirst().tests().getFirst().available()).isFalse();
        assertThat(result.editions().get(1).books().getFirst().tests().getFirst().available()).isTrue();
        verify(papers).findAllByStatusInOrderByTestNoAscIdAsc(List.of("pending", "published"));
    }

    @Test
    // 测试目的：验证没有目录数据时返回空集合而非 null。
    void returnsEmptyCatalog() {
        assertThat(service.getCatalog().editions()).isEmpty();
    }

    @Test
    // 测试目的：验证空版本和空册仍然显示，不会因为没有试卷被丢弃。
    void retainsEmptyEditionsAndBooks() {
        when(editions.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                entity(Edition.class, 1, "code", "standard", "name", "标准版"),
                entity(Edition.class, 2, "code", "campus", "name", "校园版")));
        when(books.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                entity(Book.class, 10, "editionId", 1L, "bookNo", 1, "title", "标准版 1")));
        var result = service.getCatalog();
        assertThat(result.editions().getFirst().books().getFirst().tests()).isEmpty();
        assertThat(result.editions().get(1).books()).isEmpty();
    }
}
