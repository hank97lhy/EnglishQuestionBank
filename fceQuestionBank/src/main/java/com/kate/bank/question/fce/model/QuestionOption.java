package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed question_option table. */
@Entity
@Table(name = "question_option")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionOption extends BaseEntity {
    @Column(name = "option_set_id")
    private Long optionSetId;
    @Column(name = "label")
    private String label;
    @Column(name = "content")
    private String content;
    @Column(name = "sort_order")
    private Integer sortOrder;
}
