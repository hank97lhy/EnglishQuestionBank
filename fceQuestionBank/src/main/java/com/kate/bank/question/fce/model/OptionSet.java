package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed option_set table. */
@Entity
@Table(name = "option_set")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionSet extends BaseEntity {
    @Column(name = "section_id")
    private Long sectionId;
    @Column(name = "code")
    private String code;
}
