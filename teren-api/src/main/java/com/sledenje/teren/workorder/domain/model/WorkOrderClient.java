package com.sledenje.teren.workorder.domain.model;

/**
 * Stranka naloga, kot jo potrebujejo seznam, PDF in izvoz.
 */
public record WorkOrderClient(String id, String name, String address, String contactInfo) {
}
