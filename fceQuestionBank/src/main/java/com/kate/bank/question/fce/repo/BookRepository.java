package com.kate.bank.question.fce.repo;

import java.util.List;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.Book;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface BookRepository extends Repository<Book, Long> {
    List<Book> findAllByOrderBySortOrderAscIdAsc();
}
