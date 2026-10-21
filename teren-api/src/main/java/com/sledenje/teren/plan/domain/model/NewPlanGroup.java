package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;
import java.time.Instant;

/**
 * Dogodek v planu (obisk stranke v časovnem oknu).
 *
 * @param expectedInstaller          monter, ki naj bi dogodek izvedel, ali {@code null}
 * @param expectedInstallerOtherText ime monterja pri {@link InstallerName#OSTALO}, sicer {@code null}
 */
public record NewPlanGroup(
        String clientId,
        Instant startAt,
        Instant endAt,
        String note,
        String contact,
        InstallerName expectedInstaller,
        String expectedInstallerOtherText,
        String createdById) {
}
