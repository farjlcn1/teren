package com.sledenje.teren.workorder.domain.model;

import java.time.Instant;

/**
 * Filtri in razvrščanje seznama nalogov. Prazen filter ({@code null}) ne omejuje.
 *
 * @param orderDateFrom  najzgodnejši datum naloga (vključno)
 * @param orderDateTo    najkasnejši datum naloga (vključno)
 * @param vehiclePlate   del registrske številke, ne glede na velike in male črke
 * @param imei           del IMEI-ja
 * @param installer      nalog ima tega monterja
 * @param option         nalog ima to opcijo
 * @param deviceModel    nalog ima ta model naprave
 * @param createdById    samo nalogi tega uporabnika (aplikacija ga nastavi uporabnikom brez pregleda vseh nalogov)
 * @param ascending      naraščajoče; privzeto padajoče
 * @param limit          največ toliko nalogov; {@code null} pomeni vse
 */
public record WorkOrderSearch(
        Instant orderDateFrom,
        Instant orderDateTo,
        String clientId,
        String vehiclePlate,
        String imei,
        WorkOrderType type,
        Difficulty difficulty,
        InstallerName installer,
        OptionType option,
        DeviceModel deviceModel,
        Culprit culprit,
        String createdById,
        WorkOrderSortField sortField,
        boolean ascending,
        Integer limit) {
}
