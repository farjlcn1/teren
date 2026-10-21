package com.sledenje.teren.user.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param email email prvega admina; ustvari se ob zagonu, če v bazi še ni nobenega uporabnika (prazno = ne ustvari)
 */
@ConfigurationProperties(prefix = "teren.first-admin")
public record FirstAdminProperties(String email) {
}
