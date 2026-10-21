package com.sledenje.teren.workorder.infrastructure.util;

import com.sledenje.teren.workorder.domain.model.UserReference;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderSearch;
import com.sledenje.teren.workorder.domain.model.WorkOrderSortField;
import com.sledenje.teren.workorder.presentation.dto.UserReferenceDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderClientDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderDetailDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderDeviceModelDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderEditDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderInstallerDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderOptionDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderPhotoDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderSearchRequest;
import java.util.Map;

/**
 * Pretvorbe med DTO-ji presentation nivoja in domenskim modelom naloga.
 */
public final class WorkOrderDtoConverter {

    // Imena parametra "sort" so enaka kot v naslovu strani z nalogi v aplikaciji Teren.
    private static final Map<String, WorkOrderSortField> SORT_FIELDS = Map.of(
            "orderDate", WorkOrderSortField.ORDER_DATE,
            "ident", WorkOrderSortField.IDENT,
            "vehiclePlate", WorkOrderSortField.VEHICLE_PLATE,
            "imei", WorkOrderSortField.IMEI,
            "client", WorkOrderSortField.CLIENT_NAME,
            "type", WorkOrderSortField.TYPE,
            "difficulty", WorkOrderSortField.DIFFICULTY,
            "culprit", WorkOrderSortField.CULPRIT);

    private WorkOrderDtoConverter() {
    }

    /**
     * Neznano polje za urejanje pomeni datum naloga, neznana smer pa padajoče (kot v aplikaciji).
     */
    public static WorkOrderSearch toSearch(WorkOrderSearchRequest request) {
        return new WorkOrderSearch(
                request.from(),
                request.to(),
                blankToNull(request.clientId()),
                blankToNull(request.vehiclePlate()),
                blankToNull(request.imei()),
                request.type(),
                request.difficulty(),
                request.installer(),
                request.option(),
                request.deviceModel(),
                request.culprit(),
                blankToNull(request.createdById()),
                SORT_FIELDS.getOrDefault(request.sort() == null ? "" : request.sort(), WorkOrderSortField.ORDER_DATE),
                "asc".equals(request.dir()),
                request.limit());
    }

    public static WorkOrderDto toDto(WorkOrder workOrder) {
        return new WorkOrderDto(
                workOrder.id(),
                workOrder.ident(),
                workOrder.orderDate(),
                workOrder.type(),
                workOrder.difficulty(),
                workOrder.status(),
                new WorkOrderClientDto(workOrder.client().id(), workOrder.client().name(),
                        workOrder.client().address(), workOrder.client().contactInfo()),
                workOrder.vehiclePlate(),
                workOrder.vehicleBrand(),
                workOrder.vehicleModel(),
                workOrder.vehicleYear(),
                workOrder.imei(),
                workOrder.imeiPrev(),
                workOrder.culprit(),
                workOrder.comment(),
                workOrder.signatureUrl(),
                workOrder.signedAt(),
                workOrder.lockedAt(),
                workOrder.sentAt(),
                toDto(workOrder.createdBy()),
                workOrder.createdAt(),
                workOrder.updatedAt(),
                workOrder.installers().stream()
                        .map(installer -> new WorkOrderInstallerDto(installer.id(), installer.name(), installer.otherText()))
                        .toList(),
                workOrder.options().stream()
                        .map(option -> new WorkOrderOptionDto(option.id(), option.optionType(), option.comment()))
                        .toList(),
                workOrder.deviceModels().stream()
                        .map(deviceModel -> new WorkOrderDeviceModelDto(
                                deviceModel.id(), deviceModel.deviceModel(), deviceModel.comment()))
                        .toList());
    }

    public static WorkOrderDetailDto toDto(WorkOrderDetail detail) {
        return new WorkOrderDetailDto(
                toDto(detail.workOrder()),
                detail.photos().stream()
                        .map(photo -> new WorkOrderPhotoDto(
                                photo.id(), photo.filePath(), photo.takenAt(), photo.uploadedAt()))
                        .toList(),
                detail.edits().stream()
                        .map(edit -> new WorkOrderEditDto(
                                edit.id(), edit.editedAt(), toDto(edit.editedBy()), edit.changesJson()))
                        .toList());
    }

    private static UserReferenceDto toDto(UserReference user) {
        return new UserReferenceDto(user.id(), user.fullName());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
