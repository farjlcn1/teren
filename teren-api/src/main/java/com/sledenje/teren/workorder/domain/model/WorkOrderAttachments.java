package com.sledenje.teren.workorder.domain.model;

import java.util.List;

/**
 * Datoteke, ki jih je aplikacija po vnosu naloga shranila na disk.
 *
 * @param signatureUrl pot do slike podpisa ali {@code null}; podpis nalog zaklene
 */
public record WorkOrderAttachments(String signatureUrl, List<PhotoEntry> photos) {
}
