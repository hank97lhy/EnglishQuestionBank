package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Read-only mapping of the Flyway-managed question table. */
@Entity
@Table(name = "question")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {
    @Column(name = "section_id")
    private Long sectionId;
    @Column(name = "question_no")
    private Integer questionNo;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(name = "stem")
    private String stem;
    @Column(name = "option_set_id")
    private Long optionSetId;
    @Column(name = "question_type_label")
    private String questionTypeLabel;
    @Column(name = "analysis")
    private String analysis;
    @Column(name = "type_note")
    private String typeNote;
    @Column(name = "strategy")
    private String strategy;
    @Column(name = "pitfall")
    private String pitfall;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "solve_steps", columnDefinition = "jsonb")
    private List<String> solveSteps;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distractors", columnDefinition = "jsonb")
    private Map<String, Object> distractors;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "knowledge", columnDefinition = "jsonb")
    private Map<String, Object> knowledge;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "logic_links", columnDefinition = "jsonb")
    private List<Map<String, Object>> logicLinks;
}
