package com.sledenje.teren.user.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Pravila za novo geslo (enaka kot v obrazcih aplikacije Teren).
 */
public final class PasswordPolicy {

    /**
     * BCrypt upošteva največ 72 bajtov gesla.
     */
    static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /**
     * @return sporočilo za uporabnika, če geslo ne ustreza pravilom, sicer prazno
     */
    public static Optional<String> findViolation(String password) {
        if (password == null || password.length() < 8) {
            return Optional.of("Geslo mora imeti vsaj 8 znakov");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return Optional.of("Geslo je predolgo (največ 72 znakov)");
        }
        if (password.chars().noneMatch(character -> character >= 'A' && character <= 'Z')) {
            return Optional.of("Geslo mora vsebovati vsaj eno veliko črko");
        }
        if (password.chars().noneMatch(character -> character >= '0' && character <= '9')) {
            return Optional.of("Geslo mora vsebovati vsaj eno številko");
        }
        // Kot v aplikaciji (/[^A-Za-z0-9]/): poseben znak je vse razen ASCII črk in številk, tudi "č".
        if (password.chars().allMatch(PasswordPolicy::isAsciiLetterOrDigit)) {
            return Optional.of("Geslo mora vsebovati vsaj en poseben znak");
        }
        return Optional.empty();
    }

    private static boolean isAsciiLetterOrDigit(int character) {
        return (character >= 'A' && character <= 'Z')
                || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9');
    }

}
