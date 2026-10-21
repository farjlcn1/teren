package com.sledenje.teren.user.domain.model;

import java.time.Instant;
import java.util.Locale;

/**
 * Uporabnik aplikacije Teren. Zgoščeno geslo ni del modela in nikoli ne zapusti API-ja.
 *
 * @param email    vedno z malimi črkami ({@link #normalizeEmail(String)}), unikaten
 * @param isActive onemogočen uporabnik se ne more prijaviti, njegova seja pa preneha veljati
 */
public record User(
        String id,
        String email,
        String fullName,
        boolean isActive,
        UserPermissions permissions,
        Instant createdAt,
        Instant updatedAt) {

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
