package com.sledenje.teren.vehicle.domain.model;

/**
 * Izvor zapisa vozila. Stolpec "vehicles"."source" je Postgres enum "ClientSource" (Prisma ga deli s strankami),
 * zato se vrednosti morajo ujemati z njim.
 */
public enum VehicleSource {
    MANUAL,
    IMPORT,
    SYNC
}
