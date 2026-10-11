package com.kate.bank.question.fce.support;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Constructs read-only entities without adding mutable setters to production models. */
public final class ModelFixtures {
    private ModelFixtures() {}

    public static <T> T entity(Class<T> type, long id, Object... fields) {
        T result = BeanUtils.instantiateClass(type);
        ReflectionTestUtils.setField(result, "id", id);
        for (int i = 0; i < fields.length; i += 2) {
            ReflectionTestUtils.setField(result, (String) fields[i], fields[i + 1]);
        }
        return result;
    }
}
