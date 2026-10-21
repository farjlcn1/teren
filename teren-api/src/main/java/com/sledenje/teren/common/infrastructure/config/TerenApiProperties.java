package com.sledenje.teren.common.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nastavitve aplikacije pod predpono {@code teren.api} (application.yml / okoljske spremenljivke).
 *
 * @param publicUrl javni URL API-ja, ki se prikaže v Swagger dokumentaciji
 * @param token     skupni žeton, s katerim se aplikacija Teren predstavi API-ju; brez njega se API ne zažene
 */
@Validated
@ConfigurationProperties(prefix = "teren.api")
public record TerenApiProperties(
        String publicUrl,
        @NotBlank(message = "manjka API_TOKEN") String token) {
}
