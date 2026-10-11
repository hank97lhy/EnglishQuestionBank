package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Read-only mapping of the Flyway-managed exam_paper table. */
@Entity
@Table(name = "exam_paper")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamPaper extends BaseEntity {
    @Column(name = "book_id")
    private Long bookId;
    @Column(name = "test_no")
    private Integer testNo;
    @Column(name = "slug")
    private String slug;
    @Column(name = "title")
    private String title;
    @Column(name = "subtitle")
    private String subtitle;
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    @Column(name = "status")
    private String status;
    @Column(name = "edition_note")
    private String editionNote;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_metadata", columnDefinition = "jsonb")
    private Map<String, Object> sourceMetadata;
}
