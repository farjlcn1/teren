package com.sledenje.teren.vehicle.domain.model;

import java.time.Instant;
import java.util.Locale;

/**
 * Vozilo (registrska številka), ki se izbira pri delovnih nalogih in v planu.
 *
 * @param plate      registrska številka, vedno normalizirana z {@link #normalizePlate(String)} in unikatna
 * @param externalId ID vozila v zunanjem sistemu, če je bilo uvoženo s sinhronizacijo
 */
public record Vehicle(
        String id,
        String plate,
        VehicleSource source,
        String externalId,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Registrske številke se hranijo brez presledkov na robu in z velikimi črkami ("lj 12-abc " → "LJ 12-ABC").
     */
    public static String normalizePlate(String plate) {
        return plate.trim().toUpperCase(Locale.ROOT);
    }

}
