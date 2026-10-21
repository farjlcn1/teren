package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;
import java.time.Instant;
import java.util.List;

/**
 * Dogodek v planu s planiranimi nalogi (po času vnosa).
 */
public record PlanGroup(
        String id,
        String clientId,
        String clientName,
        Instant startAt,
        Instant endAt,
        String note,
        String contact,
        InstallerName expectedInstaller,
        String expectedInstallerOtherText,
        String createdById,
        List<PlannedTask> tasks) {

    public boolean hasCompletedTask() {
        return tasks.stream().anyMatch(PlannedTask::isCompleted);
    }

}
