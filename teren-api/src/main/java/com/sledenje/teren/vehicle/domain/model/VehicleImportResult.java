package com.sledenje.teren.vehicle.domain.model;

/**
 * @param skippedCount registrske številke, ki že obstajajo ali so se v uvozu ponovile
 */
public record VehicleImportResult(int createdCount, int skippedCount) {
}
