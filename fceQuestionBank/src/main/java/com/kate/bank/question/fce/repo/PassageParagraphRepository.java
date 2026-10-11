package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.PassageParagraph;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface PassageParagraphRepository extends Repository<PassageParagraph, Long> {
    List<PassageParagraph> findBySectionIdInOrderBySortOrderAscIdAsc(Collection<Long> sectionIds);
}
