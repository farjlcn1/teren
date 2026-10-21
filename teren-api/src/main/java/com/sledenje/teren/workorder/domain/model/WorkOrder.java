package com.sledenje.teren.workorder.domain.model;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import java.time.Instant;
import java.util.List;

/**
 * Delovni nalog s stranko, monterji, opcijami in modeli naprav (brez fotografij in zgodovine urejanj).
 *
 * @param ident        številka naloga, unikatna
 * @param signatureUrl pot do slike podpisa v mapi z naloženimi datotekami aplikacije
 * @param lockedAt     čas zaklepa ob podpisu (aplikacija ga prikaže kot "Zaklenjen")
 * @param sentAt       čas zadnjega pošiljanja po e-pošti
 */
public record WorkOrder(
        String id,
        String ident,
        Instant orderDate,
        WorkOrderType type,
        Difficulty difficulty,
        WorkOrderStatus status,
        WorkOrderClient client,
        String vehiclePlate,
        String vehicleBrand,
        String vehicleModel,
        int vehicleYear,
        String imei,
        String imeiPrev,
        Culprit culprit,
        String comment,
        String signatureUrl,
        Instant signedAt,
        Instant lockedAt,
        Instant sentAt,
        UserReference createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<WorkOrderInstaller> installers,
        List<WorkOrderOption> options,
        List<WorkOrderDeviceModel> deviceModels) {
}
