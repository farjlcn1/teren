package com.sledenje.teren.workorder.domain.repository;

import com.sledenje.teren.workorder.domain.model.CreatedWorkOrder;
import com.sledenje.teren.workorder.domain.model.DeviceModelEntry;
import com.sledenje.teren.workorder.domain.model.ValueChange;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderAttachments;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetails;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorkOrderRepository {

    List<WorkOrder> findWorkOrders(WorkOrderSearch search);

    Optional<WorkOrderDetail> findWorkOrderDetail(String workOrderId);

    /**
     * @return vsi različni IMEI-ji nalogov, urejeni naraščajoče
     */
    List<String> findDistinctImeis();

    boolean existsClient(String clientId);

    boolean existsUser(String userId);

    /**
     * Atomarno poveča števec nalogov za dan (varno tudi ob hkratnem vnosu).
     *
     * @return zaporedna številka naloga tega dne, od 1 naprej
     */
    int nextDailySequence(LocalDate day);

    CreatedWorkOrder saveNewWorkOrder(String ident, Instant orderDate, WorkOrderDetails details,
            List<DeviceModelEntry> deviceModels, String createdById);

    /**
     * Zamenja vsebino naloga (monterje in opcije v celoti) in zapiše urejanje v zgodovino.
     */
    void updateWorkOrder(String workOrderId, WorkOrderDetails details, String editedById, Instant editedAt,
            Map<String, ValueChange> changes);

    /**
     * @return {@code false}, če nalog s tem ID-jem ne obstaja
     */
    boolean addAttachments(String workOrderId, WorkOrderAttachments attachments, Instant addedAt);

    /**
     * @return {@code false}, če nalog s tem ID-jem ne obstaja
     */
    boolean markSent(String workOrderId, Instant sentAt);

}
