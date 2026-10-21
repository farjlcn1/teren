package com.sledenje.teren.workorder.domain.service;

import com.sledenje.teren.workorder.domain.exception.InvalidWorkOrderException;
import com.sledenje.teren.workorder.domain.exception.WorkOrderNotFoundException;
import com.sledenje.teren.workorder.domain.model.CreatedWorkOrder;
import com.sledenje.teren.workorder.domain.model.NewWorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderAttachments;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetails;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import java.util.List;

/**
 * Delovni nalogi. Kdo sme videti ali urejati katere naloge, preveri aplikacija Teren
 * (uporabnikom brez pregleda vseh nalogov nastavi {@link WorkOrderSearch#createdById()}).
 */
public interface WorkOrderService {

    List<WorkOrder> findWorkOrders(WorkOrderSearch search);

    /**
     * @throws WorkOrderNotFoundException če nalog s tem ID-jem ne obstaja
     */
    WorkOrderDetail findWorkOrderDetail(String workOrderId);

    List<String> findDistinctImeis();

    /**
     * V eni transakciji: dodeli številko naloga, shrani nalog, doda vozilo, če ga še ni,
     * in nalog zabeleži v planu (glej {@code PlanService#recordCompletedWorkOrder}).
     *
     * @throws InvalidWorkOrderException če vsebina ne ustreza pravilom ali stranka oz. avtor ne obstajata
     */
    CreatedWorkOrder createWorkOrder(NewWorkOrder newWorkOrder);

    /**
     * Shrani spremembe in jih, če obstajajo, zapiše v zgodovino urejanj. Doda vozilo, če ga še ni.
     *
     * @throws WorkOrderNotFoundException če nalog s tem ID-jem ne obstaja
     * @throws InvalidWorkOrderException  če vsebina ne ustreza pravilom ali stranka oz. urejevalec ne obstajata
     */
    void updateWorkOrder(String workOrderId, WorkOrderDetails details, String editedById);

    /**
     * Doda fotografije in podpis (podpis nalog zaklene).
     *
     * @throws WorkOrderNotFoundException če nalog s tem ID-jem ne obstaja
     */
    void addAttachments(String workOrderId, WorkOrderAttachments attachments);

    /**
     * Zabeleži, da je bil nalog poslan po e-pošti.
     *
     * @throws WorkOrderNotFoundException če nalog s tem ID-jem ne obstaja
     */
    void markSent(String workOrderId);

}
