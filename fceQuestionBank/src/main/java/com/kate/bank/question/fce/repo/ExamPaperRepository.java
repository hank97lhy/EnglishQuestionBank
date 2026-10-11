package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.ExamPaper;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface ExamPaperRepository extends Repository<ExamPaper, Long> {
    List<ExamPaper> findAllByStatusInOrderByTestNoAscIdAsc(Collection<String> statuses);
    Optional<ExamPaper> findBySlugAndStatus(String slug, String status);
}
