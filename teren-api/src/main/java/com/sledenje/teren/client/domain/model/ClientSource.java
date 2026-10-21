package com.sledenje.teren.client.domain.model;

/**
 * Izvor zapisa stranke. Vrednosti se ujemajo s Postgres enumom "ClientSource".
 */
public enum ClientSource {
    MANUAL,
    IMPORT,
    SYNC
}
