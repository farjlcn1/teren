package com.sledenje.teren.workorder.infrastructure.api.service;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.common.infrastructure.config.TerenApiProperties;
import com.sledenje.teren.plan.domain.model.CompletedWorkOrder;
import com.sledenje.teren.plan.domain.service.PlanService;
import com.sledenje.teren.vehicle.domain.service.VehicleService;
import com.sledenje.teren.workorder.domain.exception.InvalidWorkOrderException;
import com.sledenje.teren.workorder.domain.exception.WorkOrderNotFoundException;
import com.sledenje.teren.workorder.domain.model.CreatedWorkOrder;
import com.sledenje.teren.workorder.domain.model.InstallerEntry;
import com.sledenje.teren.workorder.domain.model.NewWorkOrder;
import com.sledenje.teren.workorder.domain.model.ValueChange;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderAttachments;
import com.sledenje.teren.workorder.domain.model.WorkOrderChanges;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetails;
import com.sledenje.teren.workorder.domain.model.WorkOrderRules;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import com.sledenje.teren.workorder.domain.repository.WorkOrderRepository;
import com.sledenje.teren.workorder.domain.service.WorkOrderService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

    private static final DateTimeFormatter IDENT_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final WorkOrderRepository workOrderRepository;
    private final VehicleService vehicleService;
    private final PlanService planService;
    private final ZoneId timeZone;

    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository, VehicleService vehicleService,
            PlanService planService, TerenApiProperties terenApiProperties) {
        this.workOrderRepository = workOrderRepository;
        this.vehicleService = vehicleService;
        this.planService = planService;
        this.timeZone = terenApiProperties.timeZone();
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

    @Override
    @Transactional
    public CreatedWorkOrder createWorkOrder(NewWorkOrder newWorkOrder) {
        WorkOrderDetails details = newWorkOrder.details();
        Instant orderDate = now();
        LocalDate orderDay = orderDate.atZone(timeZone).toLocalDate();
        WorkOrderRules.validate(details, newWorkOrder.deviceModels(), orderDay.getYear());
        requireClient(details.clientId());
        requireUser(newWorkOrder.createdById());

        String ident = orderDay.format(IDENT_DATE)
                + String.format("%03d", workOrderRepository.nextDailySequence(orderDay));
        CreatedWorkOrder createdWorkOrder = workOrderRepository.saveNewWorkOrder(
                ident, orderDate, details, newWorkOrder.deviceModels(), newWorkOrder.createdById());
        vehicleService.ensureVehicleExists(details.vehiclePlate());

        InstallerEntry primaryInstaller = details.installers().getFirst();
        planService.recordCompletedWorkOrder(new CompletedWorkOrder(
                createdWorkOrder.id(),
                newWorkOrder.plannedTaskId(),
                details.clientId(),
                details.vehiclePlate(),
                details.type(),
                details.comment(),
                primaryInstaller.name(),
                primaryInstaller.name() == InstallerName.OSTALO ? primaryInstaller.otherText() : null,
                orderDate,
                newWorkOrder.createdById()));

        log.info("Ustvarjen nalog {} ({})", createdWorkOrder.ident(), createdWorkOrder.id());
        return createdWorkOrder;
    }

    @Override
    @Transactional
    public void updateWorkOrder(String workOrderId, WorkOrderDetails details, String editedById) {
        Instant editedAt = now();
        WorkOrderRules.validate(details, List.of(), editedAt.atZone(timeZone).getYear());
        WorkOrder before = findWorkOrderDetail(workOrderId).workOrder();
        requireClient(details.clientId());
        requireUser(editedById);

        Map<String, ValueChange> changes = WorkOrderChanges.between(before, details);
        if (!changes.isEmpty()) {
            workOrderRepository.updateWorkOrder(workOrderId, details, editedById, editedAt, changes);
            log.info("Urejen nalog {}: {}", workOrderId, changes.keySet());
        }
        vehicleService.ensureVehicleExists(details.vehiclePlate());
    }

    @Override
    @Transactional
    public void addAttachments(String workOrderId, WorkOrderAttachments attachments) {
        if (!workOrderRepository.addAttachments(workOrderId, attachments, now())) {
            throw new WorkOrderNotFoundException(workOrderId);
        }
        log.info("Nalogu {} dodanih {} fotografij{}", workOrderId, attachments.photos().size(),
                attachments.signatureUrl() != null ? " in podpis" : "");
    }

    @Override
    @Transactional
    public void markSent(String workOrderId) {
        if (!workOrderRepository.markSent(workOrderId, now())) {
            throw new WorkOrderNotFoundException(workOrderId);
        }
    }

    private void requireClient(String clientId) {
        if (!workOrderRepository.existsClient(clientId)) {
            throw new InvalidWorkOrderException("Stranka ne obstaja.");
        }
    }

    private void requireUser(String userId) {
        if (!workOrderRepository.existsUser(userId)) {
            throw new InvalidWorkOrderException("Uporabnik ne obstaja.");
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

}
