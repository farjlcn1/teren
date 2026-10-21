package com.sledenje.teren.client.domain.model;

/**
 * Stranka, kot jo vrne zunanji sistem pri sinhronizaciji.
 *
 * @param externalId ID stranke v zunanjem sistemu; po njem se ob naslednji sinhronizaciji najde ista stranka
 */
public record SyncedClient(String externalId, String name, String address, String contactInfo) {
}
