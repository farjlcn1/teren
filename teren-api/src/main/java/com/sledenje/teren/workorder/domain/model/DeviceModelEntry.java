package com.sledenje.teren.workorder.domain.model;

/**
 * @param comment ime naprave pri {@link DeviceModel#OSTALO}
 */
public record DeviceModelEntry(DeviceModel deviceModel, String comment) {
}
