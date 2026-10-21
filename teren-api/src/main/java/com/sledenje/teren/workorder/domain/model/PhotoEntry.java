package com.sledenje.teren.workorder.domain.model;

import java.time.Instant;

/**
 * Fotografija, ki jo je aplikacija shranila na disk.
 *
 * @param takenAt čas iz EXIF podatkov ali {@code null}
 */
public record PhotoEntry(String filePath, Instant takenAt) {
}
