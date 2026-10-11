package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.QuestionOption;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface QuestionOptionRepository extends Repository<QuestionOption, Long> {
    List<QuestionOption> findByOptionSetIdInOrderBySortOrderAscIdAsc(Collection<Long> optionSetIds);
}
