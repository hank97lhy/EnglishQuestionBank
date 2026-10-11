package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Read-only mapping of the Flyway-managed question_answer table. */
@Entity
@Table(name = "question_answer")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAnswer extends BaseEntity {
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "display_text")
    private String displayText;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_answer", columnDefinition = "jsonb")
    private Object rawAnswer;
    @Column(name = "accepted_answers")
    private String[] acceptedAnswers;
    @Column(name = "answer_status")
    private String answerStatus;
    @Column(name = "answer_source")
    private String answerSource;
}
