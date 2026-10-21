package com.sledenje.teren.user.domain.model;

/**
 * Uporabnik skupaj z zgoščenim geslom. Uporablja se samo za preverjanje prijave znotraj API-ja.
 */
public record StoredCredentials(User user, String passwordHash) {
}
