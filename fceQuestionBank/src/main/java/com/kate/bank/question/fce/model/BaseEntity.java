package com.kate.bank.question.fce.model;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/** Shared identity and audit columns; schema changes remain managed by Flyway. */
@Getter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
