package com.sledenje.teren.workorder.domain.model;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import java.util.List;

/**
 * Vsebina naloga, ki jo monter vnese in jo je mogoče urejati. Neobvezna polja so {@code null}, če niso izpolnjena.
 */
public record WorkOrderDetails(
        WorkOrderType type,
        Difficulty difficulty,
        String clientId,
        String vehiclePlate,
        String vehicleBrand,
        String vehicleModel,
        int vehicleYear,
        String imei,
        String imeiPrev,
        Culprit culprit,
        String comment,
        List<InstallerEntry> installers,
        List<OptionEntry> options) {
}
