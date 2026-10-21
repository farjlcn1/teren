package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;
import java.time.Instant;

/**
 * Podatki dogodka iz obrazca v planu.
 *
 * @param endAt                      konec; pri vnosu neobvezen (privzeto ura po začetku), pri urejanju obvezen
 * @param expectedInstallerOtherText ime monterja; upošteva se samo pri {@link InstallerName#OSTALO}
 */
public record PlanGroupChange(
        String clientId,
        Instant startAt,
        Instant endAt,
        String note,
        String contact,
        InstallerName expectedInstaller,
        String expectedInstallerOtherText) {
}
