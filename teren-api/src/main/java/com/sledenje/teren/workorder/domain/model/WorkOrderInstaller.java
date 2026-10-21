package com.sledenje.teren.workorder.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;

/**
 * @param otherText ime monterja, ko je {@code name} {@link InstallerName#OSTALO}
 */
public record WorkOrderInstaller(String id, InstallerName name, String otherText) {
}
