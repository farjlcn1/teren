package com.sledenje.teren.workorder.domain.model;

import java.util.List;

/**
 * Nalog s fotografijami (po času nalaganja) in zgodovino urejanj (najnovejše prvo).
 */
public record WorkOrderDetail(WorkOrder workOrder, List<WorkOrderPhoto> photos, List<WorkOrderEdit> edits) {
}
