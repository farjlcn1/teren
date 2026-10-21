package com.sledenje.teren.plan.domain.model;

/**
 * Planirani nalog z dogodkom in stranko (za obrazec novega naloga, ki ga zaključi).
 */
public record PlannedTaskDetail(PlannedTask task, String planGroupId, String clientId, String clientName) {
}
