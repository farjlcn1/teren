package com.sledenje.teren.client.domain.model;

/**
 * Novi podatki obstoječe stranke. Prazna neobvezna polja so {@code null} in se v bazi izbrišejo.
 */
public record ClientUpdate(String name, String address, String contactInfo) {
}
