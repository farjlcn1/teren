package com.sledenje.teren.workorder.domain.model;

import java.time.Instant;

/**
 * Fotografija naloga. Datoteko hrani aplikacija Teren na disku; API hrani samo njeno pot.
 *
 * @param filePath pot znotraj mape z naloženimi datotekami aplikacije
 * @param takenAt  čas iz EXIF podatkov slike, če ga ima
 */
public record WorkOrderPhoto(String id, String filePath, Instant takenAt, Instant uploadedAt) {
}
