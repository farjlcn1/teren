package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.common.domain.model.WorkOrderType;
import java.time.Instant;

/**
 * Pravkar ustvarjen delovni nalog, ki ga je treba zabeležiti v planu.
 *
 * @param plannedTaskId      planirani nalog, iz katerega je nalog nastal, ali {@code null}
 * @param installer          prvi monter naloga
 * @param installerOtherText ime monterja pri {@link InstallerName#OSTALO}, sicer {@code null}
 * @param createdById        avtor naloga; postane avtor samodejno ustvarjenega dogodka
 */
public record CompletedWorkOrder(
        String workOrderId,
        String plannedTaskId,
        String clientId,
        String vehiclePlate,
        WorkOrderType type,
        String note,
        InstallerName installer,
        String installerOtherText,
        Instant completedAt,
        String createdById) {
}
