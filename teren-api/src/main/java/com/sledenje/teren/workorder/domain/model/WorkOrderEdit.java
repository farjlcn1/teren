package com.sledenje.teren.workorder.domain.model;

import java.time.Instant;

/**
 * Zapis o urejanju naloga.
 *
 * @param changesJson spremembe v obliki, ki jo zapiše in prikaže aplikacija Teren
 */
public record WorkOrderEdit(String id, Instant editedAt, UserReference editedBy, String changesJson) {
}
