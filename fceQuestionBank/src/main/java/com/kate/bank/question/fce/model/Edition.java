package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed edition table. */
@Entity
@Table(name = "edition")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Edition extends BaseEntity {
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "sort_order")
    private Integer sortOrder;
}
