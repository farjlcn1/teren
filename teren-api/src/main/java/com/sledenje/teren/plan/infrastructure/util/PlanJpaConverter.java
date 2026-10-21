package com.sledenje.teren.plan.infrastructure.util;

import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlannedTask;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import com.sledenje.teren.plan.infrastructure.persistence.PlanGroupJpaEntity;
import com.sledenje.teren.plan.infrastructure.persistence.PlannedTaskJpaEntity;

/**
 * Pretvorbe med JPA entitetami in domenskim modelom plana. Klicati jih je treba v transakciji.
 */
public final class PlanJpaConverter {

    private PlanJpaConverter() {
    }

    public static PlanGroup toDomain(PlanGroupJpaEntity entity) {
        return new PlanGroup(
                entity.getId(),
                entity.getClient().getId(),
                entity.getClient().getName(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getNote(),
                entity.getContact(),
                entity.getExpectedInstaller(),
                entity.getExpectedInstallerOtherText(),
                entity.getCreatedById(),
                entity.getTasks().stream().map(PlanJpaConverter::toDomain).toList());
    }

    public static PlannedTask toDomain(PlannedTaskJpaEntity entity) {
        return new PlannedTask(entity.getId(), entity.getVehiclePlate(), entity.getType(), entity.getNote(),
                entity.getWorkOrderId());
    }

    public static PlannedTaskDetail toDetail(PlannedTaskJpaEntity entity) {
        PlanGroupJpaEntity planGroup = entity.getPlanGroup();
        return new PlannedTaskDetail(toDomain(entity), planGroup.getId(), planGroup.getClient().getId(),
                planGroup.getClient().getName());
    }

}
