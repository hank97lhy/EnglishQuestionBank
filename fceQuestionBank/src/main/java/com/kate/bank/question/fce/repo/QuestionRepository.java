package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.Question;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface QuestionRepository extends Repository<Question, Long> {
    List<Question> findBySectionIdInOrderBySortOrderAscIdAsc(Collection<Long> sectionIds);
}
