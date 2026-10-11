package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed passage_paragraph table. */
@Entity
@Table(name = "passage_paragraph")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PassageParagraph extends BaseEntity {
    @Column(name = "section_id")
    private Long sectionId;
    @Column(name = "source_key")
    private String sourceKey;
    @Column(name = "role")
    private String role;
    @Column(name = "text")
    private String text;
    @Column(name = "translation")
    private String translation;
    @Column(name = "sort_order")
    private Integer sortOrder;
}
