package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.QuestionAnswer;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface QuestionAnswerRepository extends Repository<QuestionAnswer, Long> {
    List<QuestionAnswer> findByQuestionIdIn(Collection<Long> questionIds);
}
