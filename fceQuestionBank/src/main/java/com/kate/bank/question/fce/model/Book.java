package com.kate.bank.question.fce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/** Read-only mapping of the Flyway-managed book table. */
@Entity
@Table(name = "book")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {
    @Column(name = "edition_id")
    private Long editionId;
    @Column(name = "book_no")
    private Integer bookNo;
    @Column(name = "title")
    private String title;
    @Column(name = "sort_order")
    private Integer sortOrder;
}
