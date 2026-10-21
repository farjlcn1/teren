package com.sledenje.teren.client.domain.model;

/**
 * Podatki za vnos nove stranke. ID, izvor in časovne oznake določi sistem.
 */
public record NewClient(String name, String address, String contactInfo) {
}
