package com.sledenje.teren.workorder.infrastructure.api.service;

import com.sledenje.teren.workorder.domain.exception.WorkOrderNotFoundException;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import com.sledenje.teren.workorder.domain.repository.WorkOrderRepository;
import com.sledenje.teren.workorder.domain.service.WorkOrderService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrder> findWorkOrders(WorkOrderSearch search) {
        return workOrderRepository.findWorkOrders(search);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderDetail findWorkOrderDetail(String workOrderId) {
        return workOrderRepository.findWorkOrderDetail(workOrderId)
                .orElseThrow(() -> new WorkOrderNotFoundException(workOrderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistinctImeis() {
        return workOrderRepository.findDistinctImeis();
    }

}
