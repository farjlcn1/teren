package com.sledenje.teren.workorder.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;

/**
 * Monter pri vnosu ali urejanju naloga.
 *
 * @param otherText ime monterja pri {@link InstallerName#OSTALO}
 */
public record InstallerEntry(InstallerName name, String otherText) {
}
