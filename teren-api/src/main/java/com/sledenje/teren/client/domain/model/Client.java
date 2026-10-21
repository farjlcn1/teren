package com.sledenje.teren.client.domain.model;

import java.time.Instant;

/**
 * Stranka, za katero se izvajajo delovni nalogi.
 *
 * @param externalId ID stranke v zunanjem sistemu, če je bila uvožena s sinhronizacijo
 */
public record Client(
        String id,
        String name,
        String address,
        String contactInfo,
        ClientSource source,
        String externalId,
        Instant createdAt,
        Instant updatedAt) {
}
