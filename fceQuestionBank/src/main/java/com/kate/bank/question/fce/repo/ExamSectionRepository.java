package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.ExamSection;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface ExamSectionRepository extends Repository<ExamSection, Long> {
    List<ExamSection> findByPaperIdOrderBySortOrderAscIdAsc(Long paperId);
    Optional<ExamSection> findByPaperIdAndCode(Long paperId, String code);
}
