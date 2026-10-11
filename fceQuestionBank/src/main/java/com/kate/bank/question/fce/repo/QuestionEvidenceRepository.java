package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.QuestionEvidence;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface QuestionEvidenceRepository extends Repository<QuestionEvidence, Long> {
    List<QuestionEvidence> findByQuestionIdInOrderBySortOrderAscIdAsc(Collection<Long> questionIds);
}
