package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed question_evidence table. */
@Entity
@Table(name = "question_evidence")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionEvidence extends BaseEntity {
    @Column(name = "section_id")
    private Long sectionId;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "paragraph_id")
    private Long paragraphId;
    @Column(name = "quote")
    private String quote;
    @Column(name = "sort_order")
    private Integer sortOrder;
}
