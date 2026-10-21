package com.sledenje.teren.workorder.infrastructure.util;

import com.sledenje.teren.workorder.domain.model.UserReference;
import com.sledenje.teren.workorder.domain.model.WorkOrder;
import com.sledenje.teren.workorder.domain.model.WorkOrderClient;
import com.sledenje.teren.workorder.domain.model.WorkOrderDetail;
import com.sledenje.teren.workorder.domain.model.WorkOrderDeviceModel;
import com.sledenje.teren.workorder.domain.model.WorkOrderEdit;
import com.sledenje.teren.workorder.domain.model.WorkOrderInstaller;
import com.sledenje.teren.workorder.domain.model.WorkOrderOption;
import com.sledenje.teren.workorder.domain.model.WorkOrderPhoto;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderClientJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import com.sledenje.teren.workorder.infrastructure.persistence.WorkOrderUserJpaEntity;

/**
 * Pretvorbe med JPA entitetami in domenskim modelom naloga. Klicati jih je treba v transakciji,
 * ker se povezani zapisi naložijo šele ob dostopu.
 */
public final class WorkOrderJpaConverter {

    private WorkOrderJpaConverter() {
    }

    public static WorkOrder toDomain(WorkOrderJpaEntity entity) {
        return new WorkOrder(
                entity.getId(),
                entity.getIdent(),
                entity.getOrderDate(),
                entity.getType(),
                entity.getDifficulty(),
                entity.getStatus(),
                toDomain(entity.getClient()),
                entity.getVehiclePlate(),
                entity.getVehicleBrand(),
                entity.getVehicleModel(),
                entity.getVehicleYear(),
                entity.getImei(),
                entity.getImeiPrev(),
                entity.getCulprit(),
                entity.getComment(),
                entity.getSignatureUrl(),
                entity.getSignedAt(),
                entity.getLockedAt(),
                entity.getSentAt(),
                toDomain(entity.getCreatedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getInstallers().stream()
                        .map(installer -> new WorkOrderInstaller(
                                installer.getId(), installer.getName(), installer.getOtherText()))
                        .toList(),
                entity.getOptions().stream()
                        .map(option -> new WorkOrderOption(option.getId(), option.getOptionType(), option.getComment()))
                        .toList(),
                entity.getDeviceModels().stream()
                        .map(deviceModel -> new WorkOrderDeviceModel(
                                deviceModel.getId(), deviceModel.getDeviceModel(), deviceModel.getComment()))
                        .toList());
    }

    public static WorkOrderDetail toDetail(WorkOrderJpaEntity entity) {
        return new WorkOrderDetail(
                toDomain(entity),
                entity.getPhotos().stream()
                        .map(photo -> new WorkOrderPhoto(
                                photo.getId(), photo.getFilePath(), photo.getTakenAt(), photo.getUploadedAt()))
                        .toList(),
                entity.getEdits().stream()
                        .map(edit -> new WorkOrderEdit(
                                edit.getId(), edit.getEditedAt(), toDomain(edit.getEditedBy()), edit.getChangesJson()))
                        .toList());
    }

    private static WorkOrderClient toDomain(WorkOrderClientJpaEntity client) {
        return new WorkOrderClient(client.getId(), client.getName(), client.getAddress(), client.getContactInfo());
    }

    private static UserReference toDomain(WorkOrderUserJpaEntity user) {
        return new UserReference(user.getId(), user.getFullName());
    }

}
