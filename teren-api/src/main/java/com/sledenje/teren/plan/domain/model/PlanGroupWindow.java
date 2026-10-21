package com.sledenje.teren.plan.domain.model;

import java.time.Instant;

/**
 * Časovno okno dogodka.
 */
public record PlanGroupWindow(String id, Instant startAt, Instant endAt) {
}
