package com.sledenje.teren.workorder.domain.repository;

import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository {

    List<WorkOrder> findWorkOrders(WorkOrderSearch search);

    Optional<WorkOrderDetail> findWorkOrderDetail(String workOrderId);

    /**
     * @return vsi različni IMEI-ji nalogov, urejeni naraščajoče
     */
    List<String> findDistinctImeis();

}
