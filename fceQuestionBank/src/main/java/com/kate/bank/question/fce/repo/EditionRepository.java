package com.kate.bank.question.fce.repo;

import java.util.List;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.Edition;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface EditionRepository extends Repository<Edition, Long> {
    List<Edition> findAllByOrderBySortOrderAscIdAsc();
}
