package com.sledenje.teren.common.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nastavitve aplikacije pod predpono {@code teren.api} (application.yml / okoljske spremenljivke).
 *
 * @param publicUrl javni URL API-ja, ki se prikaže v Swagger dokumentaciji
 */
@ConfigurationProperties(prefix = "teren.api")
public record TerenApiProperties(String publicUrl) {
}
