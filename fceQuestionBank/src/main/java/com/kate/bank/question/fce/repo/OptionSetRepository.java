package com.kate.bank.question.fce.repo;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.Repository;
import com.kate.bank.question.fce.model.OptionSet;

/** Read-only queries; deliberately exposes no save or delete operations. */
public interface OptionSetRepository extends Repository<OptionSet, Long> {
    List<OptionSet> findBySectionIdIn(Collection<Long> sectionIds);
}
