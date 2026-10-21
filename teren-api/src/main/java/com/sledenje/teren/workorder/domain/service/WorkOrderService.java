package com.sledenje.teren.workorder.domain.service;

import com.sledenje.teren.workorder.domain.exception.WorkOrderNotFoundException;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import java.util.List;

/**
 * Branje delovnih nalogov. Kdo sme videti katere naloge, preveri aplikacija Teren
 * (uporabnikom brez pregleda vseh nalogov nastavi {@link WorkOrderSearch#createdById()}).
 */
public interface WorkOrderService {

    List<WorkOrder> findWorkOrders(WorkOrderSearch search);

    /**
     * @throws WorkOrderNotFoundException če nalog s tem ID-jem ne obstaja
     */
    WorkOrderDetail findWorkOrderDetail(String workOrderId);

    List<String> findDistinctImeis();

}
