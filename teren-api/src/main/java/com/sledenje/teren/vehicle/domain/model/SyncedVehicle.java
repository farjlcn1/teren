package com.sledenje.teren.vehicle.domain.model;

/**
 * Vozilo, kot ga vrne zunanji sistem pri sinhronizaciji.
 *
 * @param externalId ID vozila v zunanjem sistemu; po njem se ob naslednji sinhronizaciji najde isto vozilo
 * @param plate      normalizirana registrska številka
 */
public record SyncedVehicle(String externalId, String plate) {
}
