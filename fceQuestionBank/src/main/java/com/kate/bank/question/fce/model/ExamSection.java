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

/** Read-only mapping of the Flyway-managed exam_section table. */
@Entity
@Table(name = "exam_section")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamSection extends BaseEntity {
    @Column(name = "paper_id")
    private Long paperId;
    @Column(name = "code")
    private String code;
    @Column(name = "part_no")
    private Integer partNo;
    @Column(name = "title")
    private String title;
    @Column(name = "question_format")
    private String questionFormat;
    @Column(name = "paper_reference")
    private String paperReference;
    @Column(name = "expected_option_count")
    private Integer expectedOptionCount;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_groups", columnDefinition = "jsonb")
    private List<Map<String, Object>> sourceGroups;
}
